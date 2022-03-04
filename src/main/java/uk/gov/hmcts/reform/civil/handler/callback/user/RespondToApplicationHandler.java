package uk.gov.hmcts.reform.civil.handler.callback.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUnavailabilityDates;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@SuppressWarnings({"checkstyle:Indentation", "checkstyle:EmptyLineSeparator"})
@Service
@RequiredArgsConstructor
public class RespondToApplicationHandler extends CallbackHandler {

    private static final String RESPONSE_MESSAGE = "# You have responded to an application";
    private static final String JUDGES_REVIEW_MESSAGE =
        "<p> The application and your response will be reviewed by a Judge. </p>";
    private static final String CONFIRMATION_SUMMARY = "<br/><p> You have responded to the following application"
        + "%s: </p>"
        + "<ul> %s </ul>"
        + " %s ";
    public static final String TRIAL_DATE_FROM_REQUIRED = "Please enter the Date from if the trial has been fixed";
    public static final String INVALID_TRIAL_DATE_RANGE = "Trial Date From cannot be after Trial Date to. "
        + "Please enter valid range.";
    public static final String UNAVAILABLE_DATE_RANGE_MISSING = "Please provide at least one valid Date from if you "
        + "cannot attend hearing within next 3 months.";
    public static final String UNAVAILABLE_FROM_MUST_BE_PROVIDED = "If you selected option to be unavailable then "
        + "you must provide at least one valid Date from";
    public static final String INVALID_UNAVAILABILITY_RANGE = "Unavailability Date From cannot be after "
        + "Unavailability Date to. Please enter valid range.";
    public static final String INVALID_TRAIL_DATE_FROM_BEFORE_TODAY = "Trail date from must not be before today.";
    public static final String INVALID_UNAVAILABLE_DATE_FROM_BEFORE_TODAY = "Unavailability date from must not"
        + " be before today.";
    public static final String APPLICATION_RESPONSE_PRESENT = "The General Application has already "
        +  "received a response.";
    private static final List<CaseEvent> EVENTS = Collections.singletonList(RESPOND_TO_APPLICATION);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START), this::isApplicationInJudicialReviewStage,
            callbackKey(MID, "hearing-screen-response"), this::hearingScreenResponse,
            callbackKey(SUBMITTED), this::buildResponseConfirmation
        );
    }

    private AboutToStartOrSubmitCallbackResponse isApplicationInJudicialReviewStage(CallbackParams callbackParams) {
        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(applicationExistsValidation(callbackParams))
            .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private SubmittedCallbackResponse buildResponseConfirmation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();

        return SubmittedCallbackResponse.builder()
            .confirmationHeader(RESPONSE_MESSAGE)
            .confirmationBody(buildConfirmationSummary(caseData))
            .build();
    }

    public List<String> applicationExistsValidation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();

        List<String> errors = new ArrayList<>();
        if (caseData.getCcdState() == CaseState
                .APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION
        ) {
            errors.add(APPLICATION_RESPONSE_PRESENT);
        }
        return errors;
    }

    private CallbackResponse hearingScreenResponse(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        List<String> errors = null;
        if (callbackParams.getRequest().getEventId().equals("RESPOND_TO_APPLICATION")) {
            GAHearingDetails hearingDetails = caseData.getHearingDetailsResp();
            errors = validateResponseHearingScreen(hearingDetails);
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    private List<String> validateResponseHearingScreen(GAHearingDetails hearingDetails) {
        List<String> errors = new ArrayList<>();
        validateDateRanges(errors,
                           hearingDetails.getTrialRequiredYesOrNo(),
                           hearingDetails.getTrialDateFrom(),
                           hearingDetails.getTrialDateTo(),
                           hearingDetails.getUnavailableTrialRequiredYesOrNo(),
                           hearingDetails.getGeneralAppUnavailableDates()
        );
        return errors;
    }

    private void validateDateRanges(List<String> errors,
                                    YesOrNo isTrialScheduled,
                                    LocalDate trialDateFrom,
                                    LocalDate trialDateTo,
                                    YesOrNo isUnavailable,
                                    List<Element<GAUnavailabilityDates>> datesUnavailableList) {

        if (YES.equals(isTrialScheduled)) {
            if (trialDateFrom != null) {
                if (trialDateTo != null && trialDateTo.isBefore(trialDateFrom)) {
                    errors.add(INVALID_TRIAL_DATE_RANGE);
                } else if (trialDateFrom.isBefore(LocalDate.now())) {
                    errors.add(INVALID_TRAIL_DATE_FROM_BEFORE_TODAY);
                }
            } else {
                errors.add(TRIAL_DATE_FROM_REQUIRED);
            }
        }

        if (YES.equals(isUnavailable)) {
            if (isEmpty(datesUnavailableList)) {
                errors.add(UNAVAILABLE_DATE_RANGE_MISSING);
            } else {
                for (Element<GAUnavailabilityDates> dateRange : datesUnavailableList) {
                    LocalDate dateFrom = dateRange.getValue().getUnavailableTrialDateFrom();
                    LocalDate dateTo = dateRange.getValue().getUnavailableTrialDateTo();
                    if (dateFrom == null) {
                        errors.add(UNAVAILABLE_FROM_MUST_BE_PROVIDED);
                    } else if (dateTo != null && dateTo.isBefore(dateFrom)) {
                        errors.add(INVALID_UNAVAILABILITY_RANGE);
                    } else if (dateFrom.isBefore(LocalDate.now())) {
                        errors.add(INVALID_UNAVAILABLE_DATE_FROM_BEFORE_TODAY);
                    }
                }
            }
        }
    }

    private String buildConfirmationSummary(CaseData caseData) {
        var genAppTypes = caseData.getGeneralAppType().getTypes();
        String appType = genAppTypes.stream().map(type -> "<li>" + type.getDisplayedValue() + "</li>")
            .collect(Collectors.joining());
        return format(
            CONFIRMATION_SUMMARY,
            genAppTypes.size() == 1 ? "" : "s",
            appType,
            JUDGES_REVIEW_MESSAGE
        );
    }
}
