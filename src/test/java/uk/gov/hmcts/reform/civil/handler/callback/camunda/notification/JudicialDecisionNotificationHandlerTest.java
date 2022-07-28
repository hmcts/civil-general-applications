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
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.NotificationService;
import uk.gov.hmcts.reform.civil.service.SolicitorEmailValidation;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION;
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
    private SolicitorEmailValidation solicitorEmailValidation;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @MockBean
    private NotificationService notificationService;

    private static final Long CASE_REFERENCE = 111111L;
    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";
    private static final String DUMMY_DATE = "2022-11-12";
    private static final String CASE_EVENT = "START_NOTIFICATION_PROCESS_MAKE_DECISION";
    private static final String ORG_ID = "1";
    private static final String ID = "1";
    private static final String SAMPLE_TEMPLATE = "general-application-apps-judicial-notification-template-id";
    private static final String JUDGES_DECISION = "JUDGE_MAKES_DECISION";

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
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForConcurrentWrittenOption())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();
            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForConcurrentWrittenOption());

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendThriceForSequentialWrittenRepsWhenInvoked() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForSequentialWrittenOption())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForSequentialWrittenOption());

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToGetReliefFromSanctions(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendForConcurrentWrittenRepsWhenInvoked() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForConcurrentWrittenRepRespondentNotPresent())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForConcurrentWrittenRepRespondentNotPresent());

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendForDismissal() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudgeDismissal(NO, YES))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudgeDismissal(NO, YES));

            handler.handle(params);

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationUncloakShouldSendForDismissal() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudgeDismissal(NO, NO))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudgeDismissal(NO, NO));

            handler.handle(params);

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendListForHearing() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataListForHearing())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataListForHearing());

            handler.handle(params);

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToExtendTime(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfApplicationUncloaked() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForApplicationUncloaked())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForApplicationUncloaked());

            handler.handle(params);

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgement(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfApplicationUncloakedForApproveOrEdit() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForApplicationUncloakedJudgeApproveOrEdit())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForApplicationUncloakedJudgeApproveOrEdit());

            handler.handle(params);

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStayTheClaim(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfApplicationIsToAmendStatementOfCase() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForAmendStatementOfClaim())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForAmendStatementOfClaim());

            handler.handle(params);

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfJudicialApproval() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudicialApprovalOfApplication(NO, YES))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialApprovalOfApplication(NO, YES));

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationUncloakShouldSendIfJudicialApproval() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudicialApprovalOfApplication(NO, NO))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialApprovalOfApplication(NO, NO));

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfJudicialDirectionOrder() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudicialDirectionOrderOfApplication(NO, NO))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialDirectionOrderOfApplication(NO, NO));

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationUncloakShouldSendIfJudicialDirectionOrder() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudicialDirectionOrderOfApplication(YES, NO))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialDirectionOrderOfApplication(YES, NO));

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfJudicialDirectionOrderRepArePresentInList() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudicialDirectionOrderOfApplicationWhenRespondentsArePresentInList(NO, YES))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialDirectionOrderOfApplicationWhenRespondentsArePresentInList(NO, YES));

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationUncloakShouldSendIfJudicialDirectionOrderRepArePresentInList() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudicialDirectionOrderOfApplicationWhenRespondentsArePresentInList(NO, NO))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialDirectionOrderOfApplicationWhenRespondentsArePresentInList(NO, NO));

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToAmendStatementOfCase(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfJudicialRequestForInformationWithouNotice() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudicialRequestForInformationWithoutNoticeOfApplication())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialRequestForInformationWithoutNoticeOfApplication());

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStayTheClaim(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfJudicialRequestForInformationWithNotice() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudicialRequestForInformationWithNoticeOfApplication())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialRequestForInformationWithNoticeOfApplication());

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStayTheClaim(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfJudicialRequestForInformationRepArePresentInList() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudicialRequestForInformationOfApplicationWhenRespondentsArePresentInList())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudicialRequestForInformationOfApplicationWhenRespondentsArePresentInList());

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStayTheClaim(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfApplicationUncloakedDismissed() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForApplicationUncloakedIsDismissed())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForApplicationUncloakedIsDismissed());

            handler.handle(params);

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendIfSequentialWrittenRepsArePresentInList() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForSequentialWrittenRepInList())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForSequentialWrittenRepInList());

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendForApplicationsApprovedWhenRespondentsAreInList() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForApplicationsApprovedWhenRespondentsAreInList(NO, YES))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForApplicationsApprovedWhenRespondentsAreInList(NO, YES));

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendApproveDamagesWhenRespondentsAreInList() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(NO, YES, "UNSPEC_CLAIM"))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();
            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendApproveOcmcWhenRespondentsAreInList() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(NO, YES, "SPEC_CLAIM"))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();
            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationUncloakShouldSendForApplicationApprovedWhenRespondentsAreInList() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForApplicationsApprovedWhenRespondentsAreInList(NO, NO))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForApplicationsApprovedWhenRespondentsAreInList(NO, NO));

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationUncloakShouldSendForApprovedDamageWhenRespondentsAreInList() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(NO, NO, "UNSPEC_CLAIM"))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();
            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationUncloakShouldSendForApprovedOcmcWhenRespondentsAreInList() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(NO, NO, "SPEC_CLAIM"))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();
            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendForApplicationListForHearingWhenRespondentsAreAvailableInList() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForListForHearingRespondentsAreInList())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForListForHearingRespondentsAreInList());

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendWhenApplicationIsDismissedByJudgeWhenRespondentsAreAvailableInList() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForCaseDismissedByJudgeRespondentsAreInList(NO, YES))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForCaseDismissedByJudgeRespondentsAreInList(NO, YES));

            handler.handle(params);

            verify(notificationService, times(3)).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendWhenJudgeApprovesOrderApplicationIsCloak() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudgeApprovedOrderCloakWhenRespondentsArePresentInList())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudgeApprovedOrderCloakWhenRespondentsArePresentInList());

            handler.handle(params);

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesSummeryJudgementConcurrent(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendWhenJudgeApprovesOrderDamageApplicationIsCloak() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForApprovedCloakStrikeOutWhenRespondentsArePresentInList(
                                  "UNSPEC_CLAIM"))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();
            handler.handle(params);

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendWhenJudgeApprovesOrderOcmcApplicationIsCloak() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForApprovedCloakStrikeOutWhenRespondentsArePresentInList("SPEC_CLAIM"))
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();
            handler.handle(params);

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-apps-judicial-notification-template-id",
                notificationPropertiesToStrikeOut(),
                "general-apps-judicial-notification-make-decision-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendWhenJudgeDismissedTheApplicationIsCloak() {
            CallbackParams params = CallbackParamsBuilder
                .builder().of(ABOUT_TO_SUBMIT,
                              caseDataForJudgeDismissTheApplicationCloakWhenRespondentsArePresentInList())
                .request(CallbackRequest.builder().eventId(CASE_EVENT).build()).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any()))
                .thenReturn(caseDataForJudgeDismissTheApplicationCloakWhenRespondentsArePresentInList());

            handler.handle(params);

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

        private CaseData caseDataForJudgeDismissal(YesOrNo orderAgreement, YesOrNo isWithNotice) {
            return CaseData.builder()
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(orderAgreement).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isWithNotice).build())
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION).build())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeToStrikeOut()).build())
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

        private CaseData caseDataForApplicationUncloaked() {
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

        private CaseData caseDataForApplicationUncloakedJudgeApproveOrEdit() {
            return CaseData.builder()
                .applicationIsCloaked(YES)
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT).build())
                .judicialDecision(GAJudicialDecision.builder()
                                      .decision(GAJudgeDecisionOption.MAKE_AN_ORDER).build())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeToStayTheClaim()).build())
                .build();
        }

        private CaseData caseDataForApplicationUncloakedIsDismissed() {
            return CaseData.builder()
                .applicationIsCloaked(YES)
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
                .build();
        }

        private CaseData caseDataForJudicialDirectionOrderOfApplicationWhenRespondentsArePresentInList(
            YesOrNo orderAgreement,
            YesOrNo isWithNotice) {
            return CaseData.builder()
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(orderAgreement).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isWithNotice).build())
                .generalAppRespondentSolicitors(respondentSolicitors())
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
                .build();
        }

        private CaseData caseDataForJudicialRequestForInformationWithoutNoticeOfApplication() {
            return CaseData.builder()
                .judicialDecision(GAJudicialDecision.builder()
                                      .decision(GAJudgeDecisionOption.REQUEST_MORE_INFO).build())
                .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                     .requestMoreInfoOption(REQUEST_MORE_INFORMATION)
                                                     .judgeRequestMoreInfoText("Test")
                                                     .judgeRequestMoreInfoByDate(LocalDate.now()).build())
                .generalAppRespondentSolicitors(respondentSolicitors())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeToStayTheClaim()).build())
                .build();
        }

        private CaseData caseDataForJudicialRequestForInformationWithNoticeOfApplication() {
            return CaseData.builder()
                .judicialDecision(GAJudicialDecision.builder()
                                      .decision(GAJudgeDecisionOption.REQUEST_MORE_INFO).build())
                .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                     .judgeRequestMoreInfoText("Test")
                                                     .judgeRequestMoreInfoByDate(LocalDate.now()).build())
                .generalAppRespondentSolicitors(respondentSolicitors())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeToStayTheClaim()).build())
                .build();
        }

        private CaseData caseDataForJudicialRequestForInformationOfApplicationWhenRespondentsArePresentInList() {
            return CaseData.builder()
                .generalAppRespondentSolicitors(respondentSolicitors())
                .judicialDecision(GAJudicialDecision.builder()
                                      .decision(GAJudgeDecisionOption.REQUEST_MORE_INFO).build())
                .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                     .requestMoreInfoOption(REQUEST_MORE_INFORMATION)
                                                     .judgeRequestMoreInfoText("Test")
                                                     .judgeRequestMoreInfoByDate(LocalDate.now()).build())
                .generalAppRespondentSolicitors(respondentSolicitors())
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .email(DUMMY_EMAIL).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .generalAppParentCaseLink(GeneralAppParentCaseLink.builder()
                                              .caseReference(CASE_REFERENCE.toString()).build())
                .generalAppType(GAApplicationType.builder()
                                    .types(applicationTypeToStayTheClaim()).build())
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
                    .build();
        }

        private CaseData caseDataForApplicationsApprovedStrikeOutWhenRespondentsAreInList(
            YesOrNo orderAgreement,
            YesOrNo isWithNotice, String superClaimType) {
            return
                CaseData.builder()
                    .generalAppRespondentSolicitors(respondentSolicitors())
                    .generalAppSuperClaimType(superClaimType)
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
                                        .types(applicationTypeToStrikeOut()).build())
                    .judicialConcurrentDateText(DUMMY_DATE)
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
                                                                             YesOrNo isWithNotice) {
            return
                CaseData.builder()
                    .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                                       .hasAgreed(orderAgreement).build())
                    .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isWithNotice).build())
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
    }
}
