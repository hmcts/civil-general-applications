package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
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
        }

        @Test
        void notificationShouldSendWhenInvoked() {
            CaseData caseData = getCaseData(true);

            when(solicitorEmailValidation
                     .validateApplicantSolicitorEmail(any(), any()))
                .thenReturn(caseData);
            gaNotificationService.sendNotification(caseData);
            verify(notificationService, times(2)).sendMail(
                DUMMY_EMAIL,
                "general-application-respondent-template-id",
                getNotificationDataMap(),
                "general-application-respondent-notification-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendWhenInvokedTwice() {
            CaseData caseData = getCaseData(true);

            when(solicitorEmailValidation
                     .validateApplicantSolicitorEmail(any(), any()))
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
                     .validateApplicantSolicitorEmail(any(), any()))
                .thenReturn(caseData);

            gaNotificationService.sendNotification(caseData);
            verifyNoInteractions(notificationService);
        }

        private Map<String, String> getNotificationDataMap() {
            return Map.of(
                NotificationData.APPLICANT_REFERENCE, "claimant",
                NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                NotificationData.GA_NOTIFICATION_DEADLINE,
                NOTIFICATION_DEADLINE.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
            );
        }

        private CaseData getCaseData(boolean isMet) {

            List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

            GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
                .email(DUMMY_EMAIL).organisationIdentifier("2").build();

            GASolicitorDetailsGAspec respondent2 = GASolicitorDetailsGAspec.builder().id("id")
                .email(DUMMY_EMAIL).organisationIdentifier("3").build();

            respondentSols.add(element(respondent1));
            respondentSols.add(element(respondent2));

            if (isMet) {

                return new CaseDataBuilder()
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
