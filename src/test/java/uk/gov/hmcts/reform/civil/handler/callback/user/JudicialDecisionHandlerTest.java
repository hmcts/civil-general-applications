package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GARespondentRepresentative;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.JUDGE_MAKES_DECISION;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY;

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

    @Nested
    class MidEventForRequestMoreInfoScreenDateValidity {

        private static final String VALIDATE_REQUEST_MORE_INFO_SCREEN = "validate-request-more-info-screen";
        public static final String REQUESTED_MORE_INFO_BY_DATE_REQUIRED = "The date, by which the applicant must "
                + "respond, is required.";
        public static final String REQUESTED_MORE_INFO_BY_DATE_IN_PAST = "The date, by which the applicant must "
                + "respond, cannot be in past.";
        public static final String OTHER_PARTY_MORE_INFO_BY_DATE_REQUIRED = "The date, by which the other party must "
                + "respond, is required.";
        public static final String OTHER_PARTY_MORE_INFO_BY_DATE_IN_PAST = "The date, by which the other party must "
                + "respond, cannot be in past.";

        @Test
        void shouldNotCauseAnyErrors_whenApplicationDetailsNotProvided() {
            CaseData caseData = CaseDataBuilder.builder().build();
            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldReturnErrors_whenRequestedMoreInfoAndTheDateIsNull() {
            CaseData caseData = getApplication_RequestMoreInformation(REQUEST_MORE_INFORMATION,
                    null, LocalDate.now());

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(REQUESTED_MORE_INFO_BY_DATE_REQUIRED);
        }

        @Test
        void shouldReturnErrors_whenRequestedMoreInfoAndTheDateIsInPast() {
            CaseData caseData = getApplication_RequestMoreInformation(REQUEST_MORE_INFORMATION,
                    LocalDate.now().minusDays(1), LocalDate.now());

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(REQUESTED_MORE_INFO_BY_DATE_IN_PAST);
        }

        @Test
        void shouldNotReturnErrors_whenRequestedMoreInfoAndTheDateIsInFuture() {
            CaseData caseData = getApplication_RequestMoreInformation(REQUEST_MORE_INFORMATION,
                    LocalDate.now().plusDays(1), LocalDate.now());

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldReturnErrors_whenRequestedMoreInfoFromOtherPartyAndTheDateIsNull() {
            CaseData caseData = getApplication_RequestMoreInformation(SEND_APP_TO_OTHER_PARTY,
                    LocalDate.now(), null);

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(OTHER_PARTY_MORE_INFO_BY_DATE_REQUIRED);
        }

        @Test
        void shouldReturnErrors_whenRequestedMoreInfoFromOtherPartyAndTheDateIsInPast() {
            CaseData caseData = getApplication_RequestMoreInformation(SEND_APP_TO_OTHER_PARTY,
                    LocalDate.now(), LocalDate.now().minusDays(1));

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(OTHER_PARTY_MORE_INFO_BY_DATE_IN_PAST);
        }

        @Test
        void shouldNotReturnErrors_whenRequestedMoreInfoFromOtherPartyAndTheDateIsInFuture() {
            CaseData caseData = getApplication_RequestMoreInformation(SEND_APP_TO_OTHER_PARTY,
                    LocalDate.now(), LocalDate.now().plusDays(1));

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldNotCauseAnyErrors_whenApplicationIsNotUrgentAndConsiderationDateIsNotProvided() {
            CaseData caseData = getApplication_RequestMoreInformation(null,
                    LocalDate.now().minusDays(1), LocalDate.now().minusDays(1));

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        private CaseData getApplication_RequestMoreInformation(GAJudgeRequestMoreInfoOption option,
                                                               LocalDate judgeRequestMoreInfoByDate,
                                                               LocalDate judgeSendAppToOtherPartyResponseByDate) {
            List<GeneralApplicationTypes> types = List.of(
                    (GeneralApplicationTypes.SUMMARY_JUDGEMENT));
            return CaseData.builder()
                    .parentClaimantIsApplicant(YES)
                    .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                    .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(NO).build())
                    .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().build())
                    .createdDate(LocalDateTime.of(2022, 1, 15, 0, 0, 0))
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
                    .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                            .makeAnOrder(APPROVE_OR_EDIT)
                            .build())
                    .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                            .requestMoreInfoOption(option)
                            .judgeRequestMoreInfoByDate(judgeRequestMoreInfoByDate)
                            .judgeSendAppToOtherPartyResponseByDate(judgeSendAppToOtherPartyResponseByDate)
                            .build())
                    .build();
        }
    }
}