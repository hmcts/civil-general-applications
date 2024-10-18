package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.enums.PaymentStatus;
import uk.gov.hmcts.reform.civil.enums.dq.Language;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.citizenui.RespondentLiPResponse;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@SpringBootTest(classes = {
    GeneralApplicationCreationNotificationService.class,
    GaForLipService.class,
    JacksonAutoConfiguration.class
})
public class GeneralApplicationCreationNotificationServiceTest {

    @Autowired
    private GeneralApplicationCreationNotificationService gaNotificationService;
    @MockBean
    private SolicitorEmailValidation solicitorEmailValidation;
    @MockBean
    private NotificationService notificationService;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @MockBean
    private NotificationsProperties notificationsProperties;
    @MockBean
    private FeatureToggleService featureToggleService;
    @Captor
    ArgumentCaptor<Map<String, String>> argumentCaptor;

    private static final Long CASE_REFERENCE = 111111L;
    private static final String PROCESS_INSTANCE_ID = "1";
    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";
    private static final LocalDateTime DUMMY_DATE = LocalDateTime.of(2022, 02, 15, 12, 00, 00);
    public static LocalDateTime NOTIFICATION_DEADLINE = LocalDateTime.of(2022, 02, 15, 12, 00, 00);

    @Nested
    class AboutToSubmitCallback {

        @BeforeEach
        void setup() {
            when(notificationsProperties.getGeneralApplicationRespondentEmailTemplate())
                .thenReturn("general-application-respondent-template-id");
            when(notificationsProperties.getUrgentGeneralAppRespondentEmailTemplate())
                .thenReturn("general-application-respondent-template-id");
            when(notificationsProperties.getLipGeneralAppRespondentEmailTemplate())
                    .thenReturn("general-application-respondent-template-lip-id");
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            when(notificationsProperties.getLipGeneralAppRespondentEmailTemplateInWelsh())
                .thenReturn("general-application-respondent-welsh-template-lip-id");
        }

        @Test
        void notificationShouldSendIfGa_Urgent_WithNoticeAndFreeFee() {
            CaseData caseData = getCaseData(true).toBuilder()
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(YES).build())
                .generalAppPBADetails(GAPbaDetails.builder().fee(Fee.builder().code("FREE").build()).build())
                .build();

            when(solicitorEmailValidation
                     .validateSolicitorEmail(any(), any()))
                .thenReturn(caseData);
            gaNotificationService.sendNotification(caseData);
            verify(notificationService, times(2)).sendMail(
                any(), any(), any(), any()
            );
        }

        @Test
        void notificationShouldSendIfGa_Urgent_WithNoticeAndFeePaid() {
            CaseData caseData = getCaseData(true).toBuilder()
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(YES).build())
                .generalAppPBADetails(GAPbaDetails.builder()
                                          .fee(Fee.builder().code("PAID").build())
                                          .paymentDetails(PaymentDetails.builder().status(
                    PaymentStatus.SUCCESS).build()).build())
                .build();

            when(solicitorEmailValidation
                     .validateSolicitorEmail(any(), any()))
                .thenReturn(caseData);
            gaNotificationService.sendNotification(caseData);
            verify(notificationService, times(2)).sendMail(
                any(), any(), any(), any()
            );
        }

        @Test
        void notificationShouldNotSendIfGa_NonUrgent_WithNoticeAndFreeFee() {
            CaseData caseData = getCaseData(false).toBuilder()
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(NO).build())
                .generalAppPBADetails(GAPbaDetails.builder().fee(Fee.builder().code("FREE").build()).build())
                .build();

            when(solicitorEmailValidation
                     .validateSolicitorEmail(any(), any()))
                .thenReturn(caseData);
            gaNotificationService.sendNotification(caseData);
            verifyNoInteractions(notificationService);
        }

        @Test
        void notificationShouldNotSendIfGa_NonUrgent_WithNoticeAndFeePaid() {
            CaseData caseData = getCaseData(false).toBuilder()
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(NO).build())
                .generalAppPBADetails(GAPbaDetails.builder()
                                          .fee(Fee.builder().code("PAID").build())
                                          .paymentDetails(PaymentDetails.builder().status(
                                              PaymentStatus.SUCCESS).build()).build())
                .build();

            when(solicitorEmailValidation
                     .validateSolicitorEmail(any(), any()))
                .thenReturn(caseData);
            gaNotificationService.sendNotification(caseData);
            verifyNoInteractions(notificationService);
        }

        @Test
        void notificationShouldSendIfGa_Lip_WithNoticeAndFeePaid() {
            CaseData caseData = getCaseData(true).toBuilder()
                    .isGaRespondentOneLip(YES)
                    .generalAppPBADetails(GAPbaDetails.builder()
                            .fee(Fee.builder().code("PAID").build())
                            .paymentDetails(PaymentDetails.builder().status(
                                    PaymentStatus.SUCCESS).build()).build())
                    .build();

            when(solicitorEmailValidation
                    .validateSolicitorEmail(any(), any()))
                    .thenReturn(caseData);
            when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
            gaNotificationService.sendNotification(caseData);
            verify(notificationService, times(2)).sendMail(
                    any(), eq("general-application-respondent-template-lip-id"), argumentCaptor.capture(), any()
            );
            assertThat(argumentCaptor.getValue().get("respondentName")).isEqualTo("DEF");
            assertThat(argumentCaptor.getValue().get("ClaimantvDefendant")).isEqualTo("CL v DEF");
        }

        @Test
        void notificationRespondentInWelshShouldSendIfGa_Lip_WithNoticeAndFeePaid() {
            CaseData caseData = getCaseData(true).toBuilder()
                .isGaRespondentOneLip(YES)
                .generalAppPBADetails(GAPbaDetails.builder()
                                          .fee(Fee.builder().code("PAID").build())
                                          .paymentDetails(PaymentDetails.builder().status(
                                              PaymentStatus.SUCCESS).build()).build())
                .build();

            when(solicitorEmailValidation
                     .validateSolicitorEmail(any(), any()))
                .thenReturn(caseData);
            CaseData claimRespondentResponseLan = CaseData.builder().respondent1LiPResponse(RespondentLiPResponse.builder().respondent1ResponseLanguage(
                Language.BOTH.toString()).build()).build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(claimRespondentResponseLan);
            gaNotificationService.sendNotification(caseData);
            verify(notificationService, times(2)).sendMail(
                any(), eq("general-application-respondent-welsh-template-lip-id"), argumentCaptor.capture(), any()
            );
            assertThat(argumentCaptor.getValue().get("respondentName")).isEqualTo("DEF");
            assertThat(argumentCaptor.getValue().get("ClaimantvDefendant")).isEqualTo("CL v DEF");
        }

        @Test
        void notificationShouldSendWhenInvoked() {
            CaseData caseData = getCaseData(true);

            when(solicitorEmailValidation
                     .validateSolicitorEmail(any(), any()))
                .thenReturn(caseData);
            gaNotificationService.sendNotification(caseData);
            verify(notificationService, times(2)).sendMail(
                DUMMY_EMAIL,
                "general-application-respondent-template-id",
                getNotificationDataMap(false),
                "general-application-respondent-notification-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendWhenInvokedTwice() {
            CaseData caseData = getCaseData(true);

            when(solicitorEmailValidation
                     .validateSolicitorEmail(any(), any()))
                .thenReturn(caseData);

            gaNotificationService.sendNotification(caseData);
            verify(notificationService, times(2)).sendMail(
                any(),
                any(),
                any(),
                any()
            );
        }

        @Test
        void notificationShouldNotSendWhenInvokedWhenConditionsAreNotMet() {
            CaseData caseData = getCaseData(false);

            when(solicitorEmailValidation
                     .validateSolicitorEmail(any(), any()))
                .thenReturn(caseData);

            gaNotificationService.sendNotification(caseData);
            verifyNoInteractions(notificationService);
        }

        private Map<String, String> getNotificationDataMap(boolean isLip) {
            return Map.of(
                NotificationData.APPLICANT_REFERENCE, "claimant",
                NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                NotificationData.GA_NOTIFICATION_DEADLINE,
                NOTIFICATION_DEADLINE.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                NotificationData.GA_LIP_RESP_NAME, isLip ? "Lip Resp" : "",
                NotificationData.CASE_TITLE, isLip ? "CL v DEF" : ""
            );
        }

        private CaseData getCaseData(boolean isMet) {

            List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

            GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
                .email(DUMMY_EMAIL).organisationIdentifier("2").forename("LipF").surname(Optional.of("LipS")).build();

            GASolicitorDetailsGAspec respondent2 = GASolicitorDetailsGAspec.builder().id("id")
                .email(DUMMY_EMAIL).organisationIdentifier("3").build();

            respondentSols.add(element(respondent1));
            respondentSols.add(element(respondent2));

            if (isMet) {

                return new CaseDataBuilder()
                    .applicantPartyName("App")
                    .claimant1PartyName("CL")
                    .defendant1PartyName("DEF")
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id")
                                                  .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                    .generalAppRespondentSolicitors(respondentSols)
                    .businessProcess(BusinessProcess.builder().status(STARTED)
                                         .processInstanceId(PROCESS_INSTANCE_ID).build())
                    .gaInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
                    .gaUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(NO).build())
                    .parentClaimantIsApplicant(YES)
                    .gaRespondentOrderAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                    .respondentSolicitor1EmailAddress(DUMMY_EMAIL)
                    .respondentSolicitor2EmailAddress(DUMMY_EMAIL)
                    .applicant1OrganisationPolicy(OrganisationPolicy.builder()
                                                      .organisation(Organisation.builder().organisationID("1").build())
                                                      .build())
                    .respondent1OrganisationPolicy(OrganisationPolicy.builder()
                                                       .organisation(Organisation.builder().organisationID("2").build())
                                                       .build())
                    .respondent2OrganisationPolicy(OrganisationPolicy.builder()
                                                       .organisation(Organisation.builder().organisationID("3").build())
                                                       .build())
                    .generalAppParentCaseLink(
                        GeneralAppParentCaseLink
                            .builder()
                            .caseReference(CASE_REFERENCE.toString())
                            .build())
                    .generalAppDeadlineNotificationDate(DUMMY_DATE)
                    .build();
            } else {
                return new CaseDataBuilder()
                    .applicantPartyName("App")
                    .claimant1PartyName("CL")
                    .defendant1PartyName("DEF")
                    .businessProcess(BusinessProcess.builder().status(STARTED)
                                         .processInstanceId(PROCESS_INSTANCE_ID).build())
                    .gaInformOtherParty(GAInformOtherParty.builder().isWithNotice(NO).build())
                    .gaUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(NO).build())
                    .gaRespondentOrderAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                    .ccdCaseReference(CASE_REFERENCE)
                    .respondentSolicitor1EmailAddress(DUMMY_EMAIL)
                    .respondentSolicitor2EmailAddress(DUMMY_EMAIL)
                    .applicant1OrganisationPolicy(OrganisationPolicy.builder()
                                                      .organisation(Organisation.builder().organisationID("1").build())
                                                      .build())
                    .respondent1OrganisationPolicy(OrganisationPolicy.builder()
                                                       .organisation(Organisation.builder().organisationID("2").build())
                                                       .build())
                    .respondent2OrganisationPolicy(OrganisationPolicy.builder()
                                                       .organisation(Organisation.builder().organisationID("3").build())
                                                       .build())
                    .generalAppParentCaseLink(
                        GeneralAppParentCaseLink
                            .builder()
                            .caseReference(CASE_REFERENCE.toString())
                            .build())
                    .generalAppDeadlineNotificationDate(DUMMY_DATE)
                    .build();
            }
        }
    }
}
