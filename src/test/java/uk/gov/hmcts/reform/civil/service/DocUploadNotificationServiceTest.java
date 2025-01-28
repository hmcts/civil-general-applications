package uk.gov.hmcts.reform.civil.service;

import static org.mockito.ArgumentMatchers.any;
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
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.Language;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.RespondentLiPResponse;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @MockBean
    private GaForLipService gaForLipService;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    private static final Long CASE_REFERENCE = 111111L;
    private static final String PROCESS_INSTANCE_ID = "1";
    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";
    private static final String PARTY_REFERENCE = "Claimant Reference: Not provided - Defendant Reference: Not provided";
    private static final String CUSTOM_PARTY_REFERENCE = "Claimant Reference: ABC Ltd - Defendant Reference: Defendant Ltd";

    private final Map<String, String> customProp = new HashMap<>();

    @Nested
    class AboutToSubmitCallback {
        @BeforeEach
        void setup() {
            when(notificationsProperties.getEvidenceUploadTemplate())
                    .thenReturn("general-apps-notice-of-document-template-id");
            when(notificationsProperties.getLipGeneralAppApplicantEmailTemplate())
                .thenReturn("ga-notice-of-document-lip-appln-template-id");
            when(notificationsProperties.getLipGeneralAppApplicantEmailTemplateInWelsh())
                .thenReturn("ga-notice-of-document-lip-appln-welsh-template-id");
            when(notificationsProperties.getLipGeneralAppRespondentEmailTemplate())
                .thenReturn("ga-notice-of-document-lip-respondent-template-id");
            when(notificationsProperties.getLipGeneralAppRespondentEmailTemplateInWelsh())
                .thenReturn("ga-notice-of-document-lip-respondent-welsh-template-id");
        }

        @Test
        void appNotificationShouldSendWhenInvoked() {

            CaseData caseData = getCaseData(true, NO, NO);
            docUploadNotificationService.notifyApplicantEvidenceUpload(caseData);
            verify(notificationService, times(1)).sendMail(
                    DUMMY_EMAIL,
                    "general-apps-notice-of-document-template-id",
                    getNotificationDataMap(false),
                    "general-apps-notice-of-document-upload-" + CASE_REFERENCE
            );
        }

        @Test
        void appNotificationWithSolicitorReferenceAdded() {

            CaseData caseData = getCaseData(false, NO, NO);
            docUploadNotificationService.notifyApplicantEvidenceUpload(caseData);
            verify(notificationService, times(1)).sendMail(
                DUMMY_EMAIL,
                "general-apps-notice-of-document-template-id",
                getNotificationDataMap(true),
                "general-apps-notice-of-document-upload-" + CASE_REFERENCE
            );
        }

        @Test
        void respNotificationShouldSendTwice1V2() {
            CaseData caseData = getCaseData(true, NO, YES);
            docUploadNotificationService.notifyRespondentEvidenceUpload(caseData);
            verify(notificationService, times(2)).sendMail(
                    DUMMY_EMAIL,
                    "general-apps-notice-of-document-template-id",
                    getNotificationDataMap(false),
                    "general-apps-notice-of-document-upload-" + CASE_REFERENCE
            );
        }

        @Test
        void lipApplicantNotificationShouldSendWhenInvoked() {

            when(gaForLipService.isGaForLip(any())).thenReturn(true);
            when(gaForLipService.isLipApp(any())).thenReturn(true);
            CaseData caseData = getCaseData(true, YES, NO);
            when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
            docUploadNotificationService.notifyApplicantEvidenceUpload(caseData);
            verify(notificationService, times(1)).sendMail(
                DUMMY_EMAIL,
                "ga-notice-of-document-lip-appln-template-id",
                getNotificationDataMapForLip(YES, NO),
                "general-apps-notice-of-document-upload-" + CASE_REFERENCE
            );
        }

        @Test
        void lipApplicantNotificationShouldSendWhenInvoked_whenMainClaimIssuedInWelsh() {

            when(gaForLipService.isGaForLip(any())).thenReturn(true);
            when(gaForLipService.isLipApp(any())).thenReturn(true);
            CaseData caseData =
                getCaseData(true, YES, NO).toBuilder().applicantBilingualLanguagePreference(YES).build();
            CaseData claimantClaimIssueFlag = CaseData.builder().applicantBilingualLanguagePreference(YES)
                .claimantBilingualLanguagePreference("WELSH").build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(claimantClaimIssueFlag);
            docUploadNotificationService.notifyApplicantEvidenceUpload(caseData);
            verify(notificationService, times(1)).sendMail(
                DUMMY_EMAIL,
                "ga-notice-of-document-lip-appln-welsh-template-id",
                getNotificationDataMapForLip(YES, NO),
                "general-apps-notice-of-document-upload-" + CASE_REFERENCE
            );
        }

        @Test
        void lipRespondentNotificationShouldSend() {

            when(gaForLipService.isGaForLip(any())).thenReturn(true);
            when(gaForLipService.isLipApp(any())).thenReturn(false);
            when(gaForLipService.isLipResp(any())).thenReturn(true);

            List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();
            GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
                .email(DUMMY_EMAIL).surname(Optional.of("surname")).forename("forename").organisationIdentifier("2").build();
            respondentSols.add(element(respondent1));

            CaseData caseData = getCaseData(true, NO, YES).toBuilder()
                .generalAppRespondentSolicitors(respondentSols).build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());
            docUploadNotificationService.notifyRespondentEvidenceUpload(caseData);
            verify(notificationService, times(1)).sendMail(
                DUMMY_EMAIL,
                "ga-notice-of-document-lip-respondent-template-id",
                getNotificationDataMapForLip(NO, YES),
                "general-apps-notice-of-document-upload-" + CASE_REFERENCE
            );
        }

        @Test
        void lipRespondentNotificationShouldSend_whenRespondentResponseInWelsh() {

            when(gaForLipService.isGaForLip(any())).thenReturn(true);
            when(gaForLipService.isLipApp(any())).thenReturn(false);
            when(gaForLipService.isLipResp(any())).thenReturn(true);

            List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();
            GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
                .email(DUMMY_EMAIL).surname(Optional.of("surname")).forename("forename").organisationIdentifier("2").build();
            respondentSols.add(element(respondent1));

            CaseData caseData = getCaseData(true, NO, YES).toBuilder()
                .generalAppRespondentSolicitors(respondentSols).respondentBilingualLanguagePreference(YES).build();
            CaseData claimantClaimIssueFlag = CaseData.builder().respondentBilingualLanguagePreference(YES)
                .respondent1LiPResponse(RespondentLiPResponse.builder().respondent1ResponseLanguage(
                Language.BOTH.toString()).build()).build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(claimantClaimIssueFlag);
            docUploadNotificationService.notifyRespondentEvidenceUpload(caseData);
            verify(notificationService, times(1)).sendMail(
                DUMMY_EMAIL,
                "ga-notice-of-document-lip-respondent-welsh-template-id",
                getNotificationDataMapForLip(NO, YES),
                "general-apps-notice-of-document-upload-" + CASE_REFERENCE
            );
        }

        private Map<String, String> getNotificationDataMapForLip(YesOrNo isLipAppln, YesOrNo isLipRespondent) {

            customProp.put(NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString());
            customProp.put(NotificationData.GENAPP_REFERENCE, CASE_REFERENCE.toString());
            customProp.put(NotificationData.CASE_TITLE, "CL v DEF");
            customProp.put(NotificationData.PARTY_REFERENCE, PARTY_REFERENCE);

            if (isLipAppln == YES) {
                customProp.put(NotificationData.GA_LIP_APPLICANT_NAME, "App");
            }

            if (isLipRespondent == YES) {
                customProp.put(NotificationData.GA_LIP_RESP_NAME, "DEF");
            }
            return customProp;
        }

        private Map<String, String> getNotificationDataMap(boolean customReferencePresent) {
            if (customReferencePresent) {
                return Map.of(
                    NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                    NotificationData.GENAPP_REFERENCE, CASE_REFERENCE.toString(),
                    NotificationData.PARTY_REFERENCE, CUSTOM_PARTY_REFERENCE
                );
            } else {
                return Map.of(
                    NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                    NotificationData.GENAPP_REFERENCE, CASE_REFERENCE.toString(),
                    NotificationData.PARTY_REFERENCE, PARTY_REFERENCE
                );
            }
        }

        private CaseData getCaseData(boolean isMet, YesOrNo isGaApplicantLip, YesOrNo isGaRespondentOneLip) {

            List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

            GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
                    .email(DUMMY_EMAIL).forename("forename").organisationIdentifier("2").build();

            GASolicitorDetailsGAspec respondent2 = GASolicitorDetailsGAspec.builder().id("id")
                    .email(DUMMY_EMAIL).forename("forename").organisationIdentifier("3").build();

            respondentSols.add(element(respondent1));
            respondentSols.add(element(respondent2));

            if (isMet) {

                return new CaseDataBuilder()
                        .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id")
                                .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                        .generalAppRespondentSolicitors(respondentSols)
                        .generalAppParentCaseLink(GeneralAppParentCaseLink.builder().caseReference("1").build())
                        .applicantPartyName("App")
                        .claimant1PartyName("CL")
                        .defendant1PartyName("DEF")
                        .isGaRespondentOneLip(isGaRespondentOneLip)
                        .isGaApplicantLip(isGaApplicantLip)
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
                        .emailPartyReference("Claimant Reference: ABC Ltd - Defendant Reference: Defendant Ltd")
                        .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id")
                                                      .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                        .generalAppRespondentSolicitors(respondentSols)
                        .generalAppParentCaseLink(GeneralAppParentCaseLink.builder().caseReference("1").build())
                        .applicantPartyName("App")
                        .claimant1PartyName("CL")
                        .defendant1PartyName("DEF")
                        .isGaRespondentOneLip(isGaRespondentOneLip)
                        .isGaApplicantLip(isGaApplicantLip)
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
            }
        }
    }
}
