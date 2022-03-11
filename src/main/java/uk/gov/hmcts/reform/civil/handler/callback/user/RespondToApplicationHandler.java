package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.reform.civil.handler.callback.CallbackHandlerHelper;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentResponse;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUnavailabilityDates;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.civil.utils.RespondentsResponsesUtil.isRespondentsResponseSatisfied;

@SuppressWarnings({"checkstyle:Indentation", "checkstyle:EmptyLineSeparator"})
@Service
@RequiredArgsConstructor
public class RespondToApplicationHandler extends CallbackHandler {

    private final ObjectMapper objectMapper;
    private final CaseDetailsConverter caseDetailsConverter;
    private final CallbackHandlerHelper callbackHandlerHelper;

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
            callbackKey(ABOUT_TO_SUBMIT), this::submitClaim,
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

    private CallbackResponse submitClaim(CallbackParams callbackParams) {

        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        List<Element<GARespondentResponse>> respondentsResponses =
            addApplication(buildApplication(caseData), caseData.getRespondentsResponses());

        caseDataBuilder.respondentsResponses(respondentsResponses);

        CaseState newState = isRespondentsResponseSatisfied(caseData, caseDataBuilder)
            ? APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION
            : AWAITING_RESPONDENT_RESPONSE;
        callbackHandlerHelper.updateParentWithGAState(caseData, newState.getDisplayedValue());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .state(newState.toString())
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    private List<Element<GARespondentResponse>> addApplication(GARespondentResponse gaRespondentResponseBuilder,
                                                             List<Element<GARespondentResponse>> respondentsResponses) {

        List<Element<GARespondentResponse>> newApplication = ofNullable(respondentsResponses).orElse(newArrayList());
        newApplication.add(element(gaRespondentResponseBuilder));

        return newApplication;
    }


    private GARespondentResponse buildApplication(CaseData caseData) {

        GARespondentResponse.GARespondentResponseBuilder gaRespondentResponseBuilder = GARespondentResponse.builder();

        gaRespondentResponseBuilder
            .generalAppRespondent1Representative(caseData.getGeneralAppRespondent1Representative()
                                                            .getGeneralAppRespondent1Representative())
            .gaHearingDetails(caseData.getHearingDetailsResp()).build();

        return gaRespondentResponseBuilder.build();
    }
}
