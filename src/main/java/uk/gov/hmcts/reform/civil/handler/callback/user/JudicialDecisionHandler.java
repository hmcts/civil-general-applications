package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingSupportRequirements;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentResponse;
import uk.gov.hmcts.reform.civil.service.DeadlinesCalculator;
import uk.gov.hmcts.reform.civil.service.GeneralAppLocationRefDataService;
import uk.gov.hmcts.reform.civil.service.JudicialDecisionHelper;
import uk.gov.hmcts.reform.civil.service.JudicialDecisionWrittenRepService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.JUDGE_MAKES_DECISION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.EXTEND_TIME;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STAY_THE_CLAIM;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STRIKE_OUT;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.formatLocalDate;
import static uk.gov.hmcts.reform.civil.model.common.DynamicList.fromList;

@Service
@RequiredArgsConstructor
public class JudicialDecisionHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(JUDGE_MAKES_DECISION);

    private final GeneralAppLocationRefDataService locationRefDataService;
    private final JudicialDecisionHelper helper;

    private static final String VALIDATE_MAKE_DECISION_SCREEN = "validate-make-decision-screen";
    private static final String VALIDATE_MAKE_AN_ORDER = "validate-make-an-order";
    private static final int ONE_V_ONE = 0;
    private static final String EMPTY_STRING = "";

    private final DeadlinesCalculator deadlinesCalculator;
    private static final int NUMBER_OF_DEADLINE_DAYS = 5;

    private static final String VALIDATE_REQUEST_MORE_INFO_SCREEN = "validate-request-more-info-screen";
    private static final String VALIDATE_HEARING_ORDER_SCREEN = "validate-hearing-order-screen";

    private static final String JUDICIAL_TIME_EST_TEXT_1 = "Applicant estimates "
        + "%s. Respondent estimates %s.";
    private static final String JUDICIAL_TIME_EST_TEXT_2 = " Both applicant and respondent estimate it would take %s.";
    private static final String JUDICIAL_TIME_EST_TEXT_3 = "Applicant estimates "
        + "%s. Respondent1 estimates %s. Respondent2 estimates %s.";
    private static final String JUDICIAL_APPLICANT_VULNERABILITY_TEXT = "Applicant requires support with regards to "
        + "vulnerability\n";
    private static final String JUDICIAL_RESPONDENT_VULNERABILITY_TEXT = "\n\nRespondent requires support with "
        + "regards to vulnerability\n";

    /*private static final String JUDICIAL_COURT_LOC_TEXT_1 = "Applicant estimates "
        + "%s. Respondent estimates %s.";
    private static final String JUDICIAL_COURT_LOC_TEXT_2 = " Both applicant and respondent
    estimate it would take %s.";*/

    private static final String JUDICIAL_PREF_TYPE_TEXT_1 = "Applicant prefers "
        + "%s. Respondent prefers %s.";
    private static final String JUDICIAL_PREF_TYPE_TEXT_2 = " Both applicant and respondent prefer %s.";
    private static final String JUDICIAL_PREF_TYPE_TEXT_3 = "Applicant prefers "
        + "%s. Respondent1 prefers %s. Respondent2 prefers %s.";
    private static final String JUDICIAL_SUPPORT_REQ_TEXT_1 = "Applicant require "
        + "%s. Respondent require %s.";
    private static final String JUDICIAL_SUPPORT_REQ_TEXT_2 = " Both applicant and respondent require %s.";
    private static final String JUDICIAL_SUPPORT_REQ_TEXT_3 = "Applicant require "
        + "%s. Respondent1 require %s. Respondent2 require %s.";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yy");
    private static final DateTimeFormatter DATE_FORMATTER_SUBMIT_CALLBACK = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String VALIDATE_WRITTEN_REPRESENTATION_DATE = "ga-validate-written-representation-date";
    private static final String JUDICIAL_RECITAL_TEXT = "Upon reading the application of %s dated %s and upon the "
        + "application of %s dated %s and upon considering the information provided by the parties";
    private static final String JUDICIAL_HEARING_RECITAL_TEXT = "Upon reading the "
        + "application of %s dated %s and upon considering the information provided by the parties";
    private static final String JUDICIAL_HEARING_TYPE = "Hearing type is %s";
    private static final String JUDICIAL_TIME_ESTIMATE = "Estimated length of hearing is %s";
    private static final String JUDICIAL_SEQUENTIAL_DATE =
        "The respondent may upload any written representations by 4pm on %s";
    private static final String JUDICIAL_SEQUENTIAL_APPLICANT_DATE =
        "The applicant may upload any written representations by 4pm on %s";
    private static final String JUDICIAL_CONCURRENT_DATE =
        "The applicant and respondent must respond with written representations by 4pm on %s";
    private static final String JUDICIAL_HEARING_REQ = "Hearing requirements %s";
    private static final String DISMISSAL_ORDER_TEXT = "This application is dismissed.\n\n"
        + "[Insert Draft Order from application]\n\n"
        + "A person who was not notified of the application before this order was made may apply to have the "
        + "order set aside or varied. Any application under this paragraph must be made within 7 days after "
        + "notification of the order.";
    private static final String PERSON_NOT_NOTIFIED_TEXT = "\n\n"
        + "A person who was not notified of the application"
        + " before the order was made may apply to have the order set aside or varied."
        + " Any application under this paragraph must be made within 7 days.";

    private final JudicialDecisionWrittenRepService judicialDecisionWrittenRepService;
    public static final String RESPOND_TO_DIRECTIONS_DATE_REQUIRED = "The date, by which the response to direction"
        + " should be given, is required.";
    public static final String RESPOND_TO_DIRECTIONS_DATE_IN_PAST = "The date, by which the response to direction"
        + " should be given, cannot be in past.";

    public static final String REQUESTED_MORE_INFO_BY_DATE_REQUIRED = "The date, by which the applicant must respond, "
        + "is required.";
    public static final String REQUESTED_MORE_INFO_BY_DATE_IN_PAST = "The date, by which the applicant must respond, "
        + "cannot be in past.";
    public static final String MAKE_DECISION_APPROVE_BY_DATE_IN_PAST = "The date entered cannot be in the past.";

    private final ObjectMapper objectMapper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START),
            this::checkInputForNextPage,
            callbackKey(MID, VALIDATE_MAKE_AN_ORDER),
            this::gaValidateMakeAnOrder,
            callbackKey(MID, VALIDATE_MAKE_DECISION_SCREEN),
            this::gaValidateMakeDecisionScreen,
            callbackKey(MID, VALIDATE_REQUEST_MORE_INFO_SCREEN),
            this::gaValidateRequestMoreInfoScreen,
            callbackKey(MID, VALIDATE_WRITTEN_REPRESENTATION_DATE),
            this::gaValidateWrittenRepresentationsDate,
            callbackKey(MID, VALIDATE_HEARING_ORDER_SCREEN),
            this::gaValidateHearingOrder,
            callbackKey(ABOUT_TO_SUBMIT),
            this::setJudgeBusinessProcess,
            callbackKey(SUBMITTED),
            this::buildConfirmation
        );

    }

    private CallbackResponse checkInputForNextPage(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        YesOrNo isCloaked = helper.isApplicationCloaked(caseData);
        caseDataBuilder.applicationIsCloaked(isCloaked);
        caseDataBuilder.judicialDecisionMakeOrder(makeAnOrderBuilder(caseData, callbackParams).build());

        caseDataBuilder.judgeRecitalText(getJudgeRecitalPrepopulatedText(caseData))
            .directionInRelationToHearingText(PERSON_NOT_NOTIFIED_TEXT).build();

        caseDataBuilder.judicialGeneralHearingOrderRecital(getJudgeHearingRecitalPrepopulatedText(caseData))
            .judicialGOHearingDirections(PERSON_NOT_NOTIFIED_TEXT).build();

        YesOrNo isAppAndRespSameHearingPref = (caseData.getHearingDetailsResp() != null
            && caseData.getRespondentsResponses() != null
            && caseData.getRespondentsResponses().size() == 1
            && caseData.getGeneralAppHearingDetails().getHearingPreferencesPreferredType().getDisplayedValue()
            .equals(caseData.getRespondentsResponses().stream().iterator().next().getValue().getGaHearingDetails()
                        .getHearingPreferencesPreferredType().getDisplayedValue()))
            ? YES : NO;

        GAJudgesHearingListGAspec.GAJudgesHearingListGAspecBuilder gaJudgesHearingListGAspecBuilder;
        if (caseData.getJudicialListForHearing() != null) {
            gaJudgesHearingListGAspecBuilder = caseData.getJudicialListForHearing().toBuilder();
        } else {
            gaJudgesHearingListGAspecBuilder = GAJudgesHearingListGAspec.builder();
        }

        YesOrNo isAppAndRespSameSupportReq = (caseData.getHearingDetailsResp() != null
            && caseData.getRespondentsResponses() != null
            && caseData.getRespondentsResponses().size() == 1
            && caseData.getGeneralAppHearingDetails().getSupportRequirement() != null
            && caseData.getRespondentsResponses().get(0).getValue().getGaHearingDetails()
            .getSupportRequirement() != null
            && caseData.getHearingDetailsResp().getSupportRequirement() != null
            && checkIfAppAndRespHaveSameSupportReq(caseData))
            ? YES : NO;

        /*Hearing Preferred Location in both applicant and respondent haven't yet implemented.
        Uncomment the below code once Hearing Preferred Location is implemented.*/
        String authToken = callbackParams.getParams().get(BEARER_TOKEN).toString();
        DynamicList dynamicLocationList = fromList(locationRefDataService.getCourtLocations(authToken));
        boolean isAppAndRespSameCourtLocPref = helper.isApplicantAndRespondentLocationPrefSame(caseData);
        if (isAppAndRespSameCourtLocPref) {
            String applicationLocationLabel = caseData.getGeneralAppHearingDetails().getHearingPreferredLocation()
                .getValue().getLabel();
            Optional<DynamicListElement> first = dynamicLocationList.getListItems().stream()
                .filter(l -> l.getLabel().equals(applicationLocationLabel)).findFirst();
            first.ifPresent(dynamicLocationList::setValue);
        }

        YesOrNo isAppAndRespSameTimeEst = (caseData.getHearingDetailsResp() != null
            && caseData.getRespondentsResponses() != null
            && caseData.getRespondentsResponses().size() == 1
            && caseData.getGeneralAppHearingDetails().getHearingDuration().getDisplayedValue()
            .equals(caseData.getRespondentsResponses().stream().iterator().next().getValue().getGaHearingDetails()
                        .getHearingDuration().getDisplayedValue()))
            ? YES : NO;

        caseDataBuilder.judicialListForHearing(gaJudgesHearingListGAspecBuilder
                                                   .hearingPreferredLocation(dynamicLocationList)
                                                   .hearingPreferencesPreferredTypeLabel1(
                                                       getJudgeHearingPrefType(caseData, isAppAndRespSameHearingPref))
                                                   .judgeHearingCourtLocationText1(
                                                       getJudgeHearingCourtLoc())
                                                   .judgeHearingTimeEstimateText1(
                                                       getJudgeHearingTimeEst(caseData, isAppAndRespSameTimeEst))
                                                   .judgeHearingSupportReqText1(
                                                       getJudgeHearingSupportReq(caseData, isAppAndRespSameSupportReq))
                                                   .judicialVulnerabilityText(
                                                       getJudgeVulnerabilityText(caseData)).build());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    public GAJudicialMakeAnOrder.GAJudicialMakeAnOrderBuilder makeAnOrderBuilder(CaseData caseData,
                                                                                 CallbackParams callbackParams) {
        GAJudicialMakeAnOrder.GAJudicialMakeAnOrderBuilder makeAnOrderBuilder;
        if (caseData.getJudicialDecisionMakeOrder() != null && callbackParams.getType() != ABOUT_TO_START) {
            makeAnOrderBuilder = caseData.getJudicialDecisionMakeOrder().toBuilder();

            makeAnOrderBuilder.orderText(caseData.getJudicialDecisionMakeOrder().getOrderText() == null
                                             ? caseData.getGeneralAppDetailsOfOrder() + PERSON_NOT_NOTIFIED_TEXT
                                             : caseData.getJudicialDecisionMakeOrder().getOrderText())
                .judgeRecitalText(caseData.getJudicialDecisionMakeOrder().getJudgeRecitalText())
                .dismissalOrderText(caseData.getJudicialDecisionMakeOrder().getDismissalOrderText() == null
                                        ? DISMISSAL_ORDER_TEXT
                                        : caseData.getJudicialDecisionMakeOrder().getDismissalOrderText());
        } else {
            makeAnOrderBuilder = GAJudicialMakeAnOrder.builder();
            makeAnOrderBuilder.orderText(caseData.getGeneralAppDetailsOfOrder() + PERSON_NOT_NOTIFIED_TEXT)
                .judgeRecitalText(getJudgeRecitalPrepopulatedText(caseData))
                .dismissalOrderText(DISMISSAL_ORDER_TEXT);
        }

        GAJudicialMakeAnOrder judicialDecisionMakeOrder = caseData.getJudicialDecisionMakeOrder();
        if (judicialDecisionMakeOrder != null) {
            return makeAnOrderBuilder
                .displayjudgeApproveEditOptionDate(checkApplicationTypeForDate(caseData) && APPROVE_OR_EDIT
                    .equals(judicialDecisionMakeOrder.getMakeAnOrder()) ? YES : NO)
                .displayjudgeApproveEditOptionDoc(checkApplicationTypeForDoc(caseData) && APPROVE_OR_EDIT
                    .equals(judicialDecisionMakeOrder.getMakeAnOrder()) ? YES : NO);
        }

        return makeAnOrderBuilder
            .displayjudgeApproveEditOptionDate(checkApplicationTypeForDate(caseData) ? YES : NO)
            .displayjudgeApproveEditOptionDoc(checkApplicationTypeForDoc(caseData) ? YES : NO);
    }

    /*Return True if General Application types are only Extend Time or/and Strike Out
    Else, Return False*/
    private boolean checkApplicationTypeForDoc(CaseData caseData) {

        if (caseData.getGeneralAppType() == null) {
            return false;
        }
        List<GeneralApplicationTypes> validGATypes = Arrays.asList(EXTEND_TIME, STRIKE_OUT);
        return caseData.getGeneralAppType().getTypes().stream().anyMatch(validGATypes::contains);

    }

    /*Return True if General Application types are Extend Time or/and Stay the Claim
    Else, Return False*/
    private boolean checkApplicationTypeForDate(CaseData caseData) {

        if (caseData.getGeneralAppType() == null) {
            return false;
        }
        List<GeneralApplicationTypes> validGATypes = Arrays.asList(EXTEND_TIME, STAY_THE_CLAIM);
        return caseData.getGeneralAppType().getTypes().stream().anyMatch(validGATypes::contains);
    }

    private Boolean checkIfAppAndRespHaveSameSupportReq(CaseData caseData) {

        if (caseData.getRespondentsResponses().stream().iterator().next().getValue()
            .getGaHearingDetails().getSupportRequirement() != null) {

            ArrayList<GAHearingSupportRequirements> applicantSupportReq
                = caseData.getGeneralAppHearingDetails().getSupportRequirement().stream().sorted()
                .collect(Collectors.toCollection(ArrayList::new));

            ArrayList<GAHearingSupportRequirements> respondentSupportReq
                = caseData.getRespondentsResponses().stream().iterator().next().getValue()
                .getGaHearingDetails().getSupportRequirement().stream().sorted()
                .collect(Collectors.toCollection(ArrayList::new));

            return applicantSupportReq.equals(respondentSupportReq);
        }

        return false;
    }

    private String getJudgeRecitalPrepopulatedText(CaseData caseData) {
        return format(
            JUDICIAL_RECITAL_TEXT,
            (caseData.getParentClaimantIsApplicant() == null
                || YES.equals(caseData.getParentClaimantIsApplicant()))
                ? "Claimant" : "Defendant",
            DATE_FORMATTER.format(caseData.getCreatedDate()),
            caseData.getApplicantPartyName(),
            DATE_FORMATTER.format(LocalDate.now())
        );
    }

    private String getJudgeHearingRecitalPrepopulatedText(CaseData caseData) {
        return format(
            JUDICIAL_HEARING_RECITAL_TEXT,
            (caseData.getParentClaimantIsApplicant() == null
                || YES.equals(caseData.getParentClaimantIsApplicant()))
                ? "Claimant" : "Defendant",
            DATE_FORMATTER.format(caseData.getCreatedDate())
        );
    }

    private CallbackResponse gaValidateMakeDecisionScreen(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        GAJudicialMakeAnOrder judicialDecisionMakeOrder = caseData.getJudicialDecisionMakeOrder();
        List<String> errors = Collections.emptyList();
        if (judicialDecisionMakeOrder != null) {
            errors = validateUrgencyDates(judicialDecisionMakeOrder);
            errors.addAll(validateJudgeOrderRequestDates(judicialDecisionMakeOrder));

            caseDataBuilder
                .judicialDecisionMakeOrder(makeAnOrderBuilder(caseData, callbackParams).build());
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    public List<String> validateUrgencyDates(GAJudicialMakeAnOrder judicialDecisionMakeOrder) {
        List<String> errors = new ArrayList<>();
        if (GIVE_DIRECTIONS_WITHOUT_HEARING.equals(judicialDecisionMakeOrder.getMakeAnOrder())
            && judicialDecisionMakeOrder.getDirectionsResponseByDate() == null) {
            errors.add(RESPOND_TO_DIRECTIONS_DATE_REQUIRED);
        }

        if (GIVE_DIRECTIONS_WITHOUT_HEARING.equals(judicialDecisionMakeOrder.getMakeAnOrder())
            && judicialDecisionMakeOrder.getDirectionsResponseByDate() != null) {
            LocalDate directionsResponseByDate = judicialDecisionMakeOrder.getDirectionsResponseByDate();
            if (LocalDate.now().isAfter(directionsResponseByDate)) {
                errors.add(RESPOND_TO_DIRECTIONS_DATE_IN_PAST);
            }
        }
        return errors;
    }

    private CallbackResponse gaValidateMakeAnOrder(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        caseDataBuilder.judicialDecisionMakeOrder(makeAnOrderBuilder(caseData, callbackParams).build());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    private CallbackResponse gaValidateRequestMoreInfoScreen(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        GAJudicialRequestMoreInfo judicialRequestMoreInfo = caseData.getJudicialDecisionRequestMoreInfo();
        List<String> errors = judicialRequestMoreInfo != null
            ? validateDatesForRequestMoreInfoScreen(judicialRequestMoreInfo)
            : Collections.emptyList();

        if (judicialRequestMoreInfo != null
            && SEND_APP_TO_OTHER_PARTY.equals(judicialRequestMoreInfo.getRequestMoreInfoOption())) {
            LocalDateTime deadlineForMoreInfoSubmission = deadlinesCalculator
                .calculateApplicantResponseDeadline(
                    LocalDateTime.now(), NUMBER_OF_DEADLINE_DAYS);

            caseDataBuilder
                .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo
                                                     .builder()
                                                     .requestMoreInfoOption(caseData
                                                                                .getJudicialDecisionRequestMoreInfo()
                                                                                .getRequestMoreInfoOption())
                                                     .deadlineForMoreInfoSubmission(deadlineForMoreInfoSubmission)
                                                     .build());
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    public List<String> validateDatesForRequestMoreInfoScreen(GAJudicialRequestMoreInfo judicialRequestMoreInfo) {
        List<String> errors = new ArrayList<>();
        if (REQUEST_MORE_INFORMATION.equals(judicialRequestMoreInfo.getRequestMoreInfoOption())) {
            if (judicialRequestMoreInfo.getJudgeRequestMoreInfoByDate() == null) {
                errors.add(REQUESTED_MORE_INFO_BY_DATE_REQUIRED);
            } else {
                if (LocalDate.now().isAfter(judicialRequestMoreInfo.getJudgeRequestMoreInfoByDate())) {
                    errors.add(REQUESTED_MORE_INFO_BY_DATE_IN_PAST);
                }
            }
        }
        return errors;
    }

    private CaseData.CaseDataBuilder getSharedData(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        // second idam call is workaround for null pointer when hiding field in getIdamEmail callback
        return caseData.toBuilder();
    }

    private CallbackResponse setJudgeBusinessProcess(CallbackParams callbackParams) {
        CaseData.CaseDataBuilder dataBuilder = getSharedData(callbackParams);
        dataBuilder.businessProcess(BusinessProcess.ready(JUDGE_MAKES_DECISION)).build();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(dataBuilder.build().toMap(objectMapper))
            .build();
    }

    private SubmittedCallbackResponse buildConfirmation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        GAJudicialDecision judicialDecision = caseData.getJudicialDecision();
        if (judicialDecision == null || judicialDecision.getDecision() == null) {
            throw new IllegalArgumentException("Missing data during submission of judicial decision");
        }
        String confirmationHeader = "# Your order has been made";
        String body = "<br/><br/>";
        if (REQUEST_MORE_INFO.equals(judicialDecision.getDecision())) {
            GAJudicialRequestMoreInfo requestMoreInfo = caseData.getJudicialDecisionRequestMoreInfo();
            if (requestMoreInfo != null) {
                if (REQUEST_MORE_INFORMATION.equals(requestMoreInfo.getRequestMoreInfoOption())) {
                    if (requestMoreInfo.getJudgeRequestMoreInfoByDate() != null) {
                        confirmationHeader = "# You have requested more information";
                        body = "<br/><p>The applicant will be notified. They will need to provide a response by "
                            + DATE_FORMATTER_SUBMIT_CALLBACK.format(requestMoreInfo.getJudgeRequestMoreInfoByDate())
                            + "</p>";
                    } else {
                        throw new IllegalArgumentException("Missing data during submission of judicial decision");
                    }
                } else if (SEND_APP_TO_OTHER_PARTY.equals(requestMoreInfo.getRequestMoreInfoOption())) {
                    LocalDateTime submissionEndDate = caseData.getJudicialDecisionRequestMoreInfo()
                        .getDeadlineForMoreInfoSubmission();
                    confirmationHeader = "# You have requested a response";
                    body = "<br/><p>The parties will be notified. They will need to provide a response by "
                        + DATE_FORMATTER_SUBMIT_CALLBACK.format(submissionEndDate)
                        + "</p>";
                }
            } else {
                throw new IllegalArgumentException("Missing data during submission of judicial decision");
            }
        }
        return SubmittedCallbackResponse.builder()
            .confirmationHeader(confirmationHeader)
            .confirmationBody(body)
            .build();
    }

    private CallbackResponse gaValidateWrittenRepresentationsDate(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        GAJudicialWrittenRepresentations judicialWrittenRepresentationsDate =
            caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations();

        List<String> errors;
        errors = judicialWrittenRepresentationsDate != null
            ? judicialDecisionWrittenRepService.validateWrittenRepresentationsDates(judicialWrittenRepresentationsDate)
            : Collections.emptyList();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        if (caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations().getWrittenOption()
            .equals(SEQUENTIAL_REPRESENTATIONS)) {
            caseDataBuilder.judicialSequentialDateText(getJudicalSequentialDatePupulatedText(caseData)).build();
            caseDataBuilder.judicialApplicanSequentialDateText(
                getJudicalApplicantSequentialDatePupulatedText(caseData)).build();
        } else {
            caseDataBuilder.judicialConcurrentDateText(getJudicalConcurrentDatePupulatedText(caseData)).build();
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .errors(errors)
            .build();
    }

    private CallbackResponse gaValidateHearingOrder(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        caseDataBuilder.judicialHearingGeneralOrderHearingText(getJudgeHearingPrePopulatedText(caseData))
            .judicialHearingGOHearingReqText(populateJudgeGOSupportRequirement(caseData))
            .judicialGeneralOrderHearingEstimationTimeText(getJudgeHearingTimeEstPrePopulatedText(caseData)).build();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    private String getJudgeHearingPrePopulatedText(CaseData caseData) {
        return format(
            JUDICIAL_HEARING_TYPE,
            caseData.getJudicialListForHearing().getHearingPreferencesPreferredType().getDisplayedValue()
        );
    }

    private String populateJudgeGOSupportRequirement(CaseData caseData) {

        StringJoiner supportReq = new StringJoiner(", ");

        if (caseData.getJudicialListForHearing().getJudicialSupportRequirement() != null) {
            caseData.getJudicialListForHearing().getJudicialSupportRequirement()
                .forEach(sr -> {
                    supportReq.add(sr.getDisplayedValue());
                });

            return format(
                JUDICIAL_HEARING_REQ, supportReq);
        }

        return "";
    }

    private String getJudgeHearingTimeEstPrePopulatedText(CaseData caseData) {
        return format(
            JUDICIAL_TIME_ESTIMATE, caseData.getJudicialListForHearing().getJudicialTimeEstimate().getDisplayedValue());
    }

    private String getJudicalSequentialDatePupulatedText(CaseData caseData) {
        return format(
            JUDICIAL_SEQUENTIAL_DATE, formatLocalDate(caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                                          .getWrittenSequentailRepresentationsBy(), DATE));
    }

    private String getJudicalApplicantSequentialDatePupulatedText(CaseData caseData) {
        return format(
            JUDICIAL_SEQUENTIAL_APPLICANT_DATE,
            formatLocalDate(caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                .getSequentialApplicantMustRespondWithin(), DATE)
        );
    }

    private String getJudicalConcurrentDatePupulatedText(CaseData caseData) {
        return format(
            JUDICIAL_CONCURRENT_DATE, formatLocalDate(caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                                          .getWrittenConcurrentRepresentationsBy(), DATE));
    }

    private String getJudgeHearingPrefType(CaseData caseData, YesOrNo isAppAndRespSameHearingPref) {
        String respondet1HearingType = null;
        String respondent2HearingType = null;

        if (caseData.getGeneralAppUrgencyRequirement() != null
            && caseData.getGeneralAppUrgencyRequirement().getGeneralAppUrgency() == YesOrNo.YES) {
            return "Applicant prefers ".concat(caseData
                                                   .getGeneralAppHearingDetails().getHearingPreferencesPreferredType()
                                                   .getDisplayedValue());
        }

        if (caseData.getRespondentsResponses() != null && caseData.getRespondentsResponses().size() == 1) {
            return isAppAndRespSameHearingPref == YES ? format(JUDICIAL_PREF_TYPE_TEXT_2, caseData
                .getGeneralAppHearingDetails().getHearingPreferencesPreferredType().getDisplayedValue())
                : format(JUDICIAL_PREF_TYPE_TEXT_1, caseData.getGeneralAppHearingDetails()
                .getHearingPreferencesPreferredType().getDisplayedValue(), caseData.getRespondentsResponses() == null
                             ? StringUtils.EMPTY : caseData.getRespondentsResponses()
                .stream().iterator().next().getValue().getGaHearingDetails().getHearingPreferencesPreferredType()
                .getDisplayedValue());
        }

        if (caseData.getRespondentsResponses() != null && caseData.getRespondentsResponses().size() == 2) {
            List<Element<GARespondentResponse>> respondentResponce = caseData.getRespondentsResponses();
            String respondent1Id = caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getId();
            String respondent2Id = caseData.getGeneralAppRespondentSolicitors().get(1).getValue().getId();
            if (respondentResponce != null) {
                Optional<Element<GARespondentResponse>> respondenceElementOptional;
                respondenceElementOptional = respondentResponce.stream()
                    .filter(res -> res.getValue() != null && res.getValue().getGaRespondentDetails()
                        .equals(respondent1Id)).findFirst();
                Optional<Element<GARespondentResponse>> responseElementOptiona2 = respondentResponce.stream()
                    .filter(res2 -> res2.getValue() != null && res2.getValue().getGaRespondentDetails()
                        .equals(respondent2Id)).findFirst();
                if (respondenceElementOptional.isPresent()) {
                    respondet1HearingType = respondenceElementOptional.get().getValue()
                        .getGaHearingDetails().getHearingPreferencesPreferredType().getDisplayedValue();
                }
                if (responseElementOptiona2.isPresent()) {
                    respondent2HearingType = responseElementOptiona2.get().getValue()
                        .getGaHearingDetails().getHearingPreferencesPreferredType().getDisplayedValue();
                }
            }
            return format(JUDICIAL_PREF_TYPE_TEXT_3, caseData.getGeneralAppHearingDetails()
                .getHearingDuration().getDisplayedValue(), respondet1HearingType, respondent2HearingType);
        }

        return StringUtils.EMPTY;
    }

    private String getJudgeHearingTimeEst(CaseData caseData, YesOrNo isAppAndRespSameTimeEst) {
        String respondet1HearingDuration = null;
        String respondent2HearingDuration = null;

        if (caseData.getGeneralAppUrgencyRequirement() != null
            && caseData.getGeneralAppUrgencyRequirement().getGeneralAppUrgency() == YesOrNo.YES) {
            return "Applicant estimates ".concat(caseData.getGeneralAppHearingDetails()
                                                     .getHearingDuration().getDisplayedValue());
        }

        if (caseData.getRespondentsResponses() != null && caseData.getRespondentsResponses().size() == 1) {
            return isAppAndRespSameTimeEst == YES ? format(JUDICIAL_TIME_EST_TEXT_2, caseData
                .getGeneralAppHearingDetails().getHearingDuration().getDisplayedValue())
                : format(JUDICIAL_TIME_EST_TEXT_1, caseData.getGeneralAppHearingDetails()
                .getHearingDuration().getDisplayedValue(), caseData.getRespondentsResponses() == null
                ? StringUtils.EMPTY : caseData.getRespondentsResponses()
                .stream().iterator().next().getValue().getGaHearingDetails().getHearingDuration()
                .getDisplayedValue());
        }

        if (caseData.getRespondentsResponses() != null && caseData.getRespondentsResponses().size() == 2) {
            List<Element<GARespondentResponse>> respondentResponce = caseData.getRespondentsResponses();
            String respondent1Id = caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getId();
            String respondent2Id = caseData.getGeneralAppRespondentSolicitors().get(1).getValue().getId();
            if (respondentResponce != null) {
                Optional<Element<GARespondentResponse>> responseElementOptional = respondentResponce.stream()
                    .filter(res -> res.getValue() != null && res.getValue().getGaRespondentDetails()
                        .equals(respondent1Id)).findFirst();
                Optional<Element<GARespondentResponse>> respondenceElemetnOptiona2 = respondentResponce.stream()
                    .filter(res2 -> res2.getValue() != null && res2.getValue().getGaRespondentDetails()
                        .equals(respondent2Id)).findFirst();
                if (responseElementOptional.isPresent()) {
                    respondet1HearingDuration = responseElementOptional.get().getValue()
                        .getGaHearingDetails().getHearingDuration().getDisplayedValue();
                }
                if (respondenceElemetnOptiona2.isPresent()) {
                    respondent2HearingDuration = respondenceElemetnOptiona2.get().getValue()
                        .getGaHearingDetails().getHearingDuration().getDisplayedValue();
                }
            }
            return format(JUDICIAL_TIME_EST_TEXT_3, caseData.getGeneralAppHearingDetails()
                .getHearingDuration().getDisplayedValue(), respondet1HearingDuration, respondent2HearingDuration);
        }
        return StringUtils.EMPTY;
    }

    private String getJudgeVulnerabilityText(CaseData caseData) {

        YesOrNo applicantVulnerabilityResponse = caseData.getGeneralAppHearingDetails()
            .getVulnerabilityQuestionsYesOrNo();

        int responseCount = caseData.getRespondentsResponses() != null ? caseData.getRespondentsResponses().size() : 0;

        boolean hasRespondentVulnerabilityResponded = responseCount == 1
            ? caseData.getRespondentsResponses().get(ONE_V_ONE).getValue()
            .getGaHearingDetails().getVulnerabilityQuestionsYesOrNo() == YES ? TRUE : FALSE
            : FALSE;

        if (applicantVulnerabilityResponse == YES && hasRespondentVulnerabilityResponded == TRUE) {
            return JUDICIAL_APPLICANT_VULNERABILITY_TEXT
                .concat(caseData.getGeneralAppHearingDetails()
                            .getVulnerabilityQuestion()
                            .concat(JUDICIAL_RESPONDENT_VULNERABILITY_TEXT)
                            .concat(caseData.getRespondentsResponses().stream().iterator().next().getValue()
                                        .getGaHearingDetails().getVulnerabilityQuestion()));
        }

        return applicantVulnerabilityResponse == YES ? JUDICIAL_APPLICANT_VULNERABILITY_TEXT
            .concat(caseData.getGeneralAppHearingDetails()
                        .getVulnerabilityQuestion())
            : hasRespondentVulnerabilityResponded == TRUE
            ? ltrim(JUDICIAL_RESPONDENT_VULNERABILITY_TEXT).concat(caseData.getRespondentsResponses().stream()
                                                                       .iterator().next().getValue()
                                                                       .getGaHearingDetails().getVulnerabilityQuestion())
            : "No support required with regards to vulnerability";
    }

    private String ltrim(String str) {
        return str.replaceAll("^\\s+", EMPTY_STRING);
    }

    private String getJudgeHearingSupportReq(CaseData caseData, YesOrNo isAppAndRespSameSupportReq) {

        List<String> applicantSupportReq = Collections.emptyList();
        String appSupportReq = StringUtils.EMPTY;
        String resSupportReq = StringUtils.EMPTY;
        String res2SupportReq = StringUtils.EMPTY;

        if (caseData.getGeneralAppHearingDetails().getSupportRequirement() != null) {
            applicantSupportReq
                = caseData.getGeneralAppHearingDetails().getSupportRequirement().stream()
                .map(e -> e.getDisplayedValue()).collect(Collectors.toList());

            appSupportReq = String.join(", ", applicantSupportReq);
        }

        if (caseData.getGeneralAppUrgencyRequirement() != null
            && caseData.getGeneralAppUrgencyRequirement().getGeneralAppUrgency() == YesOrNo.YES) {
            return "Applicant require(s) ".concat(applicantSupportReq.isEmpty() ? "no support" : appSupportReq);

        }

        if (caseData.getRespondentsResponses() != null && caseData.getRespondentsResponses().size() == 1) {
            List<String> respondentSupportReq = Collections.emptyList();
            if (caseData.getRespondentsResponses() != null
                && caseData.getRespondentsResponses().size() == 1
                && caseData.getRespondentsResponses().get(ONE_V_ONE).getValue().getGaHearingDetails()
                .getSupportRequirement() != null) {
                respondentSupportReq
                    = caseData.getRespondentsResponses().stream().iterator().next().getValue()
                    .getGaHearingDetails().getSupportRequirement().stream().map(e -> e.getDisplayedValue())
                    .collect(Collectors.toList());

                resSupportReq = String.join(", ", respondentSupportReq);
            }

            return isAppAndRespSameSupportReq == YES ? format(JUDICIAL_SUPPORT_REQ_TEXT_2, appSupportReq)
                : format(JUDICIAL_SUPPORT_REQ_TEXT_1, applicantSupportReq.isEmpty() ? "no support" : appSupportReq,
                         respondentSupportReq.isEmpty() ? "no support" : resSupportReq
            );
        }

        if (caseData.getRespondentsResponses() != null && caseData.getRespondentsResponses().size() == 2) {
            List<Element<GARespondentResponse>> respondentResponce = caseData.getRespondentsResponses();
            String respondent1Id = caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getId();
            String respondent2Id = caseData.getGeneralAppRespondentSolicitors().get(1).getValue().getId();
            if (respondentResponce != null) {
                Optional<Element<GARespondentResponse>> responseElementOptional = respondentResponce.stream()
                    .filter(res -> res.getValue() != null && res.getValue().getGaRespondentDetails()
                        .equals(respondent1Id)).findFirst();
                Optional<Element<GARespondentResponse>> respondenceElemetnOptiona2 = respondentResponce.stream()
                    .filter(res2 -> res2.getValue() != null && res2.getValue().getGaRespondentDetails()
                        .equals(respondent2Id)).findFirst();
                List<String> respondent1SupportReq = Collections.emptyList();
                if (responseElementOptional.isPresent()) {
                    respondent1SupportReq = responseElementOptional.get().getValue().getGaHearingDetails()
                        .getSupportRequirement().stream().map(e -> e.getDisplayedValue())
                        .collect(Collectors.toList());

                    resSupportReq = String.join(", ", respondent1SupportReq);
                }
                List<String> respondent2SupportReq = Collections.emptyList();
                if (respondenceElemetnOptiona2.isPresent()) {
                    respondent2SupportReq = respondenceElemetnOptiona2.get().getValue().getGaHearingDetails()
                        .getSupportRequirement().stream().map(e -> e.getDisplayedValue())
                        .collect(Collectors.toList());

                    res2SupportReq = String.join(", ", respondent2SupportReq);
                }
            }
            return format(JUDICIAL_SUPPORT_REQ_TEXT_3, appSupportReq, resSupportReq, res2SupportReq);
        }
        return StringUtils.EMPTY;
    }

    private String getJudgeHearingCourtLoc() {

        return "TO-DO";

        /*Hearing Preferred Location in both applicant and respondent haven't yet implemented.
        Uncomment the below code once Hearing Preferred Location is implemented.*/

        /*return isAppAndRespSameCourtLocPref == YES ? format(JUDICIAL_COURT_LOC_TEXT_2, caseData
            .getGeneralAppHearingDetails().getHearingPreferredLocation())
            : format(JUDICIAL_COURT_LOC_TEXT_1, caseData.getGeneralAppHearingDetails()
            .getHearingDuration().getDisplayedValue(), caseData.getRespondentsResponses() == null ?
            StringUtils.EMPTY : caseData.getRespondentsResponses()
                         .stream().iterator().next().getValue().getGaHearingDetails().getHearingPreferredLocation());*/
    }

    public List<String> validateJudgeOrderRequestDates(GAJudicialMakeAnOrder judicialDecisionMakeOrder) {
        List<String> errors = new ArrayList<>();

        if (judicialDecisionMakeOrder.getMakeAnOrder() != null
            && APPROVE_OR_EDIT.equals(judicialDecisionMakeOrder.getMakeAnOrder())
            && judicialDecisionMakeOrder.getJudgeApproveEditOptionDate() != null) {
            LocalDate directionsResponseByDate = judicialDecisionMakeOrder.getJudgeApproveEditOptionDate();
            if (LocalDate.now().isAfter(directionsResponseByDate)) {
                errors.add(MAKE_DECISION_APPROVE_BY_DATE_IN_PAST);
            }
        }
        return errors;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

}
