package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.PaymentStatus.SUCCESS;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@SpringBootTest(classes = {
    JudicialNotificationService.class,
    JacksonAutoConfiguration.class,
})
class JudicialNotificationServiceTest {

    @MockBean
    private NotificationsProperties notificationsProperties;
    @MockBean
    private Time time;
    @MockBean
    private DeadlinesCalculator deadlinesCalculator;
    @Autowired
    private JudicialNotificationService judicialNotificationService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private SolicitorEmailValidation solicitorEmailValidation;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    private static final Long CASE_REFERENCE = 111111L;
    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";
    private static final String DUMMY_DATE = "2022-11-12";
    private static final String CASE_EVENT = "START_NOTIFICATION_PROCESS_MAKE_DECISION";
    private static final String ORG_ID = "1";
    private static final String ID = "1";
    private static final String SAMPLE_TEMPLATE = "general-application-apps-judicial-notification-template-id";
    private static final String JUDGES_DECISION = "MAKE_DECISION";
    private LocalDateTime responseDate = LocalDateTime.now();
    private LocalDateTime deadline = LocalDateTime.now().plusDays(5);

    @Nested
    class AboutToSubmitCallback {

        @BeforeEach
        void setup() {
            when(notificationsProperties.getWrittenRepConcurrentRepresentationRespondentEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getWrittenRepConcurrentRepresentationApplicantEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getWrittenRepSequentialRepresentationRespondentEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getWrittenRepSequentialRepresentationApplicantEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeDismissesOrderApplicantEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeListsForHearingApplicantEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeDismissesOrderApplicantEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getWithNoticeUpdateRespondentEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeForApproveRespondentEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeForApprovedCaseApplicantEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeListsForHearingRespondentEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeDismissesOrderRespondentEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeForDirectionOrderApplicantEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeForDirectionOrderRespondentEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeRequestForInformationApplicantEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeRequestForInformationRespondentEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeUncloakApplicationEmailTemplate())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeApproveOrderToStrikeOutDamages())
                .thenReturn(SAMPLE_TEMPLATE);
            when(notificationsProperties.getJudgeApproveOrderToStrikeOutOCMC())
                .thenReturn(SAMPLE_TEMPLATE);
        }

        @Test
        void notificationShouldSendThriceForConcurrentWrittenRepsWhenInvoked() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForConcurrentWrittenOption());

            judicialNotificationService.sendNotification(caseDataForConcurrentWrittenOption());
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendThriceForSequentialWrittenRepsWhenInvoked() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForSequentialWrittenOption());

            judicialNotificationService.sendNotification(caseDataForSequentialWrittenOption());
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToGetReliefFromSanctions(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendForConcurrentWrittenRepsWhenInvoked() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForConcurrentWrittenRepRespondentNotPresent());

            judicialNotificationService.sendNotification(caseDataForConcurrentWrittenRepRespondentNotPresent());
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendForDismissal() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudgeDismissal(NO, YES, NO));

            judicialNotificationService.sendNotification(caseDataForJudgeDismissal(NO, YES, NO));
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationUncloakShouldSendForDismissal() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudgeDismissal(NO, NO, NO));

            judicialNotificationService.sendNotification(caseDataForJudgeDismissal(NO, NO, NO));
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendListForHearing() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataListForHearing());

            judicialNotificationService.sendNotification(caseDataListForHearing());
            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToExtendTime(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendToApplicant_IfApplicationCloakedWithApplicantSolicitor() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForCloakedApplicationWithSolicitorDataOnly());

            judicialNotificationService.sendNotification(caseDataForCloakedApplicationWithSolicitorDataOnly());
            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgement(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendSendToApplicant_IfApplicationUncloakedForApproveOrEdit() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataWithSolicitorDataOnlyForApplicationUncloakedJudgeApproveOrEdit());

            judicialNotificationService.sendNotification(
                caseDataWithSolicitorDataOnlyForApplicationUncloakedJudgeApproveOrEdit());
            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStayTheClaim(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfApplicationIsToAmendStatementOfCase() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForAmendStatementOfClaim());

            judicialNotificationService.sendNotification(caseDataForAmendStatementOfClaim());
            verify(notificationService, times(1)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfJudicialApproval() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialApprovalOfApplication(NO, YES));

            judicialNotificationService.sendNotification(caseDataForJudicialApprovalOfApplication(NO, YES));
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfJudicialDirectionOrder_AfterAdditionalPaymentReceived() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialDirectionOrderOfApplication(NO, NO).toBuilder().generalAppPBADetails(
                        GAPbaDetails.builder()
                            .additionalPaymentDetails(buildAdditionalPaymentSuccessData())
                            .build())
                                .build());

            judicialNotificationService.sendNotification(
                caseDataForJudicialApprovalOfApplication(NO, NO).toBuilder()
                    .generalAppPBADetails(GAPbaDetails.builder()
                                              .additionalPaymentDetails(buildAdditionalPaymentSuccessData())
                                              .build())
                    .build());
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationUncloakShouldSendIfJudicialApproval_AfterAdditionalPaymentReceived() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialApprovalOfApplication(NO, NO).toBuilder().generalAppPBADetails(
                        GAPbaDetails.builder()
                            .additionalPaymentDetails(buildAdditionalPaymentSuccessData())
                            .build())
                                .build());

            judicialNotificationService.sendNotification(
                caseDataForJudicialApprovalOfApplication(NO, NO).toBuilder()
                    .generalAppPBADetails(GAPbaDetails.builder()
                                              .additionalPaymentDetails(buildAdditionalPaymentSuccessData())
                                              .build())
                    .build());
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfJudicialDirectionOrder() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialDirectionOrderOfApplication(NO, NO));

            judicialNotificationService.sendNotification(caseDataForJudicialApprovalOfApplication(NO, NO));
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationUncloakShouldSendIfJudicialDirectionOrder() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialDirectionOrderOfApplication(YES, NO));

            judicialNotificationService.sendNotification(caseDataForJudicialDirectionOrderOfApplication(YES, NO));
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfJudicialDirectionOrderRepArePresentInList() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialDirectionOrderOfApplicationWhenRespondentsArePresentInList(NO, YES, NO));

            judicialNotificationService
                .sendNotification(
                    caseDataForJudicialDirectionOrderOfApplicationWhenRespondentsArePresentInList(NO, YES, NO));
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationUncloakShouldSendIfJudicialDirectionOrderRepArePresentInList() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialDirectionOrderOfApplicationWhenRespondentsArePresentInList(NO, NO, NO));

            judicialNotificationService
                .sendNotification(
                    caseDataForJudicialDirectionOrderOfApplicationWhenRespondentsArePresentInList(NO, NO, NO));
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfJudicialRequestForInformationWithoutNotice() {
            CaseData caseData = caseDataForJudicialRequestForInformationOfApplication(NO, NO, YES,
                                                                                      SEND_APP_TO_OTHER_PARTY);

            when(time.now()).thenReturn(responseDate);
            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseData);
            when(deadlinesCalculator.calculateApplicantResponseDeadline(
                any(LocalDateTime.class), any(Integer.class))).thenReturn(deadline);

            var responseCaseData = judicialNotificationService.sendNotification(caseData);

            assertThat(responseCaseData.getJudicialDecisionRequestMoreInfo().getDeadlineForMoreInfoSubmission())
                .isEqualTo(deadline.toString());

            verify(notificationService, times(1)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStayTheClaim(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfJudicialRequestForInformationWithNotice() {
            CaseData caseData = caseDataForJudicialRequestForInformationOfApplication(NO, YES, NO,
                                                                                      REQUEST_MORE_INFORMATION);

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);

            judicialNotificationService.sendNotification(caseData);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStayTheClaim(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void shouldSendAdditionalPaymentNotification_UncloakRequestForInformation_BeforeAdditionalPaymentMade() {

            CaseData caseData = caseDataForJudicialRequestForInformationOfApplication(NO, NO, NO,
                                                                                      SEND_APP_TO_OTHER_PARTY);
            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseData);

            judicialNotificationService.sendNotification(caseData);

            verify(notificationService, times(1)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStayTheClaim(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void shouldSendNotification_UncloakRequestForInformation_AfterAdditionalPaymentMade() {

            CaseData caseData = caseDataForJudicialRequestForInformationOfApplication(NO, NO, NO,
                                                                                      SEND_APP_TO_OTHER_PARTY
            ).toBuilder().generalAppPBADetails(GAPbaDetails.builder()
                                                   .additionalPaymentDetails(buildAdditionalPaymentSuccessData())
                                                   .build())
                .build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseData);

            when(time.now()).thenReturn(responseDate);
            when(deadlinesCalculator.calculateApplicantResponseDeadline(
                any(LocalDateTime.class), any(Integer.class))).thenReturn(deadline);

            var responseCaseData = judicialNotificationService.sendNotification(caseData);

            assertThat(responseCaseData.getJudicialDecisionRequestMoreInfo().getDeadlineForMoreInfoSubmission())
                .isEqualTo(deadline.toString());

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStayTheClaim(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfApplicationUncloakedDismissed_WithoutRespondentEmail() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForApplicationUncloakedIsDismissed());

            judicialNotificationService.sendNotification(caseDataForApplicationUncloakedIsDismissed());
            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfSequentialWrittenRepsArePresentInList() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForSequentialWrittenRepInList());

            judicialNotificationService.sendNotification(caseDataForSequentialWrittenRepInList());
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendForApplicationsApprovedWhenRespondentsAreInList() {
            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForApplicationsApprovedWhenRespondentsAreInList(NO, YES));

            judicialNotificationService
                .sendNotification(caseDataForApplicationsApprovedWhenRespondentsAreInList(NO, YES));
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void shouldSendNotificationToRespondent_ForApplicationApprovedUncloaked_WhenRespondentsAreInList() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForApplicationsApprovedWhenRespondentsAreInList(NO, NO));

            judicialNotificationService
                .sendNotification(caseDataForApplicationsApprovedWhenRespondentsAreInList(NO, NO));
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void shouldSendNotification_WhenApplicationUncloaked() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(
                    caseDataForApplicationsApprovedWhenRespondentsAreInList(NO, NO)
                        .toBuilder().generalAppPBADetails(GAPbaDetails.builder()
                            .additionalPaymentDetails(buildAdditionalPaymentSuccessData())
                            .build())
                                .build());

            CaseData caseData = caseDataForApplicationsApprovedWhenRespondentsAreInList(NO, NO)
                .toBuilder().generalAppPBADetails(
                    GAPbaDetails.builder()
                        .additionalPaymentDetails(buildAdditionalPaymentSuccessData())
                        .build())
                .build();

            judicialNotificationService.sendNotification(caseData);
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendApproveDamagesWhenRespondentsAreInList() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(
                    caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(NO, YES, NO, "UNSPEC_CLAIM"));

            judicialNotificationService
                .sendNotification(
                    caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(NO, YES, NO, "UNSPEC_CLAIM"));
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendApproveOcmcWhenRespondentsAreInList() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(
                    caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(NO, YES, NO, "SPEC_CLAIM"));

            judicialNotificationService
                .sendNotification(
                    caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(NO, YES, NO, "SPEC_CLAIM"));

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendForApplicationListForHearingWhenRespondentsAreAvailableInList() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForListForHearingRespondentsAreInList());

            judicialNotificationService.sendNotification(caseDataForListForHearingRespondentsAreInList());
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendWhenApplicationIsDismissedByJudgeWhenRespondentsAreAvailableInList() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForCaseDismissedByJudgeRespondentsAreInList(NO, YES, NO));

            judicialNotificationService.sendNotification(
                caseDataForCaseDismissedByJudgeRespondentsAreInList(NO, YES, NO));

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationUncloakShouldSendForApprovedDamageWhenRespondentsAreInList() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(
                    caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(NO, NO, NO, "UNSPEC_CLAIM"));

            judicialNotificationService
                .sendNotification(
                    caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(NO, NO, NO, "UNSPEC_CLAIM"));

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationUncloakShouldSendForApprovedOcmcWhenRespondentsAreInList() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(NO, NO, NO, "SPEC_CLAIM"));

            judicialNotificationService
                .sendNotification(
                    caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(NO, NO, NO, "SPEC_CLAIM"));

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void shouldSendNotification_WhenAdditionalPaymentReceived_JudgeDismissedApplicationUncloaked() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(
                    caseDataForCaseDismissedByJudgeRespondentsAreInList(NO, NO, YES).toBuilder()
                        .generalAppPBADetails(GAPbaDetails.builder()
                        .additionalPaymentDetails(buildAdditionalPaymentSuccessData())
                        .build())
                .build()
                );

            CaseData caseData = caseDataForCaseDismissedByJudgeRespondentsAreInList(NO, NO, YES)
                .toBuilder().generalAppPBADetails(
                    GAPbaDetails.builder()
                        .additionalPaymentDetails(buildAdditionalPaymentSuccessData())
                        .build())
                .build();

            judicialNotificationService.sendNotification(caseData);
            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendWhenJudgeApprovesOrderApplicationIsCloak() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudgeApprovedOrderCloakWhenRespondentsArePresentInList());

            judicialNotificationService
                .sendNotification(caseDataForJudgeApprovedOrderCloakWhenRespondentsArePresentInList());
            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendWhenJudgeApprovesOrderDamageApplicationIsCloak() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForApprovedCloakStrikeOutWhenRespondentsArePresentInList("UNSPEC_CLAIM"));

            judicialNotificationService
                .sendNotification(caseDataForApprovedCloakStrikeOutWhenRespondentsArePresentInList(
                    "UNSPEC_CLAIM"));

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendWhenJudgeApprovesOrderOcmcApplicationIsCloak() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForApprovedCloakStrikeOutWhenRespondentsArePresentInList("SPEC_CLAIM"));

            judicialNotificationService
                .sendNotification(caseDataForApprovedCloakStrikeOutWhenRespondentsArePresentInList("SPEC_CLAIM"));

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendWhenJudgeDismissedTheApplicationIsCloak() {

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudgeDismissTheApplicationCloakWhenRespondentsArePresentInList());

            judicialNotificationService
                .sendNotification(caseDataForJudgeDismissTheApplicationCloakWhenRespondentsArePresentInList());
            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        private CaseData caseDataForConcurrentWrittenOption() {
            return
                CaseData.builder()
                    .generalAppRespondentSolicitors(respondentSolicitors())
                    .judicialDecision(GAJudicialDecision.builder()
                                          .decision(GAJudgeDecisionOption.LIST_FOR_A_HEARING).build())
                    .generalAppRespondentSolicitors(respondentSolicitors())
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                                  .email(DUMMY_EMAIL).build())
                    .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                    .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                                  .caseReference(CASE_REFERENCE.toString()).build())
                    .judicialDecisionMakeAnOrderForWrittenRepresentations(
                        GAJudicialWrittenRepresentations.builder().writtenOption(
                            GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS).build())
                    .generalAppType(GAApplicationType.builder()
                                        .types(applicationTypeSummeryJudgement()).build())
                    .judicialConcurrentDateText(DUMMY_DATE)
                    .build();
        }

        private CaseData caseDataForSequentialWrittenOption() {
            return CaseData.builder()
                .generalAppRespondentSolicitors(respondentSolicitors())
                .judicialDecision(GAJudicialDecision.builder()
                                      .decision(GAJudgeDecisionOption.LIST_FOR_A_HEARING).build())
                .generalAppRespondentSolicitors(respondentSolicitors())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .judicialDecisionMakeAnOrderForWrittenRepresentations(
                    GAJudicialWrittenRepresentations.builder().writtenOption(
                        GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeToGetReliefFromSanctions()).build())
                .build();
        }

        private CaseData caseDataForConcurrentWrittenRepRespondentNotPresent() {
            return CaseData.builder()
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .generalAppRespondentSolicitors(respondentSolicitors())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .judicialDecisionMakeAnOrderForWrittenRepresentations(
                    GAJudicialWrittenRepresentations.builder().writtenOption(
                        GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeSummeryJudgement()).build())
                .judicialConcurrentDateText(DUMMY_DATE)
                .build();
        }

        private CaseData caseDataForJudgeDismissal(YesOrNo orderAgreement, YesOrNo isWithNotice, YesOrNo isCloaked) {
            return CaseData.builder()
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(orderAgreement).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isWithNotice).build())
                .applicationIsCloaked(isCloaked)
                .generalAppRespondentSolicitors(respondentSolicitors())
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION).build())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeToStrikeOut()).build())
                .generalAppPBADetails(GAPbaDetails.builder().build())
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
                                    .types(applicationTypeToExtendTheClaim()).build())
                .build();
        }

        private CaseData caseDataForCloakedApplicationWithSolicitorDataOnly() {
            return CaseData.builder()
                .applicationIsCloaked(YES)
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(GIVE_DIRECTIONS_WITHOUT_HEARING).build())
                .judicialDecision(GAJudicialDecision.builder()
                                      .decision(GAJudgeDecisionOption.MAKE_AN_ORDER).build())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeSummeryJudgement()).build())
                .build();
        }

        private CaseData caseDataWithSolicitorDataOnlyForApplicationUncloakedJudgeApproveOrEdit() {
            return CaseData.builder()
                .applicationIsCloaked(NO)
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT).build())
                .judicialDecision(GAJudicialDecision.builder()
                                      .decision(GAJudgeDecisionOption.MAKE_AN_ORDER).build())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(YES).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeToStayTheClaim()).build())
                .build();
        }

        private CaseData caseDataForApplicationUncloakedIsDismissed() {
            return CaseData.builder()
                .applicationIsCloaked(NO)
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION).build())
                .judicialDecision(GAJudicialDecision.builder()
                                      .decision(GAJudgeDecisionOption.MAKE_AN_ORDER).build())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeToStrikeOut()).build())
                .build();
        }

        private CaseData caseDataForAmendStatementOfClaim() {
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
                                    .types(applicationTypeToAmendStatmentOfClaim()).build())
                .build();
        }

        private CaseData caseDataForJudicialApprovalOfApplication(YesOrNo orderAgreement, YesOrNo isWithNotice) {
            return CaseData.builder()
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(orderAgreement).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isWithNotice).build())
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT).build())
                .generalAppRespondentSolicitors(respondentSolicitors())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeToAmendStatmentOfClaim()).build())
                .generalAppPBADetails(GAPbaDetails.builder().build())
                .build();
        }

        private CaseData caseDataForJudicialDirectionOrderOfApplication(YesOrNo orderAgreement, YesOrNo isWithNotice) {
            return CaseData.builder()
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(orderAgreement).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isWithNotice).build())
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(GIVE_DIRECTIONS_WITHOUT_HEARING).build())
                .generalAppRespondentSolicitors(respondentSolicitors())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeToAmendStatmentOfClaim()).build())
                .generalAppPBADetails(GAPbaDetails.builder().build())
                .build();
        }

        private CaseData caseDataForJudicialDirectionOrderOfApplicationWhenRespondentsArePresentInList(
            YesOrNo orderAgreement,
            YesOrNo isWithNotice,
            YesOrNo isCloaked) {
            return CaseData.builder()
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(orderAgreement).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isWithNotice).build())
                .applicationIsCloaked(isCloaked)
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(GIVE_DIRECTIONS_WITHOUT_HEARING).build())
                .generalAppRespondentSolicitors(respondentSolicitors())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeToAmendStatmentOfClaim()).build())
                .generalAppPBADetails(GAPbaDetails.builder().build())
                .build();
        }

        private CaseData caseDataForJudicialRequestForInformationOfApplication(
            YesOrNo isRespondentOrderAgreement, YesOrNo isWithNotice, YesOrNo isCloaked,
            GAJudgeRequestMoreInfoOption gaJudgeRequestMoreInfoOption) {

            return CaseData.builder()
                .generalAppRespondentSolicitors(respondentSolicitors())
                .applicationIsCloaked(isCloaked)
                .judicialDecision(GAJudicialDecision.builder()
                                      .decision(GAJudgeDecisionOption.REQUEST_MORE_INFO).build())
                .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                     .requestMoreInfoOption(gaJudgeRequestMoreInfoOption)
                                                     .judgeRequestMoreInfoText("Test")
                                                     .judgeRequestMoreInfoByDate(LocalDate.now()).build())
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                                   .hasAgreed(isRespondentOrderAgreement).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isWithNotice).build())
                .generalAppRespondentSolicitors(respondentSolicitors())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeToStayTheClaim()).build())
                .generalAppPBADetails(GAPbaDetails.builder().build())
                .build();

        }

        private CaseData caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(
            YesOrNo orderAgreement,
            YesOrNo isWithNotice, YesOrNo isCloaked, String superClaimType) {
            return
                CaseData.builder()
                    .generalAppRespondentSolicitors(respondentSolicitors())
                    .generalAppSuperClaimType(superClaimType)
                    .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                                       .hasAgreed(orderAgreement).build())
                    .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isWithNotice).build())
                    .applicationIsCloaked(isCloaked)
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                                  .email(DUMMY_EMAIL).build())
                    .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                    .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                                  .caseReference(CASE_REFERENCE.toString()).build())
                    .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                   .makeAnOrder(
                                                       GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT).build())
                    .generalAppType(GAApplicationType.builder()
                                        .types(applicationTypeToStrikeOut()).build())
                    .judicialConcurrentDateText(DUMMY_DATE)
                    .generalAppPBADetails(GAPbaDetails.builder().build())
                    .build();
        }

        private CaseData caseDataForSequentialWrittenRepInList() {
            return
                CaseData.builder()
                    .judicialDecision(GAJudicialDecision.builder()
                                          .decision(GAJudgeDecisionOption.LIST_FOR_A_HEARING).build())
                    .generalAppRespondentSolicitors(respondentSolicitors())
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                                  .email(DUMMY_EMAIL).build())
                    .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                    .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                                  .caseReference(CASE_REFERENCE.toString()).build())
                    .judicialDecisionMakeAnOrderForWrittenRepresentations(
                        GAJudicialWrittenRepresentations.builder().writtenOption(
                            GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS).build())
                    .generalAppType(GAApplicationType.builder()
                                        .types(applicationTypeSummeryJudgement()).build())
                    .judicialConcurrentDateText(DUMMY_DATE)
                    .generalAppPBADetails(GAPbaDetails.builder().build())
                    .build();
        }

        private CaseData caseDataForApplicationsApprovedWhenRespondentsAreInList(YesOrNo orderAgreement,
                                                                                 YesOrNo isWithNotice) {
            return
                CaseData.builder()
                    .generalAppRespondentSolicitors(respondentSolicitors())
                    .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                                       .hasAgreed(orderAgreement).build())
                    .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isWithNotice).build())
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                                  .email(DUMMY_EMAIL).build())
                    .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                    .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                                  .caseReference(CASE_REFERENCE.toString()).build())
                    .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                   .makeAnOrder(
                                                       GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT).build())
                    .generalAppType(GAApplicationType.builder()
                                        .types(applicationTypeSummeryJudgement()).build())
                    .judicialConcurrentDateText(DUMMY_DATE)
                    .generalAppPBADetails(GAPbaDetails.builder().build())
                    .build();
        }

        private CaseData caseDataForListForHearingRespondentsAreInList() {
            return
                CaseData.builder()
                    .generalAppRespondentSolicitors(respondentSolicitors())
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                                  .email(DUMMY_EMAIL).build())
                    .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                    .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                                  .caseReference(CASE_REFERENCE.toString()).build())
                    .judicialDecision(GAJudicialDecision.builder()
                                          .decision(GAJudgeDecisionOption.LIST_FOR_A_HEARING).build())
                    .generalAppType(GAApplicationType.builder()
                                        .types(applicationTypeSummeryJudgement()).build())
                    .judicialConcurrentDateText(DUMMY_DATE)
                    .build();
        }

        private CaseData caseDataForCaseDismissedByJudgeRespondentsAreInList(YesOrNo orderAgreement,
                                                                             YesOrNo isWithNotice,
                                                                             YesOrNo isCloaked) {
            return
                CaseData.builder()
                    .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                                       .hasAgreed(orderAgreement).build())
                    .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isWithNotice).build())
                    .applicationIsCloaked(isCloaked)
                    .generalAppRespondentSolicitors(respondentSolicitors())
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                                  .email(DUMMY_EMAIL).build())
                    .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                    .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                                  .caseReference(CASE_REFERENCE.toString()).build())
                    .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().makeAnOrder(
                        GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION
                    ).build())
                    .generalAppType(GAApplicationType.builder()
                                        .types(applicationTypeSummeryJudgement()).build())
                    .judicialConcurrentDateText(DUMMY_DATE)
                    .generalAppPBADetails(GAPbaDetails.builder().build())
                    .build();
        }

        private CaseData caseDataForApprovedCloakStrikeOutWhenRespondentsArePresentInList(String superClaimType) {
            return
                CaseData.builder()
                    .generalAppRespondentSolicitors(respondentSolicitors())
                    .generalAppSuperClaimType(superClaimType)
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                                  .email(DUMMY_EMAIL).build())
                    .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                    .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                                  .caseReference(CASE_REFERENCE.toString()).build())
                    .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().makeAnOrder(
                        GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT
                    ).build())
                    .judicialDecision(GAJudicialDecision.builder()
                                          .decision(GAJudgeDecisionOption.MAKE_AN_ORDER).build())
                    .generalAppType(GAApplicationType.builder()
                                        .types(applicationTypeToStrikeOut()).build())
                    .applicationIsCloaked(YES)
                    .judicialConcurrentDateText(DUMMY_DATE)
                    .build();
        }

        private CaseData caseDataForJudgeApprovedOrderCloakWhenRespondentsArePresentInList() {
            return
                CaseData.builder()
                    .generalAppRespondentSolicitors(respondentSolicitors())
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                                  .email(DUMMY_EMAIL).build())
                    .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                    .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                                  .caseReference(CASE_REFERENCE.toString()).build())
                    .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().makeAnOrder(
                        GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT
                    ).build())
                    .judicialDecision(GAJudicialDecision.builder()
                                          .decision(GAJudgeDecisionOption.MAKE_AN_ORDER).build())
                    .generalAppType(GAApplicationType.builder()
                                        .types(applicationTypeSummeryJudgement()).build())
                    .applicationIsCloaked(YES)
                    .judicialConcurrentDateText(DUMMY_DATE)
                    .build();
        }

        private CaseData caseDataForJudgeDismissTheApplicationCloakWhenRespondentsArePresentInList() {
            return
                CaseData.builder()
                    .generalAppRespondentSolicitors(respondentSolicitors())
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                                  .email(DUMMY_EMAIL).build())
                    .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                    .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                                  .caseReference(CASE_REFERENCE.toString()).build())
                    .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().makeAnOrder(
                        GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION
                    ).build())
                    .judicialDecision(GAJudicialDecision.builder()
                                          .decision(GAJudgeDecisionOption.MAKE_AN_ORDER).build())
                    .generalAppType(GAApplicationType.builder()
                                        .types(applicationTypeSummeryJudgement()).build())
                    .applicationIsCloaked(YES)
                    .judicialConcurrentDateText(DUMMY_DATE)
                    .build();
        }

        private List<Element<GASolicitorDetailsGAspec>> respondentSolicitors() {
            return Arrays.asList(element(GASolicitorDetailsGAspec.builder().id(ID)
                                             .email(DUMMY_EMAIL).organisationIdentifier(ORG_ID).build()),
                                 element(GASolicitorDetailsGAspec.builder().id(ID)
                                             .email(DUMMY_EMAIL).organisationIdentifier(ORG_ID).build())
            );
        }

        private Map<String, String> notificationPropertiesSummeryJudgement() {
            return Map.of(
                NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                NotificationData.GA_APPLICATION_TYPE,
                GeneralApplicationTypes.SUMMARY_JUDGEMENT.getDisplayedValue()
            );
        }

        private List<GeneralApplicationTypes> applicationTypeSummeryJudgement() {
            return List.of(
                GeneralApplicationTypes.SUMMARY_JUDGEMENT
            );
        }

        private Map<String, String> notificationPropertiesSummeryJudgementConcurrent() {
            return Map.of(
                NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                NotificationData.GA_APPLICATION_TYPE,
                GeneralApplicationTypes.SUMMARY_JUDGEMENT.getDisplayedValue()
            );
        }

        private Map<String, String> notificationPropertiesToStrikeOut() {
            return Map.of(
                NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                NotificationData.GA_APPLICATION_TYPE,
                GeneralApplicationTypes.STRIKE_OUT.getDisplayedValue()
            );
        }

        private List<GeneralApplicationTypes> applicationTypeToStrikeOut() {
            return List.of(
                GeneralApplicationTypes.STRIKE_OUT
            );
        }

        private Map<String, String> notificationPropertiesToStayTheClaim() {
            return Map.of(
                NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                NotificationData.GA_APPLICATION_TYPE,
                GeneralApplicationTypes.STAY_THE_CLAIM.getDisplayedValue()
            );
        }

        private List<GeneralApplicationTypes> applicationTypeToStayTheClaim() {
            return List.of(
                GeneralApplicationTypes.STAY_THE_CLAIM
            );
        }

        private Map<String, String> notificationPropertiesToExtendTime() {
            return Map.of(
                NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                NotificationData.GA_APPLICATION_TYPE,
                GeneralApplicationTypes.EXTEND_TIME.getDisplayedValue()
            );
        }

        private List<GeneralApplicationTypes> applicationTypeToExtendTheClaim() {
            return List.of(
                GeneralApplicationTypes.EXTEND_TIME
            );
        }

        private Map<String, String> notificationPropertiesToAmendStatementOfCase() {
            return Map.of(
                NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                NotificationData.GA_APPLICATION_TYPE,
                GeneralApplicationTypes.AMEND_A_STMT_OF_CASE.getDisplayedValue()
            );
        }

        private List<GeneralApplicationTypes> applicationTypeToAmendStatmentOfClaim() {
            return List.of(
                GeneralApplicationTypes.AMEND_A_STMT_OF_CASE
            );
        }

        private Map<String, String> notificationPropertiesToGetReliefFromSanctions() {
            return Map.of(
                NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                NotificationData.GA_APPLICATION_TYPE,
                GeneralApplicationTypes.RELIEF_FROM_SANCTIONS.getDisplayedValue()
            );
        }

        private List<GeneralApplicationTypes> applicationTypeToGetReliefFromSanctions() {
            return List.of(
                GeneralApplicationTypes.RELIEF_FROM_SANCTIONS
            );
        }

        private PaymentDetails buildAdditionalPaymentSuccessData() {
            return PaymentDetails.builder()
                .status(SUCCESS)
                .customerReference(null)
                .reference("123445")
                .errorCode(null)
                .errorMessage(null)
                .build();
        }
    }
}
