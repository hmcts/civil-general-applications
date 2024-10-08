package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.enums.dq.Language;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.OrganisationResponse;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.GaForLipService;
import uk.gov.hmcts.reform.civil.service.NotificationService;
import uk.gov.hmcts.reform.civil.service.OrganisationService;
import uk.gov.hmcts.reform.civil.service.SolicitorEmailValidation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@ExtendWith(MockitoExtension.class)
public class TranslatedDocumentUploadedApplicantNotificationHandlerTest extends BaseCallbackHandlerTest {

    @InjectMocks
    private TranslatedDocumentUploadedApplicantNotificationHandler handler;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationsProperties notificationsProperties;
    @Mock
    private GaForLipService gaForLipService;
    @Mock
    private OrganisationService organisationService;
    @Mock
    private CaseDetailsConverter caseDetailsConverter;
    @Mock
    private CoreCaseDataService coreCaseDataService;
    @Mock
    private SolicitorEmailValidation solicitorEmailValidation;

    @Nested
    class AboutToSubmitCallback {

        @Test
        void shouldSendNotificationLiPApplicantConsent_WhenParentCaseInEnglish() {
            // Given
            CaseData caseData =
                CaseData.builder()
                    .applicantPartyName("applicant1")
                    .defendant1PartyName("respondent1")
                    .generalAppRespondentSolicitors(List.of(
                        Element.<GASolicitorDetailsGAspec>builder()
                            .value(GASolicitorDetailsGAspec.builder().email("respondent@gmail.com").build()).build()))
                    .generalAppParentCaseLink(GeneralAppParentCaseLink.builder().caseReference("1234567").build())
                    .generalAppConsentOrder(YES)
                    .ccdCaseReference(Long.valueOf("56789"))
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().email("applicant@gmail.com").build())
                    .isGaApplicantLip(YES)
                    .build();
            CaseDetails civil = CaseDetails.builder().id(123L).data(Map.of("case_data", caseData)).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
            when(coreCaseDataService.getCase(any())).thenReturn(civil);
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            when(gaForLipService.isLipApp(caseData)).thenReturn(true);
            when(notificationsProperties.getLipGeneralAppApplicantEmailTemplate()).thenReturn("template-id");
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(CaseEvent.NOTIFY_RESPONDENT_TRANSLATED_DOCUMENT_UPLOADED_GA.name())
                    .build()).build();
            ;
            // When
            handler.handle(params);

            // Then
            verify(notificationService).sendMail(
                eq("applicant@gmail.com"),
                eq("template-id"),
                anyMap(),
                anyString()
            );
        }

        @Test
        void shouldSendNotificationLiPApplicantConsent_WhenParentCaseInWelsh() {
            // Given
            CaseData caseData =
                CaseData.builder()
                    .applicantPartyName("applicant1")
                    .defendant1PartyName("respondent1")
                    .generalAppRespondentSolicitors(List.of(
                        Element.<GASolicitorDetailsGAspec>builder()
                            .value(GASolicitorDetailsGAspec.builder().email("respondent@gmail.com").build()).build()))
                    .generalAppParentCaseLink(GeneralAppParentCaseLink.builder().caseReference("1234567").build())
                    .generalAppConsentOrder(YES)
                    .ccdCaseReference(Long.valueOf("56789"))
                    .isGaApplicantLip(YES)
                    .parentClaimantIsApplicant(YES)
                    .applicantBilingualLanguagePreference(YES)
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().email("applicant@gmail.com").build())
                    .build();
            CaseDetails civil = CaseDetails.builder().id(123L).data(Map.of("case_data", caseData)).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
            when(coreCaseDataService.getCase(any())).thenReturn(civil);
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            when(gaForLipService.isLipApp(caseData)).thenReturn(true);
            when(notificationsProperties.getNotifyApplicantLiPTranslatedDocumentUploadedWhenParentCaseInBilingual()).thenReturn(
                "template-id");
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(CaseEvent.NOTIFY_RESPONDENT_TRANSLATED_DOCUMENT_UPLOADED_GA.name())
                    .build()).build();
            ;
            // When
            handler.handle(params);

            // Then
            verify(notificationService).sendMail(
                eq("applicant@gmail.com"),
                eq("template-id"),
                anyMap(),
                anyString()
            );
        }

        @Test
        void shouldSendNotificationApplicantConsentForLR() {
            // Given
            CaseData caseData =
                CaseData.builder()
                    .applicantPartyName("applicant1")
                    .defendant1PartyName("respondent1")
                    .generalAppRespondentSolicitors(List.of(
                        Element.<GASolicitorDetailsGAspec>builder()
                            .value(GASolicitorDetailsGAspec.builder().email("respondent@gmail.com").build()).build()))
                    .generalAppParentCaseLink(GeneralAppParentCaseLink.builder().caseReference("1234567").build())
                    .generalAppConsentOrder(YES)
                    .ccdCaseReference(Long.valueOf("56789"))
                    .isGaApplicantLip(NO)
                    .parentClaimantIsApplicant(YES)
                    .respondentBilingualLanguagePreference(YES)
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().email("applicant@gmail.com").build())
                    .build();
            CaseDetails civil = CaseDetails.builder().id(123L).data(Map.of("case_data", caseData)).build();

            when(solicitorEmailValidation.validateSolicitorEmail(any(), any())).thenReturn(caseData);
            when(coreCaseDataService.getCase(any())).thenReturn(civil);
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            when(gaForLipService.isLipApp(caseData)).thenReturn(false);
            when(organisationService.findOrganisationById(any())).thenReturn(Optional.of(OrganisationResponse.builder()
                                                                                             .name("LegalRep")
                                                                                             .build()));
            when(notificationsProperties.getNotifyLRTranslatedDocumentUploaded()).thenReturn(
                "template-id");
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(CaseEvent.NOTIFY_RESPONDENT_TRANSLATED_DOCUMENT_UPLOADED_GA.name())
                    .build()).build();
            ;
            // When
            handler.handle(params);

            // Then
            verify(notificationService).sendMail(
                eq("applicant@gmail.com"),
                eq("template-id"),
                anyMap(),
                anyString()
            );
        }
    }
}
