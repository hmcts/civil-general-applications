package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.service.NotificationService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@SpringBootTest(classes = {
    JudicialDecisionNotificationHandler.class,
    JacksonAutoConfiguration.class,
})
class JudicialDecisionNotificationHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private JudicialDecisionNotificationHandler handler;

    @MockBean
    private NotificationsProperties notificationsProperties;

    @MockBean
    private NotificationService notificationService;

    private static final Long CASE_REFERENCE = 111111L;
    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";
    //    private static final String DUMMY_DATE = "2022-02-15T12:00";
    private static final String CASE_EVENT = "START_NOTIFICATION_PROCESS_MAKE_DECISION";
    private static final String ORG_ID = "1";
    private static final String ID = "1";
    private static final String FOR_SUMMARY_JUDGEMENT = "for summary judgment";
    private static final String SAMPLE_TEMPLATE = "general-application-apps-judicial-notification-template-id";
    private static final String JUDGES_DECISION = "JUDGE_MAKES_DECISION";

    @Nested
    class AboutToSubmitCallback {

        @BeforeEach
        void setup() {
            when(notificationsProperties.getRespondentWrittenRepConcurrentRepresentationEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getApplicantWrittenRepConcurrentRepresentationEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getRespondentWrittenRepSequentialRepresentationEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getApplicantWrittenRepSequentialRepresentationEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeDismissesOrderApplicantEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeListsForHearingApplicantEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeUncloaksApplicationApplicantEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getWithNoticeUpdateRespondentEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
        }

//        @Test
//        void notificationShouldSendThriceForConcurrentWrittenRepsWhenInvoked() {
//            CallbackParams params = CallbackParamsBuilder
//                .builder().of(ABOUT_TO_SUBMIT,
//                              caseDataForConcurrentWrittenOption())
//                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();
//            handler.handle(params);
//
//            verify(notificationService, times(3)).sendMail(
//                DUMMY_EMAIL,
//                "general-application-apps-judicial-notification-template-id",
//                notificationProperties(),
//                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
//            );
//        }

//        @Test
//        void notificationShouldSendThriceForSequentialWrittenRepsWhenInvoked() {
//            CallbackParams params = CallbackParamsBuilder
//                .builder().of(ABOUT_TO_SUBMIT,
//                              caseDataForSequentialWrittenOption())
//                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();
//            handler.handle(params);
//
//            verify(notificationService, times(3)).sendMail(
//                DUMMY_EMAIL,
//                "general-application-apps-judicial-notification-template-id",
//                notificationProperties(),
//                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
//            );
//        }
//
//        @Test
//        void notificationShouldSendForSequentialWrittenRepsWhenInvoked() {
//            CallbackParams params = CallbackParamsBuilder
//                .builder().of(ABOUT_TO_SUBMIT,
//                              caseDataForSequentialWrittenRepRespondentNotPresent())
//                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();
//            handler.handle(params);
//
//            verify(notificationService).sendMail(
//                DUMMY_EMAIL,
//                "general-application-apps-judicial-notification-template-id",
//                notificationProperties(),
//                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
//            );
//        }

//        @Test
//        void notificationShouldSendForConcurrentWrittenRepsWhenInvoked() {
//            CallbackParams params = CallbackParamsBuilder
//                .builder().of(ABOUT_TO_SUBMIT,
//                              caseDataForConcurrentWrittenRepRespondentNotPresent())
//                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();
//            handler.handle(params);
//
//            verify(notificationService).sendMail(
//                DUMMY_EMAIL,
//                "general-application-apps-judicial-notification-template-id",
//                notificationProperties(),
//                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
//            );
//        }

        @Test
        void notificationShouldSendForDismissal() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudgeDismissal())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();
            handler.handle(params);

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationProperties(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendListForHearing() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataListForHearing())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();
            handler.handle(params);

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationProperties(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        private CaseData caseDataForConcurrentWrittenOption() {
            return CaseData.builder()
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .generalAppRespondentSolicitors(respondentSolicitors())
                .generalAppType(GAApplicationType.builder().types(applicationType()).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                .caseReference(CASE_REFERENCE.toString()).build())
                .judicialDecisionMakeAnOrderForWrittenRepresentations(
                    GAJudicialWrittenRepresentations.builder().writtenOption(
                    GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .build();
        }

        private CaseData caseDataForSequentialWrittenOption() {
            return CaseData.builder()
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .generalAppRespondentSolicitors(respondentSolicitors())
                .generalAppType(GAApplicationType.builder().types(applicationType()).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .judicialDecisionMakeAnOrderForWrittenRepresentations(
                    GAJudicialWrittenRepresentations.builder().writtenOption(
                        GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS).build())
                .build();
        }

        private CaseData caseDataForSequentialWrittenRepRespondentNotPresent() {
            return CaseData.builder()
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .generalAppType(GAApplicationType.builder().types(applicationType()).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .judicialDecisionMakeAnOrderForWrittenRepresentations(
                    GAJudicialWrittenRepresentations.builder().writtenOption(
                        GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS).build())
                .build();
        }

        private CaseData caseDataForConcurrentWrittenRepRespondentNotPresent() {
            return CaseData.builder()
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .generalAppType(GAApplicationType.builder().types(applicationType()).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .judicialDecisionMakeAnOrderForWrittenRepresentations(
                    GAJudicialWrittenRepresentations.builder().writtenOption(
                        GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS).build())
                .build();
        }

        private CaseData caseDataForJudgeDismissal() {
            return CaseData.builder()
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION).build())
                .judicialDecision(GAJudicialDecision.builder()
                                      .decision(GAJudgeDecisionOption.LIST_FOR_A_HEARING).build())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationType()).build())
                .build();
        }

        private CaseData caseDataListForHearing() {
            return CaseData.builder()
                .judicialDecision(GAJudicialDecision.builder()
                                      .decision(GAJudgeDecisionOption.LIST_FOR_A_HEARING).build())
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION).build())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationType()).build())
                .build();
        }

        private List<GeneralApplicationTypes> applicationType() {
            return List.of(
                GeneralApplicationTypes.SUMMARY_JUDGEMENT,
                GeneralApplicationTypes.STAY_THE_CLAIM,
                GeneralApplicationTypes.EXTEND_TIME,
                GeneralApplicationTypes.STRIKE_OUT
            );
        }

        private List<Element<GASolicitorDetailsGAspec>> respondentSolicitors() {
            return Arrays.asList(element(GASolicitorDetailsGAspec.builder().id(ID)
                                             .email(DUMMY_EMAIL).organisationIdentifier(ORG_ID).build()),
                                 element(GASolicitorDetailsGAspec.builder().id(ID)
                                             .email(DUMMY_EMAIL).organisationIdentifier(ORG_ID).build())
            );
        }

        private Map<String, String> notificationProperties() {
            return Map.of(
                NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                NotificationData.GA_APPLICATION_TYPE, FOR_SUMMARY_JUDGEMENT
            );
        }
    }
}
