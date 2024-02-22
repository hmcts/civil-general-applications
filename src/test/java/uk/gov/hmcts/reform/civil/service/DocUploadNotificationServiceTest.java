package uk.gov.hmcts.reform.civil.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {
    DocUploadNotificationService.class,
    JacksonAutoConfiguration.class
})
public class DocUploadNotificationServiceTest {

    @Autowired
    private DocUploadNotificationService docUploadNotificationService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationsProperties notificationsProperties;

    private static final Long CASE_REFERENCE = 111111L;
    private static final String PROCESS_INSTANCE_ID = "1";
    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";

    @Nested
    class AboutToSubmitCallback {
        @BeforeEach
        void setup() {
            when(notificationsProperties.getEvidenceUploadTemplate())
                    .thenReturn("general-apps-notice-of-document-template-id");
        }

        @Test
        void appNotificationShouldSendWhenInvoked() {
            CaseData caseData = getCaseData(true);
            docUploadNotificationService.notifyApplicantEvidenceUpload(caseData);
            verify(notificationService, times(1)).sendMail(
                    DUMMY_EMAIL,
                    "general-apps-notice-of-document-template-id",
                    getNotificationDataMap(),
                    "general-apps-notice-of-document-upload-" + CASE_REFERENCE
            );
        }

        @Test
        void respNotificationShouldSendTwice1V2() {
            CaseData caseData = getCaseData(true);
            docUploadNotificationService.notifyRespondentEvidenceUpload(caseData);
            verify(notificationService, times(2)).sendMail(
                    DUMMY_EMAIL,
                    "general-apps-notice-of-document-template-id",
                    getNotificationDataMap(),
                    "general-apps-notice-of-document-upload-" + CASE_REFERENCE
            );
        }

        private Map<String, String> getNotificationDataMap() {
            return Map.of(
                    NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString()
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
                        .ccdCaseReference(CASE_REFERENCE)
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
                        .build();
            }
        }
    }
}
