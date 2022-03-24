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
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingSupportRequirements;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.service.JudicialDecisionService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.JUDGE_MAKES_DECISION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY;

@Service
@RequiredArgsConstructor
public class JudicialDecisionHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(JUDGE_MAKES_DECISION);
    private static final String VALIDATE_MAKE_DECISION_SCREEN = "validate-make-decision-screen";

    private static final String VALIDATE_REQUEST_MORE_INFO_SCREEN = "validate-request-more-info-screen";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yy");
    private static final DateTimeFormatter DATE_FORMATTER_SUBMIT_CALLBACK = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String VALIDATE_WRITTEN_REPRESENTATION_DATE = "ga-validate-written-representation-date";
    private static final String JUDICIAL_RECITAL_TEXT = "Upon reading the application of %s dated %s and upon the "
            + "application of %s dated %s and upon considering the information provided by the parties";
    private static final String JUDICIAL_HEARING_RECITAL_TEXT = "Upon the "
        + "application of %s dated %s and upon considering the information provided by the parties";
    private static final String JUDICIAL_HEARING_DIRECTIONS_TEXT = "A person who was not notified of the application"
        + "before this order was made may apply to have the order set aside or varied.\n"
        + "Any application under this paragraph must be made within 7 days after "
        + "notification of the order.";
    private static final String DISMISSAL_ORDER_TEXT = "This application is dismissed.\n\n"
            + "[Insert Draft Order from application]\n\n"
            + "A person who was not notified of the application before this order was made may apply to have the "
            + "order set aside or varied. Any application under this paragraph must be made within 7 days after "
            + "notification of the order.";
    private static final String DIRECTIONS_IN_RELATION_TO_HEARING_TEXT = "A person who was not notified of the "
        + "application before this order was made may apply to have this order set aside or varied. "
        + "Any application under this paragraph must be made within 7 days after notification of the order.";

    private final JudicialDecisionService judicialDecisionService;
    public static final String RESPOND_TO_DIRECTIONS_DATE_REQUIRED = "The date, by which the response to direction"
            + " should be given, is required.";
    public static final String RESPOND_TO_DIRECTIONS_DATE_IN_PAST = "The date, by which the response to direction"
            + " should be given, cannot be in past.";

    public static final String REQUESTED_MORE_INFO_BY_DATE_REQUIRED = "The date, by which the applicant must respond, "
            + "is required.";
    public static final String REQUESTED_MORE_INFO_BY_DATE_IN_PAST = "The date, by which the applicant must respond, "
            + "cannot be in past.";

    private final ObjectMapper objectMapper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_START), this::checkInputForNextPage,
                callbackKey(MID, VALIDATE_MAKE_DECISION_SCREEN), this::gaValidateMakeDecisionScreen,
                callbackKey(MID, VALIDATE_REQUEST_MORE_INFO_SCREEN), this::gaValidateRequestMoreInfoScreen,
                callbackKey(MID, VALIDATE_WRITTEN_REPRESENTATION_DATE), this::gaValidateWrittenRepresentationsDate,
                callbackKey(ABOUT_TO_SUBMIT), this::emptySubmittedCallbackResponse,
                callbackKey(SUBMITTED), this::buildConfirmation);

    }

    private CallbackResponse checkInputForNextPage(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        YesOrNo isCloaked = (caseData.getGeneralAppRespondentAgreement() != null
                && NO.equals(caseData.getGeneralAppRespondentAgreement().getHasAgreed())
                && caseData.getGeneralAppInformOtherParty() != null
                && NO.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice()))
                ? YES : NO;
        caseDataBuilder.applicationIsCloaked(isCloaked);

        GAJudicialMakeAnOrder.GAJudicialMakeAnOrderBuilder makeAnOrderBuilder;
        if (caseData.getJudicialDecisionMakeOrder() != null) {
            makeAnOrderBuilder = caseData.getJudicialDecisionMakeOrder().toBuilder();
        } else {
            makeAnOrderBuilder = GAJudicialMakeAnOrder.builder();
        }
        caseDataBuilder.judicialDecisionMakeOrder(makeAnOrderBuilder
                .judgeRecitalText(getJudgeRecitalPrepopulatedText(caseData))
                .dismissalOrderText(DISMISSAL_ORDER_TEXT).build());

        caseDataBuilder.judgeRecitalText(getJudgeRecitalPrepopulatedText(caseData))
            .directionInRelationToHearingText(DIRECTIONS_IN_RELATION_TO_HEARING_TEXT).build();

        caseDataBuilder.judicialGeneralHearingOrderRecital(getJudgeHearingRecitalPrepopulatedText(caseData))
            .judicialGeneralOrderHearingDirections(JUDICIAL_HEARING_DIRECTIONS_TEXT).build();

        YesOrNo isAppAndRespSameHearingPref = (caseData.getHearingDetailsResp() != null
            && caseData.getRespondentsResponses() != null
            && caseData.getRespondentsResponses().size() == 1
            && caseData.getHearingDetailsResp().getHearingPreferencesPreferredType().getDisplayedValue()
            .equals(caseData.getRespondentsResponses().stream().iterator().next().getValue().getGaHearingDetails()
                        .getHearingPreferencesPreferredType().getDisplayedValue()))
            ? YES : NO;

        GAJudgesHearingListGAspec.GAJudgesHearingListGAspecBuilder gaJudgesHearingListGAspecBuilder;
        if (caseData.getJudicialListForHearing() != null) {
            gaJudgesHearingListGAspecBuilder = caseData.getJudicialListForHearing().toBuilder();
        } else {
            gaJudgesHearingListGAspecBuilder = GAJudgesHearingListGAspec.builder();
        }

        caseDataBuilder.judicialListForHearing(gaJudgesHearingListGAspecBuilder
                                                   .sameHearingPrefByAppAndResp(isAppAndRespSameHearingPref)
                                                   .build());

        /*Hearing Preferred Location in both applicant and respondent haven't yet implemented.
        Uncomment the below code once Hearing Preferred Location is implemented.*/

        /*YesOrNo isAppAndRespSameCourtLocPref = (caseData.getHearingDetailsResp() != null
            && caseData.getRespondentsResponses() != null
            && caseData.getRespondentsResponses().size() == 1
            && caseData.getHearingDetailsResp() != null
            && caseData.getHearingDetailsResp().getHearingPreferredLocation().getValue()
            .equals(caseData.getRespondentsResponses().stream().findFirst().get().getValue().getGaHearingDetails()
                        .getHearingPreferredLocation().getValue()))
            ? YES : NO;*/

        YesOrNo isAppAndRespSameTimeEst = (caseData.getHearingDetailsResp() != null
            && caseData.getRespondentsResponses() != null
            && caseData.getRespondentsResponses().size() == 1
            && caseData.getHearingDetailsResp().getHearingDuration().getDisplayedValue()
            .equals(caseData.getRespondentsResponses().stream().iterator().next().getValue().getGaHearingDetails()
                        .getHearingDuration().getDisplayedValue()))
            ? YES : NO;

        YesOrNo isAppAndRespSameSupportReq = (caseData.getHearingDetailsResp() != null
            && caseData.getRespondentsResponses() != null
            && caseData.getRespondentsResponses().size() == 1
            && caseData.getHearingDetailsResp().getSupportRequirement() != null
            && checkIfAppAndRespHaveSameSupportReq(caseData))
            ? YES : NO;

        caseDataBuilder.judicialListForHearing(gaJudgesHearingListGAspecBuilder
                                                   .sameCourtLocationPrefByAppAndResp(YES)
                                                   .sameHearingTimeEstByAppAndResp(isAppAndRespSameTimeEst)
                                                   .sameHearingSupportReqByAppAndResp(isAppAndRespSameSupportReq)
                                                   .build());

        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
    }

    private Boolean checkIfAppAndRespHaveSameSupportReq(CaseData caseData) {

        if (caseData.getRespondentsResponses().stream().iterator().next().getValue()
            .getGaHearingDetails().getSupportRequirement() != null) {

            ArrayList<GAHearingSupportRequirements> applicantSupportReq
                = caseData.getHearingDetailsResp().getSupportRequirement().stream().sorted()
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
        return format(JUDICIAL_RECITAL_TEXT,
                (caseData.getParentClaimantIsApplicant() == null
                        || YES.equals(caseData.getParentClaimantIsApplicant()))
                        ? "Claimant" : "Defendant",
                DATE_FORMATTER.format(caseData.getCreatedDate()),
                caseData.getApplicantPartyName(),
                DATE_FORMATTER.format(LocalDate.now()));
    }

    private String getJudgeHearingRecitalPrepopulatedText(CaseData caseData) {
        return format(
            JUDICIAL_HEARING_RECITAL_TEXT,
            (caseData.getApplicantPartyName() == null ? "party" : caseData.getApplicantPartyName()),
            DATE_FORMATTER.format(caseData.getCreatedDate()));
    }

    private CallbackResponse gaValidateMakeDecisionScreen(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        GAJudicialMakeAnOrder judicialDecisionMakeOrder = caseData.getJudicialDecisionMakeOrder();
        List<String> errors = judicialDecisionMakeOrder != null
                ? validateUrgencyDates(judicialDecisionMakeOrder)
                : Collections.emptyList();

        return AboutToStartOrSubmitCallbackResponse.builder()
                .errors(errors)
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

    private CallbackResponse gaValidateRequestMoreInfoScreen(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        GAJudicialRequestMoreInfo judicialRequestMoreInfo = caseData.getJudicialDecisionRequestMoreInfo();
        List<String> errors = judicialRequestMoreInfo != null
                ? validateDatesForRequestMoreInfoScreen(judicialRequestMoreInfo)
                : Collections.emptyList();

        return AboutToStartOrSubmitCallbackResponse.builder()
                .errors(errors)
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
                    confirmationHeader = "# You have requested a response";
                    //TODO: The LocalDate.now().plusDays(7) is a temporary evaluation. This date will be populated
                    //later based on the deadline calculator
                    body = "<br/><p>The parties will be notified. They will need to provide a response by "
                            + DATE_FORMATTER_SUBMIT_CALLBACK.format(LocalDate.now().plusDays(7))
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
        List<String> errors = judicialWrittenRepresentationsDate != null
            ? judicialDecisionService.validateWrittenRepresentationsDates(judicialWrittenRepresentationsDate)
            : Collections.emptyList();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

}
