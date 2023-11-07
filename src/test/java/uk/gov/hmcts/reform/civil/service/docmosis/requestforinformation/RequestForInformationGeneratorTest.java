package uk.gov.hmcts.reform.civil.service.docmosis.requestforinformation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.requestmoreinformation.RequestForInformationGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.REQUEST_FOR_INFORMATION;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    RequestForInformationGenerator.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class RequestForInformationGeneratorTest {

    private static final String BEARER_TOKEN = "Bearer Token";
    private static final byte[] bytes = {1, 2, 3, 4, 5, 6};

    @MockBean
    private UnsecuredDocumentManagementService documentManagementService;
    @MockBean
    private DocumentGeneratorService documentGeneratorService;
    @Autowired
    private RequestForInformationGenerator requestForInformationGenerator;

    @Test
    void shouldGenerateRequestForInformationDocument() {
        CaseData caseData = CaseDataBuilder.builder().requestForInformationApplication().build();

        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class), eq(REQUEST_FOR_INFORMATION)))
            .thenReturn(new DocmosisDocument(REQUEST_FOR_INFORMATION.getDocumentTitle(), bytes));

        requestForInformationGenerator.generate(caseData, BEARER_TOKEN);

        verify(documentManagementService).uploadDocument(
            BEARER_TOKEN,
            new PDF(any(), any(), DocumentType.REQUEST_FOR_INFORMATION)
        );
        verify(documentGeneratorService).generateDocmosisDocument(any(JudgeDecisionPdfDocument.class),
                                                                  eq(REQUEST_FOR_INFORMATION));
    }

    @Nested
    class GetTemplateData {

        @Test
        void whenJudgeMakeDecision_ShouldGetRequestForInformationData() {
            CaseData caseData = CaseDataBuilder.builder().requestForInformationApplication().build().toBuilder()
                .build();

            var templateData = requestForInformationGenerator.getTemplateData(caseData);

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
                () -> assertEquals(templateData.getCourtName(), caseData.getLocationName()),
                () -> assertEquals(templateData.getJudgeComments(), caseData.getJudicialDecisionRequestMoreInfo()
                    .getJudgeRequestMoreInfoText())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetRequestForInformationData_1v1() {
            CaseData caseData = CaseDataBuilder.builder().requestForInformationApplication().build().toBuilder()
                .defendant2PartyName(null)
                .claimant2PartyName(null)
                .isMultiParty(NO)
                .build();

            var templateData = requestForInformationGenerator.getTemplateData(caseData);

            assertThatFieldsAreCorrect_RequestForInformation_1v1(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_RequestForInformation_1v1(JudgeDecisionPdfDocument templateData,
                                                                      CaseData caseData) {
            Assertions.assertAll(
                "Request For Information Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), null),
                () -> assertEquals(templateData.getIsMultiParty(), NO),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(templateData.getDefendant2Name(), null),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudicialDecisionRequestMoreInfo()
                    .getJudgeRecitalText()),
                () -> assertEquals(templateData.getCourtName(), caseData.getLocationName()),
                () -> assertEquals(templateData.getJudgeComments(), caseData.getJudicialDecisionRequestMoreInfo()
                    .getJudgeRequestMoreInfoText())
            );
        }
    }
}
