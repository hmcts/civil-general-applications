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
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentResponse;
import uk.gov.hmcts.reform.civil.service.AssignCaseToResopondentSolHelper;
import uk.gov.hmcts.reform.civil.service.GeneralAppLocationRefDataService;
import uk.gov.hmcts.reform.civil.service.JudicialDecisionHelper;
import uk.gov.hmcts.reform.civil.service.JudicialDecisionWrittenRepService;
import uk.gov.hmcts.reform.civil.service.docmosis.directionorder.DirectionOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.dismissalorder.DismissalOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.generalorder.GeneralOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.hearingorder.HearingOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.requestmoreinformation.RequestForInformationGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationconcurrentorder.WrittenRepresentationConcurrentOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationsequentialorder.WrittenRepresentationSequentailOrderGenerator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static org.apache.logging.log4j.util.Strings.concat;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAKE_DECISION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION;
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

    private static final List<CaseEvent> EVENTS = Collections.singletonList(MAKE_DECISION);

    private final GeneralAppLocationRefDataService locationRefDataService;
    private final JudicialDecisionHelper helper;
    private final AssignCaseToResopondentSolHelper assignCaseToResopondentSolHelper;
    private static final String VALIDATE_MAKE_DECISION_SCREEN = "validate-make-decision-screen";
    private static final String VALIDATE_MAKE_AN_ORDER = "validate-make-an-order";
    private static final int ONE_V_ONE = 0;
    private static final int ONE_V_TWO = 1;
    private static final String EMPTY_STRING = "";

    private static final String VALIDATE_REQUEST_MORE_INFO_SCREEN = "validate-request-more-info-screen";
    private static final String VALIDATE_HEARING_ORDER_SCREEN = "validate-hearing-order-screen";
    private static final String POPULATE_HEARING_ORDER_DOC = "populate-hearing-order-doc";
    private static final String POPULATE_WRITTEN_REP_PREVIEW_DOC = "populate-written-rep-preview-doc";

    private static final String JUDICIAL_TIME_EST_TEXT_1 = "Applicant estimates "
        + "%s. Respondent estimates %s.";
    private static final String JUDICIAL_TIME_EST_TEXT_2 = "Both applicant and respondent estimate it would take %s.";
    private static final String JUDICIAL_TIME_EST_TEXT_3 = "Applicant estimates "
        + "%s. Respondent 1 estimates %s. Respondent 2 estimates %s.";
    private static final String APPLICANT_REQUIRES = "Applicant requires ";
    private static final String JUDICIAL_APPLICANT_VULNERABILITY_TEXT = APPLICANT_REQUIRES + "support with regards to "
        + "vulnerability\n";
    private static final String JUDICIAL_RESPONDENT_VULNERABILITY_TEXT = "\n\nRespondent requires support with "
        + "regards to vulnerability\n";
    private static final String JUDICIAL_RESPONDENT1_VULNERABILITY_TEXT = "\n\nRespondent 1 requires support with "
        + "regards to vulnerability\n";
    private static final String JUDICIAL_RESPONDENT2_VULNERABILITY_TEXT = "\n\nRespondent 2 requires support with "
        + "regards to vulnerability\n";
    private static final String JUDICIAL_PREF_COURT_LOC_APPLICANT_TEXT = "Applicant prefers Location %s.";
    private static final String JUDICIAL_PREF_COURT_LOC_RESP1_TEXT = "Respondent 1 prefers Location %s.";
    private static final String JUDICIAL_PREF_COURT_LOC_RESP2_TEXT = "Respondent 2 prefers Location %s.";
    private static final String JUDICIAL_PREF_TYPE_TEXT_1 = "Applicant prefers "
        + "%s. Respondent prefers %s.";
    private static final String JUDICIAL_PREF_TYPE_TEXT_2 = "Both applicant and respondent prefer %s.";
    private static final String JUDICIAL_PREF_TYPE_TEXT_3 = "Applicant prefers "
        + "%s. Respondent 1 prefers %s. Respondent 2 prefers %s.";
    private static final String JUDICIAL_SUPPORT_REQ_TEXT_1 = APPLICANT_REQUIRES
        + "%s. Respondent requires %s.";
    private static final String JUDICIAL_SUPPORT_REQ_TEXT_2 = " Both applicant and respondent require %s.";
    private static final String JUDICIAL_SUPPORT_REQ_TEXT_3 = APPLICANT_REQUIRES
        + "%s. Respondent 1 requires %s. Respondent 2 requires %s.";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private static final DateTimeFormatter DATE_FORMATTER_SUBMIT_CALLBACK = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String VALIDATE_WRITTEN_REPRESENTATION_DATE = "ga-validate-written-representation-date";
    private static final String JUDICIAL_RECITAL_TEXT = "Judge: %s \n\n"
        + "The Judge considered the%sapplication of %s dated %s \n\nAnd the Judge considered "
        + "the information provided by the %s";
    private static final String JUDICIAL_HEARING_TYPE = "Hearing type is %s";
    private static final String JUDICIAL_TIME_ESTIMATE = "Estimated length of hearing is %s";
    private static final String JUDICIAL_SEQUENTIAL_DATE =
        "The respondent may upload any written representations by 4pm on %s";
    private static final String JUDICIAL_SEQUENTIAL_APPLICANT_DATE =
        "The applicant may upload any written representations by 4pm on %s";
    private static final String JUDICIAL_CONCURRENT_DATE =
        "The applicant and respondent may respond with written representations by 4pm on %s";
    private static final String JUDICIAL_HEARING_REQ = "Hearing requirements %s";
    private static final String DISMISSAL_ORDER_TEXT = "This application is dismissed.\n\n"
        + "[Insert Draft Order from application]\n\n";

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

    public static final String PREFERRED_LOCATION_REQUIRED = "Select your preferred hearing location.";

    public static final String PREFERRED_TYPE_IN_PERSON = "IN_PERSON";

    public static final String JUDICIAL_DECISION_LIST_FOR_HEARING = "LIST_FOR_A_HEARING";

    private final ObjectMapper objectMapper;

    private final GeneralOrderGenerator generalOrderGenerator;
    private final RequestForInformationGenerator requestForInformationGenerator;
    private final DirectionOrderGenerator directionOrderGenerator;
    private final DismissalOrderGenerator dismissalOrderGenerator;
    private final HearingOrderGenerator hearingOrderGenerator;
    private final WrittenRepresentationSequentailOrderGenerator writtenRepresentationSequentailOrderGenerator;
    private final WrittenRepresentationConcurrentOrderGenerator writtenRepresentationConcurrentOrderGenerator;

    private final IdamClient idamClient;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START), this::checkInputForNextPage,
            callbackKey(MID, VALIDATE_MAKE_AN_ORDER), this::gaValidateMakeAnOrder,
            callbackKey(MID, VALIDATE_MAKE_DECISION_SCREEN), this::gaValidateMakeDecisionScreen,
            callbackKey(MID, VALIDATE_REQUEST_MORE_INFO_SCREEN), this::gaValidateRequestMoreInfoScreen,
            callbackKey(MID, VALIDATE_WRITTEN_REPRESENTATION_DATE), this::gaValidateWrittenRepresentationsDate,
            callbackKey(MID, VALIDATE_HEARING_ORDER_SCREEN), this::gaValidateHearingOrder,
            callbackKey(MID, POPULATE_HEARING_ORDER_DOC), this::gaPopulateHearingOrderDoc,
            callbackKey(MID, POPULATE_WRITTEN_REP_PREVIEW_DOC), this::gaPopulateWrittenRepPreviewDoc,
            callbackKey(ABOUT_TO_SUBMIT), this::setJudgeBusinessProcess,
            callbackKey(SUBMITTED), this::buildConfirmation
        );

    }

    private CallbackResponse checkInputForNextPage(CallbackParams callbackParams) {
        UserDetails userDetails = idamClient.getUserDetails(callbackParams.getParams().get(BEARER_TOKEN).toString());
        String judgeNameTitle = userDetails.getFullName();

        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        if (caseData.getApplicationIsCloaked() == null) {
            caseDataBuilder.applicationIsCloaked(helper.isApplicationCreatedWithoutNoticeByApplicant(caseData));
        }

        caseDataBuilder.judicialDecisionMakeOrder(makeAnOrderBuilder(caseData, callbackParams).build());
        caseDataBuilder.judgeRecitalText(getJudgeRecitalPrepopulatedText(caseData, judgeNameTitle)).build();

        caseDataBuilder
            .judicialDecisionRequestMoreInfo(buildRequestMoreInfo(caseData, judgeNameTitle).build());

        caseDataBuilder.judicialGeneralHearingOrderRecital(getJudgeHearingRecitalPrepopulatedText(caseData,
                                                                                                  judgeNameTitle))
            .build();

        YesOrNo isAppAndRespSameHearingPref = (caseData.getGeneralAppHearingDetails() != null
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

        YesOrNo isAppAndRespSameSupportReq = (caseData.getGeneralAppHearingDetails() != null
            && caseData.getRespondentsResponses() != null
            && caseData.getRespondentsResponses().size() == 1
            && caseData.getRespondentsResponses().get(0).getValue().getGaHearingDetails()
            .getSupportRequirement() != null
            && caseData.getGeneralAppHearingDetails().getSupportRequirement() != null
            && checkIfAppAndRespHaveSameSupportReq(caseData))
            ? YES : NO;

        String authToken = callbackParams.getParams().get(BEARER_TOKEN).toString();
        DynamicList dynamicLocationList = getLocationsFromList(locationRefDataService.getCourtLocations(authToken));

        boolean isAppAndRespSameCourtLocPref = helper.isApplicantAndRespondentLocationPrefSame(caseData);
        if (isAppAndRespSameCourtLocPref) {
            String applicationLocationLabel = caseData.getGeneralAppHearingDetails().getHearingPreferredLocation()
                .getValue().getLabel();
            Optional<DynamicListElement> first = dynamicLocationList.getListItems().stream()
                .filter(l -> l.getLabel().equals(applicationLocationLabel)).findFirst();
            first.ifPresent(dynamicLocationList::setValue);
        }

        YesOrNo isAppAndRespSameTimeEst = (caseData.getGeneralAppHearingDetails() != null
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
                                                       generateRespondentCourtLocationText(caseData))
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

    private DynamicList getLocationsFromList(final List<LocationRefData> locations) {
        return fromList(locations.stream().map(location -> new StringBuilder().append(location.getSiteName())
                .append(" - ").append(location.getCourtAddress())
                .append(" - ").append(location.getPostcode()).toString())
                            .collect(Collectors.toList()));
    }

    public GAJudicialRequestMoreInfo.GAJudicialRequestMoreInfoBuilder buildRequestMoreInfo(CaseData caseData,
                                                                                           String judgeNameTitle) {

        GAJudicialRequestMoreInfo.GAJudicialRequestMoreInfoBuilder gaJudicialRequestMoreInfoBuilder
            = GAJudicialRequestMoreInfo.builder();

        if (caseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(NO)) {
            if (isAdditionalPaymentMade(caseData).equals(YES)) {
                gaJudicialRequestMoreInfoBuilder.isWithNotice(YES).build();
            } else {
                gaJudicialRequestMoreInfoBuilder
                    .isWithNotice(caseData.getGeneralAppInformOtherParty().getIsWithNotice()).build();
            }

        } else if (caseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(YES)) {
            gaJudicialRequestMoreInfoBuilder.isWithNotice(YES).build();

        }

        /*
         * Set isWithNotice to Yes if Judge uncloaks the application
         * */
        if (caseData.getApplicationIsUncloackedOnce() != null
            && caseData.getApplicationIsUncloackedOnce().equals(YES)) {
            gaJudicialRequestMoreInfoBuilder.isWithNotice(YES).build();
        }

        gaJudicialRequestMoreInfoBuilder
            .judgeRecitalText(format(JUDICIAL_RECITAL_TEXT,
                                     judgeNameTitle,
                                     helper.isApplicationCreatedWithoutNoticeByApplicant(caseData) == YES
                                         ? " without notice " : " ",
                                     (caseData.getParentClaimantIsApplicant() == null
                                         || YES.equals(caseData.getParentClaimantIsApplicant()))
                                         ? "Claimant" : "Defendant",
                                     DATE_FORMATTER.format(caseData.getCreatedDate()),
                                     (helper.isApplicationCreatedWithoutNoticeByApplicant(caseData)
                                         == NO ? "parties" : (caseData.getParentClaimantIsApplicant() == null
                                         || YES.equals(caseData.getParentClaimantIsApplicant()))
                                         ? "Claimant" : "Defendant"))).build();

        return gaJudicialRequestMoreInfoBuilder;
    }

    public GAJudicialMakeAnOrder.GAJudicialMakeAnOrderBuilder makeAnOrderBuilder(CaseData caseData,
                                                                                 CallbackParams callbackParams) {

        UserDetails userDetails = idamClient.getUserDetails(callbackParams.getParams().get(BEARER_TOKEN).toString());
        String judgeNameTitle = userDetails.getFullName();

        GAJudicialMakeAnOrder.GAJudicialMakeAnOrderBuilder makeAnOrderBuilder;
        if (caseData.getJudicialDecisionMakeOrder() != null && callbackParams.getType() != ABOUT_TO_START) {
            makeAnOrderBuilder = caseData.getJudicialDecisionMakeOrder().toBuilder();

            makeAnOrderBuilder.orderText(caseData.getJudicialDecisionMakeOrder().getOrderText())
                .judgeRecitalText(caseData.getJudicialDecisionMakeOrder().getJudgeRecitalText())
                .dismissalOrderText(caseData.getJudicialDecisionMakeOrder().getDismissalOrderText() == null
                                        ? DISMISSAL_ORDER_TEXT
                                        : caseData.getJudicialDecisionMakeOrder().getDismissalOrderText())
                .directionsText(caseData.getJudicialDecisionMakeOrder().getDirectionsText());
        } else {
            makeAnOrderBuilder = GAJudicialMakeAnOrder.builder();
            makeAnOrderBuilder.orderText(caseData.getGeneralAppDetailsOfOrder())
                .judgeRecitalText(getJudgeRecitalPrepopulatedText(caseData, judgeNameTitle))
                .dismissalOrderText(DISMISSAL_ORDER_TEXT)
                .isOrderProcessedByStayScheduler(NO);
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

    /*Return True if General Application types contains Stay the Claim
    Else, Return False*/
    private boolean checkApplicationTypeForDate(CaseData caseData) {

        if (caseData.getGeneralAppType() == null) {
            return false;
        }
        List<GeneralApplicationTypes> validGATypes = Arrays.asList(STAY_THE_CLAIM);
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

    private String getJudgeRecitalPrepopulatedText(CaseData caseData, String judgeNameTitle) {
        return format(
            JUDICIAL_RECITAL_TEXT,
            judgeNameTitle,
            helper.isApplicationCreatedWithoutNoticeByApplicant(caseData) == YES ? " without notice " : " ",
            (caseData.getParentClaimantIsApplicant() == null
                || YES.equals(caseData.getParentClaimantIsApplicant()))
                ? "Claimant" : "Defendant",
            DATE_FORMATTER.format(caseData.getCreatedDate()),
            (helper.isApplicationCreatedWithoutNoticeByApplicant(caseData) == NO ? "parties" : (caseData
                .getParentClaimantIsApplicant() == null || YES.equals(caseData.getParentClaimantIsApplicant()))
                ? "Claimant" : "Defendant")
        );
    }

    private String getJudgeHearingRecitalPrepopulatedText(CaseData caseData, String judgeNameTitle) {
        return format(
            JUDICIAL_RECITAL_TEXT,
            judgeNameTitle,
            helper.isApplicationCreatedWithoutNoticeByApplicant(caseData) == YES ? " without notice " : " ",
            (caseData.getParentClaimantIsApplicant() == null
                || YES.equals(caseData.getParentClaimantIsApplicant()))
                ? "Claimant" : "Defendant",
            DATE_FORMATTER.format(caseData.getCreatedDate()),
            (helper.isApplicationCreatedWithoutNoticeByApplicant(caseData) == NO ? "parties" : (caseData
                .getParentClaimantIsApplicant() == null || YES.equals(caseData.getParentClaimantIsApplicant()))
                ? "Claimant" : "Defendant")
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

            CaseDocument judgeDecision = null;
            if (judicialDecisionMakeOrder.getOrderText() != null
                && judicialDecisionMakeOrder.getMakeAnOrder().equals(APPROVE_OR_EDIT)) {
                judgeDecision = generalOrderGenerator.generate(
                    caseDataBuilder.build(),
                    callbackParams.getParams().get(BEARER_TOKEN).toString()
                );
                caseDataBuilder.judicialMakeOrderDocPreview(judgeDecision.getDocumentLink());
            } else if (judicialDecisionMakeOrder.getDirectionsText() != null
                && judicialDecisionMakeOrder.getMakeAnOrder().equals(GIVE_DIRECTIONS_WITHOUT_HEARING)) {
                judgeDecision = directionOrderGenerator.generate(
                    caseDataBuilder.build(),
                    callbackParams.getParams().get(BEARER_TOKEN).toString()
                );
                caseDataBuilder.judicialMakeOrderDocPreview(judgeDecision.getDocumentLink());
            } else if (judicialDecisionMakeOrder.getMakeAnOrder().equals(DISMISS_THE_APPLICATION)) {
                judgeDecision = dismissalOrderGenerator.generate(
                    caseDataBuilder.build(),
                    callbackParams.getParams().get(BEARER_TOKEN).toString()
                );
                caseDataBuilder.judicialMakeOrderDocPreview(judgeDecision.getDocumentLink());
            }
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

        UserDetails userDetails = idamClient.getUserDetails(callbackParams.getParams().get(BEARER_TOKEN).toString());
        String judgeNameTitle = userDetails.getFullName();

        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        caseDataBuilder.judicialDecisionMakeOrder(makeAnOrderBuilder(caseData, callbackParams).build());

        caseDataBuilder
            .judicialDecisionRequestMoreInfo(buildRequestMoreInfo(caseData, judgeNameTitle).build());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    private CallbackResponse gaValidateRequestMoreInfoScreen(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        GAJudicialRequestMoreInfo judicialRequestMoreInfo = caseData.getJudicialDecisionRequestMoreInfo();

        GAJudicialRequestMoreInfo.GAJudicialRequestMoreInfoBuilder gaJudicialRequestMoreInfoBuilder
            = judicialRequestMoreInfo.toBuilder();

        if (judicialRequestMoreInfo.getIsWithNotice() == null && caseData.getApplicationIsUncloackedOnce() == null) {

            if (caseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(NO)) {
                gaJudicialRequestMoreInfoBuilder
                    .isWithNotice(caseData.getGeneralAppInformOtherParty().getIsWithNotice()).build();

            } else if (caseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(YES)) {
                gaJudicialRequestMoreInfoBuilder.isWithNotice(YES).build();

            }
        }

        if ((judicialRequestMoreInfo.getIsWithNotice() != null
            && judicialRequestMoreInfo.getIsWithNotice().equals(YES))
            ||
            (caseData.getJudicialDecisionRequestMoreInfo() != null
            && caseData.getJudicialDecisionRequestMoreInfo().getRequestMoreInfoOption()
            .equals(REQUEST_MORE_INFORMATION))) {

            caseDataBuilder.showRequestInfoPreviewDoc(YES);

        } else {

            caseDataBuilder.showRequestInfoPreviewDoc(NO);

        }

        List<String> errors = validateDatesForRequestMoreInfoScreen(caseData, judicialRequestMoreInfo);

        caseDataBuilder
            .judicialDecisionRequestMoreInfo(gaJudicialRequestMoreInfoBuilder.build());

        CaseDocument judgeDecision = null;

        /*
         * Generate Request More Information preview Doc if it's without notice application and Request More Info
         * OR General Application is With notice
         * */

        if ((judicialRequestMoreInfo.getIsWithNotice() != null
            && judicialRequestMoreInfo.getIsWithNotice().equals(YES))
            ||
            (judicialRequestMoreInfo.getJudgeRequestMoreInfoByDate() != null
                && judicialRequestMoreInfo.getJudgeRequestMoreInfoText() != null
                && caseData.getJudicialDecisionRequestMoreInfo().getRequestMoreInfoOption()
                .equals(REQUEST_MORE_INFORMATION))) {

            judgeDecision = requestForInformationGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString());

            caseDataBuilder.judicialRequestMoreInfoDocPreview(judgeDecision.getDocumentLink());
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    public List<String> validateDatesForRequestMoreInfoScreen(CaseData caseData,
                                                              GAJudicialRequestMoreInfo judicialRequestMoreInfo) {
        List<String> errors = new ArrayList<>();
        if (REQUEST_MORE_INFO.equals(caseData.getJudicialDecision().getDecision())
            && REQUEST_MORE_INFORMATION.equals(judicialRequestMoreInfo.getRequestMoreInfoOption())
            || judicialRequestMoreInfo.getRequestMoreInfoOption() == null) {
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
        CaseData caseData = callbackParams.getCaseData();
        String caseId = caseData.getCcdCaseReference().toString();

        if (caseData.getJudicialDecision().getDecision().name().equals(JUDICIAL_DECISION_LIST_FOR_HEARING)) {
            if (caseData.getJudicialListForHearing().getHearingPreferredLocation() != null) {
                GAJudgesHearingListGAspec gaJudgesHearingListGAspec = caseData.getJudicialListForHearing().toBuilder()
                    .hearingPreferredLocation(
                        populateJudicialHearingLocation(caseData))
                    .build();
                CaseData updatedCaseData = caseData.toBuilder().judicialListForHearing(gaJudgesHearingListGAspec)
                    .build();
                caseData = updatedCaseData;
                dataBuilder = updatedCaseData.toBuilder();
            }
        }

        dataBuilder.businessProcess(BusinessProcess.ready(MAKE_DECISION)).build();

        var isApplicationUncloaked = isApplicationContinuesCloakedAfterJudicialDecision(caseData);
        if (Objects.isNull(isApplicationUncloaked)
            && helper.isApplicationCreatedWithoutNoticeByApplicant(caseData).equals(NO)) {
            dataBuilder.applicationIsCloaked(NO);
        } else {
            dataBuilder.applicationIsCloaked(isApplicationUncloaked);
        }

        /*
        * Assign case respondent solicitors if judge uncloak the application
        * */

        if (isApplicationUncloaked != null
            && isApplicationUncloaked.equals(NO)) {
            dataBuilder.applicationIsUncloackedOnce(YES);
            assignCaseToResopondentSolHelper.assignCaseToRespondentSolicitor(caseData, caseId);

        }

        if (Objects.nonNull(caseData.getJudicialMakeOrderDocPreview())) {
            dataBuilder.judicialMakeOrderDocPreview(null);
        }

        if (Objects.nonNull(caseData.getJudicialListHearingDocPreview())) {
            dataBuilder.judicialListHearingDocPreview(null);
        }

        if (Objects.nonNull(caseData.getJudicialWrittenRepDocPreview())) {
            dataBuilder.judicialWrittenRepDocPreview(null);
        }

        if (Objects.nonNull(caseData.getJudicialRequestMoreInfoDocPreview())) {
            dataBuilder.judicialRequestMoreInfoDocPreview(null);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(dataBuilder.build().toMap(objectMapper))
            .build();
    }

    private DynamicList populateJudicialHearingLocation(CaseData caseData) {
        DynamicList dynamicLocationList;
        String applicationLocationLabel = caseData.getJudicialListForHearing()
                .getHearingPreferredLocation().getValue().getLabel();
        dynamicLocationList = fromList(List.of(applicationLocationLabel));
        Optional<DynamicListElement> first = dynamicLocationList.getListItems().stream()
                .filter(l -> l.getLabel().equals(applicationLocationLabel)).findFirst();
        first.ifPresent(dynamicLocationList::setValue);
        return dynamicLocationList;
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
                if (REQUEST_MORE_INFORMATION.equals(requestMoreInfo.getRequestMoreInfoOption())
                    || requestMoreInfo.getRequestMoreInfoOption() == null) {
                    if (requestMoreInfo.getJudgeRequestMoreInfoByDate() != null) {
                        confirmationHeader = "# You have requested more information";
                        body = "<br/><p>The applicant will be notified. They will need to provide a response by "
                            + DATE_FORMATTER_SUBMIT_CALLBACK.format(requestMoreInfo.getJudgeRequestMoreInfoByDate())
                            + "</p>";
                    } else {
                        throw new IllegalArgumentException("Missing data during submission of judicial decision");
                    }
                } else if (SEND_APP_TO_OTHER_PARTY.equals(requestMoreInfo.getRequestMoreInfoOption())) {
                    confirmationHeader = "# You have requested a response";
                    body = "<br/><p>The parties will be notified.</p>";
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
        errors = caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations() != null
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

    private CallbackResponse gaPopulateWrittenRepPreviewDoc(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        GAJudicialWrittenRepresentations judicialWrittenRepresentationsDate =
            caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        CaseDocument judgeDecision = null;

        if (caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations() != null
            && caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
            .getWrittenSequentailRepresentationsBy() != null
            && judicialWrittenRepresentationsDate.getSequentialApplicantMustRespondWithin() != null) {

            judgeDecision = writtenRepresentationSequentailOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            caseDataBuilder.judicialWrittenRepDocPreview(judgeDecision.getDocumentLink());

        } else if (caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations() != null
            && caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
            .getWrittenConcurrentRepresentationsBy() != null) {

            judgeDecision = writtenRepresentationConcurrentOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            caseDataBuilder.judicialWrittenRepDocPreview(judgeDecision.getDocumentLink());

        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    private CallbackResponse gaValidateHearingOrder(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        List<String> errors = new ArrayList<>();
        String preferredType = caseData.getJudicialListForHearing().getHearingPreferencesPreferredType().name();
        if (preferredType.equals(PREFERRED_TYPE_IN_PERSON)
            && (caseData.getJudicialListForHearing().getHearingPreferredLocation() == null)) {
            errors.add(PREFERRED_LOCATION_REQUIRED);
        }
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.judicialHearingGeneralOrderHearingText(getJudgeHearingPrePopulatedText(caseData))
            .judicialHearingGOHearingReqText(populateJudgeGOSupportRequirement(caseData))
            .judicialGeneralOrderHearingEstimationTimeText(getJudgeHearingTimeEstPrePopulatedText(caseData)).build();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    private CallbackResponse gaPopulateHearingOrderDoc(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        CaseDocument judgeDecision = null;
        if (caseData.getJudicialListForHearing() != null) {
            judgeDecision = hearingOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );
            caseDataBuilder.judicialListHearingDocPreview(judgeDecision.getDocumentLink());
        }

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
        String respondent1HearingType = null;
        String respondent2HearingType = null;

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
            Optional<Element<GARespondentResponse>> responseElementOptional1 = response1(caseData);
            Optional<Element<GARespondentResponse>> responseElementOptional2 = response2(caseData);
            if (responseElementOptional1.isPresent()) {
                respondent1HearingType = responseElementOptional1.get().getValue()
                    .getGaHearingDetails().getHearingPreferencesPreferredType().getDisplayedValue();
            }
            if (responseElementOptional2.isPresent()) {
                respondent2HearingType = responseElementOptional2.get().getValue()
                    .getGaHearingDetails().getHearingPreferencesPreferredType().getDisplayedValue();
            }
            return format(JUDICIAL_PREF_TYPE_TEXT_3, caseData.getGeneralAppHearingDetails()
                              .getHearingPreferencesPreferredType().getDisplayedValue(),
                          respondent1HearingType, respondent2HearingType
            );
        }

        if ((caseData.getGeneralAppUrgencyRequirement() != null
            && caseData.getGeneralAppUrgencyRequirement().getGeneralAppUrgency() == YesOrNo.YES)
            || caseData.getGeneralAppHearingDetails().getHearingPreferencesPreferredType() != null) {
            return "Applicant prefers ".concat(caseData
                                                   .getGeneralAppHearingDetails().getHearingPreferencesPreferredType()
                                                   .getDisplayedValue());
        }

        return StringUtils.EMPTY;
    }

    private String getJudgeHearingTimeEst(CaseData caseData, YesOrNo isAppAndRespSameTimeEst) {
        String respondet1HearingDuration = null;
        String respondent2HearingDuration = null;

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
            Optional<Element<GARespondentResponse>> responseElementOptional1 = response1(caseData);
            Optional<Element<GARespondentResponse>> responseElementOptional2 = response2(caseData);
            if (responseElementOptional1.isPresent()) {
                respondet1HearingDuration = responseElementOptional1.get().getValue()
                    .getGaHearingDetails().getHearingDuration().getDisplayedValue();
            }
            if (responseElementOptional2.isPresent()) {
                respondent2HearingDuration = responseElementOptional2.get().getValue()
                    .getGaHearingDetails().getHearingDuration().getDisplayedValue();
            }

            return format(JUDICIAL_TIME_EST_TEXT_3, caseData.getGeneralAppHearingDetails()
                .getHearingDuration().getDisplayedValue(), respondet1HearingDuration, respondent2HearingDuration);
        }

        if ((caseData.getGeneralAppUrgencyRequirement() != null
            && caseData.getGeneralAppUrgencyRequirement().getGeneralAppUrgency() == YesOrNo.YES)
            || caseData.getGeneralAppHearingDetails().getHearingDuration() != null) {
            return "Applicant estimates ".concat(caseData.getGeneralAppHearingDetails()
                                                     .getHearingDuration().getDisplayedValue());
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

        boolean hasRespondent1VulnerabilityResponded = responseCount == 2
            ? caseData.getRespondentsResponses().get(ONE_V_ONE).getValue()
            .getGaHearingDetails().getVulnerabilityQuestionsYesOrNo() == YES ? TRUE : FALSE
            : FALSE;

        boolean hasRespondent2VulnerabilityResponded = responseCount == 2
            ? caseData.getRespondentsResponses().get(ONE_V_TWO).getValue()
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

        if (applicantVulnerabilityResponse == YES && hasRespondent1VulnerabilityResponded == TRUE
            && hasRespondent2VulnerabilityResponded == TRUE) {
            Optional<Element<GARespondentResponse>> responseElementOptional1 = response1(caseData);
            Optional<Element<GARespondentResponse>> responseElementOptional2 = response2(caseData);
            if (responseElementOptional1.isPresent() && responseElementOptional2.isPresent()) {
                return JUDICIAL_APPLICANT_VULNERABILITY_TEXT
                    .concat(caseData.getGeneralAppHearingDetails()
                                .getVulnerabilityQuestion()
                                .concat(JUDICIAL_RESPONDENT1_VULNERABILITY_TEXT)
                                .concat(responseElementOptional1.get().getValue()
                                            .getGaHearingDetails().getVulnerabilityQuestion())
                                .concat(JUDICIAL_RESPONDENT2_VULNERABILITY_TEXT)
                                .concat(responseElementOptional2.get().getValue()
                                            .getGaHearingDetails().getVulnerabilityQuestion()));
            }
        }

        if (applicantVulnerabilityResponse == NO && hasRespondent1VulnerabilityResponded == TRUE
            && hasRespondent2VulnerabilityResponded == TRUE) {
            Optional<Element<GARespondentResponse>> responseElementOptional1 = response1(caseData);
            Optional<Element<GARespondentResponse>> responseElementOptional2 = response2(caseData);
            if (responseElementOptional1.isPresent() && responseElementOptional2.isPresent()) {
                return JUDICIAL_RESPONDENT1_VULNERABILITY_TEXT
                    .concat(responseElementOptional1.get().getValue()
                                .getGaHearingDetails().getVulnerabilityQuestion())
                    .concat(JUDICIAL_RESPONDENT2_VULNERABILITY_TEXT)
                    .concat(responseElementOptional2.get().getValue()
                                .getGaHearingDetails().getVulnerabilityQuestion());
            }
        }

        if (applicantVulnerabilityResponse == YES && hasRespondent1VulnerabilityResponded == TRUE
            && hasRespondent2VulnerabilityResponded == FALSE) {
            Optional<Element<GARespondentResponse>> responseElementOptional1 = response1(caseData);
            Optional<Element<GARespondentResponse>> responseElementOptional2 = response2(caseData);
            if (responseElementOptional1.isPresent()) {
                return JUDICIAL_APPLICANT_VULNERABILITY_TEXT
                    .concat(caseData.getGeneralAppHearingDetails()
                                .getVulnerabilityQuestion()
                                .concat(JUDICIAL_RESPONDENT1_VULNERABILITY_TEXT)
                                .concat(responseElementOptional1.get().getValue()
                                            .getGaHearingDetails().getVulnerabilityQuestion()));
            }
        }

        if (applicantVulnerabilityResponse == YES && hasRespondent1VulnerabilityResponded == FALSE
            && hasRespondent2VulnerabilityResponded == TRUE) {
            Optional<Element<GARespondentResponse>> responseElementOptional1 = response1(caseData);
            Optional<Element<GARespondentResponse>> responseElementOptional2 = response2(caseData);
            if (responseElementOptional2.isPresent()) {
                return JUDICIAL_APPLICANT_VULNERABILITY_TEXT
                    .concat(caseData.getGeneralAppHearingDetails()
                                .getVulnerabilityQuestion()
                                .concat(JUDICIAL_RESPONDENT2_VULNERABILITY_TEXT)
                                .concat(responseElementOptional2.get().getValue()
                                            .getGaHearingDetails().getVulnerabilityQuestion()));
            }
        }

        if (applicantVulnerabilityResponse == NO && hasRespondent1VulnerabilityResponded == FALSE
            && hasRespondent2VulnerabilityResponded == TRUE) {
            Optional<Element<GARespondentResponse>> responseElementOptional2 = response2(caseData);
            if (responseElementOptional2.isPresent()) {
                return JUDICIAL_RESPONDENT2_VULNERABILITY_TEXT
                    .concat(responseElementOptional2.get().getValue()
                                .getGaHearingDetails().getVulnerabilityQuestion());
            }
        }

        if (applicantVulnerabilityResponse == NO && hasRespondent1VulnerabilityResponded == TRUE
            && hasRespondent2VulnerabilityResponded == FALSE) {
            Optional<Element<GARespondentResponse>> responseElementOptional1 = response1(caseData);
            if (responseElementOptional1.isPresent()) {
                return JUDICIAL_RESPONDENT1_VULNERABILITY_TEXT
                    .concat(responseElementOptional1.get().getValue()
                                .getGaHearingDetails().getVulnerabilityQuestion());
            }
        }
        return applicantVulnerabilityResponse == YES ? JUDICIAL_APPLICANT_VULNERABILITY_TEXT
            .concat(caseData.getGeneralAppHearingDetails()
                        .getVulnerabilityQuestion())
            : hasRespondentVulnerabilityResponded == TRUE
            ? ltrim(JUDICIAL_RESPONDENT_VULNERABILITY_TEXT).concat(caseData.getRespondentsResponses().stream()
                                                                       .iterator().next().getValue()
                                                                       .getGaHearingDetails()
                                                                       .getVulnerabilityQuestion())
            : "No support required with regards to vulnerability";
    }

    private String ltrim(String str) {
        return str.replaceAll("^\\s+", EMPTY_STRING);
    }

    private String getJudgeHearingSupportReq(CaseData caseData, YesOrNo isAppAndRespSameSupportReq) {
        List<String> applicantSupportReq = Collections.emptyList();
        String resSupportReq = StringUtils.EMPTY;
        String appSupportReq = StringUtils.EMPTY;

        if (caseData.getGeneralAppHearingDetails().getSupportRequirement() != null) {
            applicantSupportReq = caseData.getGeneralAppHearingDetails().getSupportRequirement().stream()
                .map(GAHearingSupportRequirements::getDisplayedValue).collect(Collectors.toList());

            appSupportReq = String.join(", ", applicantSupportReq);
        }

        if (caseData.getRespondentsResponses() != null && caseData.getRespondentsResponses().size() == 1) {
            List<String> respondentSupportReq = Collections.emptyList();
            if (caseData.getRespondentsResponses() != null
                && caseData.getRespondentsResponses().size() == 1
                && caseData.getRespondentsResponses().get(ONE_V_ONE).getValue().getGaHearingDetails()
                .getSupportRequirement() != null) {
                respondentSupportReq
                    = caseData.getRespondentsResponses().stream().iterator().next().getValue()
                    .getGaHearingDetails().getSupportRequirement().stream()
                    .map(GAHearingSupportRequirements::getDisplayedValue)
                    .collect(Collectors.toList());

                resSupportReq = String.join(", ", respondentSupportReq);
            }

            return isAppAndRespSameSupportReq == YES ? format(JUDICIAL_SUPPORT_REQ_TEXT_2, appSupportReq)
                : format(JUDICIAL_SUPPORT_REQ_TEXT_1, applicantSupportReq.isEmpty() ? "no support" : appSupportReq,
                         respondentSupportReq.isEmpty() ? "no support" : resSupportReq
            );
        }

        if (caseData.getRespondentsResponses() != null && caseData.getRespondentsResponses().size() == 2) {
            Optional<Element<GARespondentResponse>> response1 = response1(caseData);
            Optional<Element<GARespondentResponse>> response2 = response2(caseData);

            String respondentOne = retrieveSupportRequirementsFromResponse(response1);
            String respondentTwo = retrieveSupportRequirementsFromResponse(response2);
            return format(JUDICIAL_SUPPORT_REQ_TEXT_3,
                          appSupportReq.isEmpty() ? "no support" : appSupportReq,
                          respondentOne.isEmpty() ? "no support" : respondentOne,
                          respondentTwo.isEmpty() ? "no support" : respondentTwo);
        }

        if ((caseData.getGeneralAppUrgencyRequirement() != null
            && caseData.getGeneralAppUrgencyRequirement().getGeneralAppUrgency() == YesOrNo.YES)
            || caseData.getGeneralAppHearingDetails() != null) {
            return APPLICANT_REQUIRES.concat(applicantSupportReq.isEmpty() ? "no support" : appSupportReq);

        }
        return StringUtils.EMPTY;
    }

    private String retrieveSupportRequirementsFromResponse(Optional<Element<GARespondentResponse>> response) {
        if (response.isPresent()
            && response.get().getValue().getGaHearingDetails().getSupportRequirement() != null) {
            return response.get().getValue().getGaHearingDetails()
                .getSupportRequirement().stream().map(GAHearingSupportRequirements::getDisplayedValue)
                .collect(Collectors.joining(", "));
        }
        return StringUtils.EMPTY;
    }

    private String generateRespondentCourtLocationText(CaseData caseData) {

        if (caseData.getGeneralAppHearingDetails().getHearingPreferredLocation() == null
            && caseData.getRespondentsResponses() != null) {
            return generateRespondentCourtDirectionText(caseData);
        }

        if (caseData.getGeneralAppHearingDetails().getHearingPreferredLocation() != null
            && caseData.getRespondentsResponses() == null) {
            return format(JUDICIAL_PREF_COURT_LOC_APPLICANT_TEXT, caseData.getGeneralAppHearingDetails()
                .getHearingPreferredLocation().getValue().getLabel());
        }
        if (caseData.getGeneralAppHearingDetails().getHearingPreferredLocation() != null
            && caseData.getRespondentsResponses() != null) {

            return concat(concat(format(JUDICIAL_PREF_COURT_LOC_APPLICANT_TEXT, caseData.getGeneralAppHearingDetails()
                              .getHearingPreferredLocation().getValue().getLabel()), " "),
                          generateRespondentCourtDirectionText(caseData)).trim();
        }

        return StringUtils.EMPTY;
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

    private Optional<Element<GARespondentResponse>> response1(CaseData caseData) {
        List<Element<GARespondentResponse>> respondentResponse = caseData.getRespondentsResponses();
        String respondent1Id = caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getId();
        Optional<Element<GARespondentResponse>> responseElementOptional1;
        responseElementOptional1 = respondentResponse.stream()
            .filter(res -> res.getValue() != null && res.getValue().getGaRespondentDetails()
                .equals(respondent1Id)).findAny();
        return responseElementOptional1;
    }

    private Optional<Element<GARespondentResponse>> response2(CaseData caseData) {
        List<Element<GARespondentResponse>> respondentResponse = caseData.getRespondentsResponses();
        String respondent2Id = caseData.getGeneralAppRespondentSolicitors().get(1).getValue().getId();
        Optional<Element<GARespondentResponse>> responseElementOptional2;
        responseElementOptional2 = respondentResponse.stream()
            .filter(res -> res.getValue() != null && res.getValue().getGaRespondentDetails()
                .equals(respondent2Id)).findAny();
        return responseElementOptional2;
    }

    private String generateRespondentCourtDirectionText(CaseData caseData) {
        Optional<Element<GARespondentResponse>> responseElementOptional1 = Optional.empty();
        Optional<Element<GARespondentResponse>> responseElementOptional2 = Optional.empty();

        if (caseData.getGeneralAppRespondentSolicitors() != null
            && caseData.getGeneralAppRespondentSolicitors().size() > 0) {
            responseElementOptional1 = response1(caseData);
        }
        if (caseData.getGeneralAppRespondentSolicitors() != null
            && caseData.getGeneralAppRespondentSolicitors().size() > 1) {
            responseElementOptional2 = response2(caseData);
        }
        YesOrNo hasRespondent1PreferredLocation = hasPreferredLocation(responseElementOptional1);
        YesOrNo hasRespondent2PreferredLocation = hasPreferredLocation(responseElementOptional2);

        if (responseElementOptional1.isPresent() && responseElementOptional2.isPresent()
            && hasRespondent1PreferredLocation == YES && hasRespondent2PreferredLocation == YES) {
            return concat(concat(format(JUDICIAL_PREF_COURT_LOC_RESP1_TEXT, responseElementOptional1.get()
                              .getValue().getGaHearingDetails().getHearingPreferredLocation()
                              .getValue().getLabel()), " "),
                          format(JUDICIAL_PREF_COURT_LOC_RESP2_TEXT, responseElementOptional2.get().getValue()
                              .getGaHearingDetails().getHearingPreferredLocation().getValue().getLabel()));
        }
        if (responseElementOptional1.isPresent() && hasRespondent1PreferredLocation == YES) {
            return format(JUDICIAL_PREF_COURT_LOC_RESP1_TEXT, responseElementOptional1.get().getValue()
                .getGaHearingDetails().getHearingPreferredLocation().getValue().getLabel());

        }
        if (responseElementOptional2.isPresent() && hasRespondent2PreferredLocation == YES) {
            return format(JUDICIAL_PREF_COURT_LOC_RESP2_TEXT, responseElementOptional2.get().getValue()
                .getGaHearingDetails().getHearingPreferredLocation().getValue().getLabel());
        }
        return StringUtils.EMPTY;
    }

    private YesOrNo hasPreferredLocation(Optional<Element<GARespondentResponse>> responseElementOptional) {
        if (responseElementOptional.isPresent() && responseElementOptional.get().getValue().getGaHearingDetails()
            != null && responseElementOptional.get().getValue().getGaHearingDetails().getHearingPreferredLocation()
            != null) {
            return YES;
        }
        return NO;
    }

    private YesOrNo isApplicationContinuesCloakedAfterJudicialDecision(CaseData caseData) {
        if (caseData.getMakeAppVisibleToRespondents() != null
            || isApplicationUncloakedForRequestMoreInformation(caseData).equals(YES)) {
            return NO;
        }
        return caseData.getApplicationIsCloaked();
    }

    private YesOrNo isApplicationUncloakedForRequestMoreInformation(CaseData caseData) {
        if (caseData.getJudicialDecisionRequestMoreInfo() != null
            && caseData.getJudicialDecisionRequestMoreInfo().getRequestMoreInfoOption() != null
            && caseData.getJudicialDecisionRequestMoreInfo()
            .getRequestMoreInfoOption().equals(SEND_APP_TO_OTHER_PARTY)) {
            return YES;
        }
        return NO;
    }

    private YesOrNo isAdditionalPaymentMade(CaseData caseData) {
        return caseData.getGeneralAppInformOtherParty().getIsWithNotice().equals(NO)
            && Objects.nonNull(caseData.getGeneralAppPBADetails())
            && Objects.nonNull(caseData.getGeneralAppPBADetails().getAdditionalPaymentDetails()) ? YES : NO;

    }
}
