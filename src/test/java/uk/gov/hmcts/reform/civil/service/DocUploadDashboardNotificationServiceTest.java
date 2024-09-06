package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.UploadDocumentByType;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.dashboard.data.ScenarioRequestParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_OTHER_PARTY_UPLOADED_DOC_APPLICANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_OTHER_PARTY_UPLOADED_DOC_RESPONDENT;
import static uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder.STRING_CONSTANT;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@ExtendWith(MockitoExtension.class)
public class DocUploadDashboardNotificationServiceTest {

    @InjectMocks
    private DocUploadDashboardNotificationService docUploadDashboardNotificationService;

    @Mock
    DashboardApiClient dashboardApiClient;
    @Mock
    FeatureToggleService featureToggleService;
    @Mock
    GaForLipService gaForLipService;
    @Mock
    DashboardNotificationsParamsMapper mapper;

    private static final String DUMMY_EMAIL = "test@gmail.com";

    @Nested
    class AboutToSubmitCallback {

        @Test
        void shouldCreateDashboardNotificationWhenLipApplicantUploadDoc() {

            List<Element<UploadDocumentByType>> uploadDocumentByApplicant = new ArrayList<>();
            uploadDocumentByApplicant.add(element(UploadDocumentByType.builder()
                                                      .documentType("Witness")
                                                      .additionalDocument(Document.builder()
                                                                              .documentFileName("witness_document.pdf")
                                                                              .documentUrl("http://dm-store:8080")
                                                                              .documentBinaryUrl(
                                                                                  "http://dm-store:8080/documents")
                                                                              .build()).build()));
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(YesOrNo.NO)
                .applicationIsUncloakedOnce(YES)
                .parentClaimantIsApplicant(YES)
                .uploadDocument(uploadDocumentByApplicant)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .generalAppConsentOrder(YES)
                .isGaApplicantLip(YES)
                .isGaRespondentOneLip(YES)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id(STRING_CONSTANT).forename(
                        "GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())

                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();
            when(gaForLipService.isGaForLip(any(CaseData.class))).thenReturn(true);
            when(gaForLipService.isLipResp(any(CaseData.class))).thenReturn(true);
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            docUploadDashboardNotificationService.createDashboardNotification(caseData, "Applicant", "BEARER_TOKEN");

            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_OTHER_PARTY_UPLOADED_DOC_RESPONDENT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

        @Test
        void shouldCreateDashboardNotificationWhenLipRespondentUploadDoc() {

            List<Element<UploadDocumentByType>> uploadDocumentByApplicant = new ArrayList<>();
            uploadDocumentByApplicant.add(element(UploadDocumentByType.builder()
                                                      .documentType("Witness")
                                                      .additionalDocument(Document.builder()
                                                                              .documentFileName("witness_document.pdf")
                                                                              .documentUrl("http://dm-store:8080")
                                                                              .documentBinaryUrl(
                                                                                  "http://dm-store:8080/documents")
                                                                              .build()).build()));
            List<Element<GASolicitorDetailsGAspec>> gaRespSolicitors = new ArrayList<>();
            gaRespSolicitors.add(element(GASolicitorDetailsGAspec.builder()
                                             .id(STRING_CONSTANT)
                                             .email(DUMMY_EMAIL)
                                             .organisationIdentifier("2").build()));

            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(YesOrNo.NO)
                .applicationIsUncloakedOnce(YES)
                .parentClaimantIsApplicant(YES)
                .uploadDocument(uploadDocumentByApplicant)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .generalAppConsentOrder(YES)
                .isGaApplicantLip(YES)
                .isGaRespondentOneLip(YES)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("123456789").forename("GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())
                .generalAppRespondentSolicitors(gaRespSolicitors)

                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();
            when(gaForLipService.isGaForLip(any(CaseData.class))).thenReturn(true);
            when(gaForLipService.isLipApp(any(CaseData.class))).thenReturn(true);
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            docUploadDashboardNotificationService.createDashboardNotification(
                caseData,
                "Respondent One",
                "BEARER_TOKEN"
            );

            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_OTHER_PARTY_UPLOADED_DOC_APPLICANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

        @Test
        void shouldNotCreateDashboardNotificationWhenGaFlagIsDisabled() {

            List<Element<UploadDocumentByType>> uploadDocumentByApplicant = new ArrayList<>();
            uploadDocumentByApplicant.add(element(UploadDocumentByType.builder()
                                                      .documentType("Witness")
                                                      .additionalDocument(Document.builder()
                                                                              .documentFileName("witness_document.pdf")
                                                                              .documentUrl("http://dm-store:8080")
                                                                              .documentBinaryUrl(
                                                                                  "http://dm-store:8080/documents")
                                                                              .build()).build()));
            CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .ccdCaseReference(1678356749555475L)
                .build().toBuilder()
                .respondent2SameLegalRepresentative(YesOrNo.NO)
                .applicationIsUncloakedOnce(YES)
                .parentClaimantIsApplicant(YES)
                .uploadDocument(uploadDocumentByApplicant)
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .generalAppConsentOrder(YES)
                .isGaApplicantLip(YES)
                .isGaRespondentOneLip(YES)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id(STRING_CONSTANT).forename(
                        "GAApplnSolicitor")
                                              .email(DUMMY_EMAIL).organisationIdentifier("1").build())

                .build();
            when(gaForLipService.isGaForLip(any(CaseData.class))).thenReturn(false);
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);

            docUploadDashboardNotificationService.createDashboardNotification(
                caseData,
                "Respondent One",
                "BEARER_TOKEN"
            );

            verifyNoInteractions(dashboardApiClient);
        }

    }
}
