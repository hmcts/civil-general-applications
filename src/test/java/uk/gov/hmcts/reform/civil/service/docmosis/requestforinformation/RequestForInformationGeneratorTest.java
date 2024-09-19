package uk.gov.hmcts.reform.civil.service.docmosis.requestforinformation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.model.genapplication.GACaseLocation;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.GaForLipService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.requestmoreinformation.RequestForInformationGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.SecuredDocumentManagementService;

import static java.time.LocalDate.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.REQUEST_FOR_INFORMATION;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.REQUEST_FOR_INFORMATION_SEND_TO_OTHER_PARTY;

@ExtendWith(MockitoExtension.class)
class RequestForInformationGeneratorTest {

    private static final String BEARER_TOKEN = "Bearer Token";
    private static final byte[] bytes = {1, 2, 3, 4, 5, 6};

    @Mock
    private SecuredDocumentManagementService documentManagementService;
    @Mock
    private DocumentGeneratorService documentGeneratorService;
    @InjectMocks
    private RequestForInformationGenerator requestForInformationGenerator;
    @Mock
    private DocmosisService docmosisService;

    @Mock
    private GaForLipService gaForLipService;

    @Test
    void shouldGenerateRequestForInformationDocument() {
        CaseData caseData = CaseDataBuilder.builder().requestForInformationApplication().build();

        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class), eq(REQUEST_FOR_INFORMATION)))
            .thenReturn(new DocmosisDocument(REQUEST_FOR_INFORMATION.getDocumentTitle(), bytes));
        when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
            .thenReturn(LocationRefData.builder().epimmsId("2").venueName("London").build());

        requestForInformationGenerator.generate(caseData, BEARER_TOKEN);

        verify(documentManagementService).uploadDocument(
            BEARER_TOKEN,
            new PDF(any(), any(), DocumentType.REQUEST_FOR_INFORMATION)
        );
        verify(documentGeneratorService).generateDocmosisDocument(any(JudgeDecisionPdfDocument.class),
                                                                  eq(REQUEST_FOR_INFORMATION));
    }

    @Test
    void shouldGenerateSendAppToOtherPartyDocumentForLipClaimant() {
        when(gaForLipService.isLipApp(any(CaseData.class))).thenReturn(true);
        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class), eq(REQUEST_FOR_INFORMATION_SEND_TO_OTHER_PARTY)))
            .thenReturn(new DocmosisDocument(REQUEST_FOR_INFORMATION_SEND_TO_OTHER_PARTY.getDocumentTitle(), bytes));
        when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
            .thenReturn(LocationRefData.builder().epimmsId("2").venueName("London").build());

        CaseData caseData = CaseDataBuilder.builder().requestForInformationApplication()
            .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                 .judgeRecitalText("test")
                                                 .requestMoreInfoOption(SEND_APP_TO_OTHER_PARTY)
                                                 .judgeRequestMoreInfoByDate(now()).build())
            .build();

        requestForInformationGenerator.generate(caseData, BEARER_TOKEN);

        verify(documentManagementService).uploadDocument(
            BEARER_TOKEN,
            new PDF(any(), any(), DocumentType.SEND_APP_TO_OTHER_PARTY)
        );
        verify(documentGeneratorService).generateDocmosisDocument(any(JudgeDecisionPdfDocument.class),
                                                                  eq(REQUEST_FOR_INFORMATION_SEND_TO_OTHER_PARTY));
    }

    @Test
    void shouldThrowExceptionWhenNoLocationMatch() {
        CaseData caseData = CaseDataBuilder.builder().requestForInformationApplication()
            .caseManagementLocation(GACaseLocation.builder().baseLocation("8").build())
            .build();

        doThrow(new IllegalArgumentException("Court Name is not found in location data"))
            .when(docmosisService).getCaseManagementLocationVenueName(any(), any());

        Exception exception =
            assertThrows(IllegalArgumentException.class, ()
                -> requestForInformationGenerator.generate(caseData, BEARER_TOKEN));
        String expectedMessage = "Court Name is not found in location data";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Nested
    class GetTemplateData {

        @Test
        void whenJudgeMakeDecision_ShouldGetRequestForInformationData() {
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").venueName("London").build());
            CaseData caseData = CaseDataBuilder.builder().requestForInformationApplication().build().toBuilder()
                .build();

            var templateData = requestForInformationGenerator.getTemplateData(caseData, "auth");

            assertThatFieldsAreCorrect_RequestForInformation(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_RequestForInformation(JudgeDecisionPdfDocument templateData,
                                                                      CaseData caseData) {
            Assertions.assertAll(
                "Request For Information Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), caseData.getClaimant2PartyName()),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(templateData.getDefendant2Name(), caseData.getDefendant2PartyName()),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudicialDecisionRequestMoreInfo()
                    .getJudgeRecitalText()),
                () -> assertEquals(templateData.getCourtName(), "London"),
                () -> assertEquals(templateData.getJudgeComments(), caseData.getJudicialDecisionRequestMoreInfo()
                    .getJudgeRequestMoreInfoText()),
                () -> assertEquals(templateData.getAddress(), caseData.getCaseManagementLocation().getAddress()),
                () -> assertEquals(templateData.getSiteName(), caseData.getCaseManagementLocation().getSiteName()),
                () -> assertEquals(templateData.getPostcode(), caseData.getCaseManagementLocation().getPostcode())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetRequestForInformationData_1v1() {
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").venueName("Manchester").build());
            CaseData caseData = CaseDataBuilder.builder().requestForInformationApplication().build().toBuilder()
                .defendant2PartyName(null)
                .claimant2PartyName(null)
                .caseManagementLocation(GACaseLocation.builder().baseLocation("3").build())
                .isMultiParty(NO)
                .build();

            var templateData =
                requestForInformationGenerator.getTemplateData(caseData, "auth");

            assertThatFieldsAreCorrect_RequestForInformation_1v1(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_RequestForInformation_1v1(JudgeDecisionPdfDocument templateData,
                                                                      CaseData caseData) {
            Assertions.assertAll(
                "Request For Information Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertNull(templateData.getClaimant2Name()),
                () -> assertEquals(NO, templateData.getIsMultiParty()),
                () -> assertEquals(templateData.getCourtName(), "Manchester"),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertNull(templateData.getDefendant2Name()),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudicialDecisionRequestMoreInfo()
                    .getJudgeRecitalText()),
                () -> assertEquals(templateData.getJudgeComments(), caseData.getJudicialDecisionRequestMoreInfo()
                    .getJudgeRequestMoreInfoText())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetRequestForInformationData_LIP_Send_to_other_party() {

            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").venueName("Manchester").build());
            CaseData caseData = CaseDataBuilder.builder().requestForInformationApplication().build().toBuilder()
                .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                     .judgeRecitalText("test")
                                                     .requestMoreInfoOption(SEND_APP_TO_OTHER_PARTY)
                                                     .judgeRequestMoreInfoByDate(now()).build())
                .caseManagementLocation(GACaseLocation.builder().baseLocation("3").build())
                .build();

            var templateData =
                requestForInformationGenerator.getTemplateData(caseData, "auth");

            Assertions.assertAll(
                "Request For Information Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getCourtName(), "Manchester"),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudicialDecisionRequestMoreInfo()
                    .getJudgeRecitalText()),
                () -> assertEquals(templateData.getJudgeComments(), caseData.getJudicialDecisionRequestMoreInfo()
                    .getJudgeRequestMoreInfoText()),
                () -> assertEquals(templateData.getApplicationCreatedDate(), caseData.getCreatedDate().toLocalDate()),
                () -> assertEquals(templateData.getAdditionalApplicationFee(), "£275")
            );
        }
    }
}
