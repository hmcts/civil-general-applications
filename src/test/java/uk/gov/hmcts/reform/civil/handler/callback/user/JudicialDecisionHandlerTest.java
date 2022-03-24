package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingDuration;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingSupportRequirements;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingType;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GARespondentRepresentative;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.JUDGE_MAKES_DECISION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@SuppressWarnings({"checkstyle:EmptyLineSeparator", "checkstyle:Indentation"})
@SpringBootTest(classes = {
        JudicialDecisionHandler.class,
        JacksonAutoConfiguration.class,
},
        properties = {"reference.database.enabled=false"})
public class JudicialDecisionHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    JudicialDecisionHandler handler;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CAMUNDA_EVENT = "INITIATE_GENERAL_APPLICATION";
    private static final String BUSINESS_PROCESS_INSTANCE_ID = "11111";
    private static final String ACTIVITY_ID = "anyActivity";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yy");


    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(JUDGE_MAKES_DECISION);
    }

    @Test
    void testAboutToStartForHearingGeneralOrderRecital() {
        CallbackParams params = callbackParamsOf(getHearingOrderApplnAndResp(), ABOUT_TO_START);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response).isNotNull();
        GAJudgesHearingListGAspec hearingOrder = getJudicialHearingOrder(response);

        assertThat(hearingOrder.getSameHearingPrefByAppAndResp()).isEqualTo(YES);
        assertThat(hearingOrder.getSameHearingTimeEstByAppAndResp()).isEqualTo(YES);
        assertThat(hearingOrder.getSameHearingSupportReqByAppAndResp()).isEqualTo(YES);
    }

    @Test
    void testAboutToStartForNotifiedApplication() {
        String expectedRecitalText = "Upon reading the application of Claimant dated 15 January 22 and upon the "
                + "application of ApplicantPartyName dated %s and upon considering the information "
                + "provided by the parties";
        String expectedDismissalOrder = "This application is dismissed.\n\n"
                + "[Insert Draft Order from application]\n\n"
                + "A person who was not notified of the application before this order was made may apply to have the "
                + "order set aside or varied. Any application under this paragraph must be made within 7 days after "
                + "notification of the order.";
        CallbackParams params = callbackParamsOf(getNotifiedApplication(), ABOUT_TO_START);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response).isNotNull();
        assertThat(getApplicationIsCloakedStatus(response)).isEqualTo(NO);
        GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

        assertThat(makeAnOrder.getJudgeRecitalText()).isEqualTo(String.format(expectedRecitalText,
                DATE_FORMATTER.format(LocalDate.now())));
        assertThat(makeAnOrder.getDismissalOrderText()).isEqualTo(expectedDismissalOrder);
    }

    @Test
    void testAboutToStartForCloakedApplication() {
        String expectedRecitalText = "Upon reading the application of Claimant dated 15 January 22 and upon the "
                + "application of ApplicantPartyName dated %s and upon considering the information "
                + "provided by the parties";
        String expectedDismissalOrder = "This application is dismissed.\n\n"
                + "[Insert Draft Order from application]\n\n"
                + "A person who was not notified of the application before this order was made may apply to have the "
                + "order set aside or varied. Any application under this paragraph must be made within 7 days after "
                + "notification of the order.";
        CallbackParams params = callbackParamsOf(getCloakedApplication(), ABOUT_TO_START);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response).isNotNull();
        assertThat(getApplicationIsCloakedStatus(response)).isEqualTo(YES);
        GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

        assertThat(makeAnOrder.getJudgeRecitalText()).isEqualTo(String.format(expectedRecitalText,
                DATE_FORMATTER.format(LocalDate.now())));
        assertThat(makeAnOrder.getDismissalOrderText()).isEqualTo(expectedDismissalOrder);
    }

    @Test
    void testAboutToStartForDefendant_judgeRecitalText() {
        String expectedRecitalText = "Upon reading the application of Defendant dated 15 January 22 and upon the "
                + "application of ApplicantPartyName dated %s and upon considering the information "
                + "provided by the parties";
        String expectedDismissalOrder = "This application is dismissed.\n\n"
                + "[Insert Draft Order from application]\n\n"
                + "A person who was not notified of the application before this order was made may apply to have the "
                + "order set aside or varied. Any application under this paragraph must be made within 7 days after "
                + "notification of the order.";
        CallbackParams params = callbackParamsOf(getApplicationByParentCaseDefendant(), ABOUT_TO_START);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response).isNotNull();
        assertThat(getApplicationIsCloakedStatus(response)).isEqualTo(NO);
        GAJudicialMakeAnOrder makeAnOrder = getJudicialMakeAnOrder(response);

        assertThat(makeAnOrder.getJudgeRecitalText()).isEqualTo(String.format(expectedRecitalText,
                DATE_FORMATTER.format(LocalDate.now())));
        assertThat(makeAnOrder.getDismissalOrderText()).isEqualTo(expectedDismissalOrder);
    }

    private GAJudicialMakeAnOrder getJudicialMakeAnOrder(AboutToStartOrSubmitCallbackResponse response) {
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        return responseCaseData.getJudicialDecisionMakeOrder();
    }

    private GAJudgesHearingListGAspec getJudicialHearingOrder(AboutToStartOrSubmitCallbackResponse response) {
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        return responseCaseData.getJudicialListForHearing();
    }

    private YesOrNo getApplicationIsCloakedStatus(AboutToStartOrSubmitCallbackResponse response) {
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        return responseCaseData.getApplicationIsCloaked();
    }

    private CaseData getNotifiedApplication() {
        List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
        return CaseData.builder()
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
                .createdDate(LocalDateTime.of(2022, 01, 15, 0, 0, 0))
                .applicantPartyName("ApplicantPartyName")
                .generalAppRespondent1Representative(
                        GARespondentRepresentative.builder()
                                .generalAppRespondent1Representative(YES)
                                .build())
                .generalAppType(
                        GAApplicationType
                                .builder()
                                .types(types).build())
                .businessProcess(BusinessProcess
                        .builder()
                        .camundaEvent(CAMUNDA_EVENT)
                        .processInstanceId(BUSINESS_PROCESS_INSTANCE_ID)
                        .status(BusinessProcessStatus.STARTED)
                        .activityId(ACTIVITY_ID)
                        .build())
                .ccdState(CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
                .build();
    }

    private CaseData getHearingOrderApplnAndResp() {

        List<GAHearingSupportRequirements> applSupportReq = new ArrayList<>();
        applSupportReq
            .add(GAHearingSupportRequirements.HEARING_LOOPS);
        applSupportReq
            .add(GAHearingSupportRequirements.OTHER_SUPPORT);

        List<GAHearingSupportRequirements> respSupportReq = new ArrayList<>();
        respSupportReq
            .add(GAHearingSupportRequirements.OTHER_SUPPORT);
        respSupportReq
            .add(GAHearingSupportRequirements.HEARING_LOOPS);

        List<Element<GARespondentResponse>> respondentsResponses = new ArrayList<>();
        respondentsResponses
            .add(element(GARespondentResponse.builder()
                             .gaHearingDetails(GAHearingDetails.builder()
                                                   .hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                                   .hearingDuration(GAHearingDuration.HOUR_1)
                                                   .supportRequirement(respSupportReq)
                                                   .build()).build()
        ));

        List<GeneralApplicationTypes> types = List.of(
            (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
        return CaseData.builder()
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .hearingDetailsResp(GAHearingDetails.builder().hearingPreferencesPreferredType(GAHearingType.IN_PERSON)
                                    .hearingDuration(GAHearingDuration.HOUR_1)
                                    .supportRequirement(applSupportReq)
                                    .build())
            .respondentsResponses(respondentsResponses)
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
            .createdDate(LocalDateTime.of(2022, 01, 15, 0, 0, 0))
            .applicantPartyName("ApplicantPartyName")
            .generalAppRespondent1Representative(
                GARespondentRepresentative.builder()
                    .generalAppRespondent1Representative(YES)
                    .build())
            .generalAppType(
                GAApplicationType
                    .builder()
                    .types(types).build())
            .businessProcess(BusinessProcess
                                 .builder()
                                 .camundaEvent(CAMUNDA_EVENT)
                                 .processInstanceId(BUSINESS_PROCESS_INSTANCE_ID)
                                 .status(BusinessProcessStatus.STARTED)
                                 .activityId(ACTIVITY_ID)
                                 .build())
            .ccdState(CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .build();
    }

    private CaseData getCloakedApplication() {
        List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
        return CaseData.builder()
                .parentClaimantIsApplicant(YES)
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(NO).build())
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().build())
                .createdDate(LocalDateTime.of(2022, 01, 15, 0, 0, 0))
                .applicantPartyName("ApplicantPartyName")
                .generalAppRespondent1Representative(
                        GARespondentRepresentative.builder()
                                .generalAppRespondent1Representative(YES)
                                .build())
                .generalAppType(
                        GAApplicationType
                                .builder()
                                .types(types).build())
                .businessProcess(BusinessProcess
                        .builder()
                        .camundaEvent(CAMUNDA_EVENT)
                        .processInstanceId(BUSINESS_PROCESS_INSTANCE_ID)
                        .status(BusinessProcessStatus.STARTED)
                        .activityId(ACTIVITY_ID)
                        .build())
                .ccdState(CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
                .build();
    }

    private CaseData getApplicationByParentCaseDefendant() {
        List<GeneralApplicationTypes> types = List.of(
                (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
        return CaseData.builder()
                .parentClaimantIsApplicant(NO)
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().build())
                .createdDate(LocalDateTime.of(2022, 01, 15, 0, 0, 0))
                .applicantPartyName("ApplicantPartyName")
                .generalAppRespondent1Representative(
                        GARespondentRepresentative.builder()
                                .generalAppRespondent1Representative(YES)
                                .build())
                .generalAppType(
                        GAApplicationType
                                .builder()
                                .types(types).build())
                .businessProcess(BusinessProcess
                        .builder()
                        .camundaEvent(CAMUNDA_EVENT)
                        .processInstanceId(BUSINESS_PROCESS_INSTANCE_ID)
                        .status(BusinessProcessStatus.STARTED)
                        .activityId(ACTIVITY_ID)
                        .build())
                .ccdState(CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
                .build();
    }

}
