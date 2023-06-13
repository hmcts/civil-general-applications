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
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GARespondentRepresentative;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentResponse;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUnavailabilityDates;
import uk.gov.hmcts.reform.civil.service.GeneralAppLocationRefDataService;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;
import uk.gov.hmcts.reform.civil.service.docmosis.applicationdraft.GeneralApplicationDraftGenerator;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;
import static uk.gov.hmcts.reform.civil.enums.GADebtorPaymentPlanGAspec.PAYFULL;
import static uk.gov.hmcts.reform.civil.enums.GARespondentDebtorOfferOptionsGAspec.ACCEPT;
import static uk.gov.hmcts.reform.civil.enums.GARespondentDebtorOfferOptionsGAspec.DECLINE;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.model.common.DynamicList.fromList;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;
import static uk.gov.hmcts.reform.civil.utils.RespondentsResponsesUtil.isRespondentsResponseSatisfied;

@Service
@RequiredArgsConstructor
public class RespondToApplicationHandler extends CallbackHandler {

    private final ObjectMapper objectMapper;
    private final CaseDetailsConverter caseDetailsConverter;
    private final ParentCaseUpdateHelper parentCaseUpdateHelper;
    private final IdamClient idamClient;
    private final GeneralApplicationDraftGenerator gaDraftGenerator;
    private final AssignCategoryId assignCategoryId;
    private final GeneralAppLocationRefDataService locationRefDataService;

    private static final String RESPONSE_MESSAGE = "# You have provided the requested information";
    private static final String JUDGES_REVIEW_MESSAGE =
        "<p> The application and your response will be reviewed by a Judge. </p>";
    private static final String CONFIRMATION_SUMMARY = "<br/><p> In relation to the following application(s): </p>"
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
    public static final String RESPONDENT_RESPONSE_EXISTS = "The application has already been responded to.";
    public static final String PAYMENT_DATE_CANNOT_BE_IN_PAST =
        "The date entered cannot be in the past.";

    public static final String PREFERRED_TYPE_IN_PERSON = "IN_PERSON";
    private static final List<CaseEvent> EVENTS = Collections.singletonList(RESPOND_TO_APPLICATION);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START), this::applicationValidation,
            callbackKey(MID, "validate-debtor-offer"), this::validateDebtorOffer,
            callbackKey(MID, "hearing-screen-response"), this::hearingScreenResponse,
            callbackKey(ABOUT_TO_SUBMIT), this::submitClaim,
            callbackKey(SUBMITTED), this::buildResponseConfirmation
        );
    }

    private AboutToStartOrSubmitCallbackResponse applicationValidation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        String authToken = callbackParams.getParams().get(BEARER_TOKEN).toString();

        if (caseData.getGeneralAppType().getTypes().contains(GeneralApplicationTypes.VARY_JUDGEMENT)
            && caseData.getParentClaimantIsApplicant().equals(NO)) {
            caseDataBuilder.generalAppVaryJudgementType(YesOrNo.YES);
        } else {
            caseDataBuilder.generalAppVaryJudgementType(NO);
        }

        caseDataBuilder
            .hearingDetailsResp(
                GAHearingDetails
                    .builder()
                    .hearingPreferredLocation(getLocationsFromList(locationRefDataService
                                                                       .getCourtLocations(authToken)))
                    .build());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(applicationExistsValidation(callbackParams))
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    private CallbackResponse validateDebtorOffer(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        ArrayList<String> errors = new ArrayList<>();
        if (ofNullable(caseData.getGaRespondentDebtorOffer()).isPresent()
            && caseData.getGaRespondentDebtorOffer().getRespondentDebtorOffer().equals(DECLINE)) {
            if (caseData.getGaRespondentDebtorOffer().getPaymentPlan().equals(PAYFULL)
                && !now().isBefore(caseData.getGaRespondentDebtorOffer().getPaymentSetDate())) {
                errors.add(PAYMENT_DATE_CANNOT_BE_IN_PAST);
            }
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    private DynamicList getLocationsFromList(final List<LocationRefData> locations) {
        return fromList(locations.stream().map(location -> new StringBuilder().append(location.getSiteName())
                .append(" - ").append(location.getCourtAddress())
                .append(" - ").append(location.getPostcode()).toString())
                            .collect(Collectors.toList()));
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
        UserDetails userDetails = idamClient.getUserDetails(callbackParams.getParams().get(BEARER_TOKEN).toString());
        List<Element<GARespondentResponse>> respondentResponse = caseData.getRespondentsResponses();

        List<String> errors = new ArrayList<>();
        if (caseData.getCcdState() == CaseState
            .APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION
        ) {
            errors.add(APPLICATION_RESPONSE_PRESENT);
        }
        if (respondentResponse != null) {
            Optional<Element<GARespondentResponse>> respondentResponseElement = respondentResponse.stream().findAny();
            if (respondentResponseElement.isPresent()) {
                String respondentResponseId = respondentResponseElement.get().getValue().getGaRespondentDetails();
                if (respondentResponseId.equals(userDetails.getId())) {
                    errors.add(RESPONDENT_RESPONSE_EXISTS);
                }
            }
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
            appType,
            JUDGES_REVIEW_MESSAGE
        );
    }

    private CallbackResponse submitClaim(CallbackParams callbackParams) {

        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        UserDetails userDetails = idamClient.getUserDetails(callbackParams.getParams().get(BEARER_TOKEN).toString());

        List<Element<GARespondentResponse>> respondentsResponses =
            addResponse(buildResponse(caseData, userDetails), caseData.getRespondentsResponses());

        caseDataBuilder.respondentsResponses(respondentsResponses);
        caseDataBuilder.hearingDetailsResp(populateHearingDetailsResp(caseData));
        caseDataBuilder.generalAppRespondent1Representative(GARespondentRepresentative.builder().build());

        CaseDocument gaDraftDocument;
        if (isRespondentsResponseSatisfied(caseData, caseDataBuilder.build())
            && isNull(caseData.getJudicialDecision())) {

            gaDraftDocument = gaDraftGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            List<Element<CaseDocument>> draftApplicationList = newArrayList();

            draftApplicationList.addAll(wrapElements(gaDraftDocument));

            assignCategoryId.assignCategoryIdToCollection(draftApplicationList,
                                                          document -> document.getValue().getDocumentLink(),
                                                          AssignCategoryId.APPLICATIONS);
            caseDataBuilder.gaDraftDocument(draftApplicationList);
        }
        CaseData updatedCaseData = caseDataBuilder.build();

        CaseState newState = isRespondentsResponseSatisfied(caseData, updatedCaseData)
            ? APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION
            : AWAITING_RESPONDENT_RESPONSE;
        parentCaseUpdateHelper.updateParentWithGAState(updatedCaseData, newState.getDisplayedValue());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .state(newState.toString())
            .data(updatedCaseData.toMap(objectMapper))
            .build();
    }

    private GAHearingDetails populateHearingDetailsResp(CaseData caseData) {
        GAHearingDetails gaHearingDetailsResp;
        String preferredType = caseData.getHearingDetailsResp().getHearingPreferencesPreferredType().name();
        if (preferredType.equals(PREFERRED_TYPE_IN_PERSON)
            && (caseData.getHearingDetailsResp().getHearingPreferredLocation() != null)) {
            String applicationLocationLabel = caseData.getHearingDetailsResp()
                .getHearingPreferredLocation().getValue()
                .getLabel();
            DynamicList dynamicLocationList = fromList(List.of(applicationLocationLabel));
            Optional<DynamicListElement> first = dynamicLocationList.getListItems().stream()
                .filter(l -> l.getLabel().equals(applicationLocationLabel)).findFirst();
            first.ifPresent(dynamicLocationList::setValue);
            gaHearingDetailsResp = caseData.getHearingDetailsResp().toBuilder()
                .hearingPreferredLocation(dynamicLocationList).build();

        } else {
            gaHearingDetailsResp = caseData.getHearingDetailsResp().toBuilder()
                .hearingPreferredLocation(DynamicList.builder().build()).build();
        }
        return gaHearingDetailsResp;
    }

    private List<Element<GARespondentResponse>> addResponse(GARespondentResponse gaRespondentResponseBuilder,
                                                            List<Element<GARespondentResponse>> respondentsResponses) {

        List<Element<GARespondentResponse>> newApplication = ofNullable(respondentsResponses).orElse(newArrayList());
        newApplication.add(element(gaRespondentResponseBuilder));

        return newApplication;
    }

    private GARespondentResponse buildResponse(CaseData caseData, UserDetails userDetails) {

        YesOrNo generalOther = NO;
        if (Objects.nonNull(caseData.getGeneralAppConsentOrder())) {
            generalOther = caseData.getGaRespondentConsent();
        } else if (caseData.getGeneralAppType().getTypes().contains(GeneralApplicationTypes.VARY_JUDGEMENT)
            && caseData.getParentClaimantIsApplicant().equals(NO)) {

            if (ofNullable(caseData.getGaRespondentDebtorOffer()).isPresent()
                && caseData.getGaRespondentDebtorOffer().getRespondentDebtorOffer().equals(ACCEPT)) {
                generalOther = YES;
            }
        }

        GARespondentResponse.GARespondentResponseBuilder gaRespondentResponseBuilder = GARespondentResponse.builder();

        gaRespondentResponseBuilder
            .generalAppRespondent1Representative(caseData.getGeneralAppRespondent1Representative() == null
                                                     ? generalOther
                                                     : caseData.getGeneralAppRespondent1Representative()
                .getGeneralAppRespondent1Representative())
            .gaHearingDetails(populateHearingDetailsResp(caseData))
            .gaRespondentDetails(userDetails.getId()).build();

        return gaRespondentResponseBuilder.build();
    }
}
