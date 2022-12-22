package uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationconcurrentorder;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.ListGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.WRITTEN_REPRESENTATION_CONCURRENT;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    WrittenRepresentationConcurrentOrderGenerator.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class WrittenRepresentationConcurrentGeneratorOrderTest {

    private static final String BEARER_TOKEN = "Bearer Token";
    private static final byte[] bytes = {1, 2, 3, 4, 5, 6};

    @MockBean
    private UnsecuredDocumentManagementService documentManagementService;
    @MockBean
    private DocumentGeneratorService documentGeneratorService;
    @MockBean
    private ListGeneratorService listGeneratorService;
    @Autowired
    private WrittenRepresentationConcurrentOrderGenerator writtenRepresentationConcurrentOrderGenerator;

    @Test
    void shouldGenerateWrittenRepresentationConcurrentDocument() {
        CaseData caseData = CaseDataBuilder.builder().writtenRepresentationConcurrentApplication().build();

        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class),
                                                               eq(WRITTEN_REPRESENTATION_CONCURRENT)))
            .thenReturn(new DocmosisDocument(WRITTEN_REPRESENTATION_CONCURRENT.getDocumentTitle(), bytes));

        when(listGeneratorService.applicationType(caseData)).thenReturn("Extend time");
        when(listGeneratorService.claimantsName(caseData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
        when(listGeneratorService.defendantsName(caseData)).thenReturn("Test Defendant1 Name, Test Defendant2 Name");

        writtenRepresentationConcurrentOrderGenerator.generate(caseData, BEARER_TOKEN);

        verify(documentManagementService).uploadDocument(
            BEARER_TOKEN,
            new PDF(any(), any(), DocumentType.WRITTEN_REPRESENTATION_CONCURRENT)
        );
        verify(documentGeneratorService).generateDocmosisDocument(any(JudgeDecisionPdfDocument.class),
                                                                  eq(WRITTEN_REPRESENTATION_CONCURRENT));
    }

    @Nested
    class GetTemplateData {

        @Test
        void whenJudgeMakeDecision_ShouldGetWrittenRepresentationConcurrentData() {
            CaseData caseData = CaseDataBuilder.builder().writtenRepresentationConcurrentApplication().build()
                .toBuilder().build();

            when(listGeneratorService.applicationType(caseData)).thenReturn("Extend time");
            when(listGeneratorService.claimantsName(caseData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(caseData))
                .thenReturn("Test Defendant1 Name, Test Defendant2 Name");

            var templateData = writtenRepresentationConcurrentOrderGenerator.getTemplateData(caseData);

            assertThatFieldsAreCorrect_WrittenRepresentationConcurrent(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_WrittenRepresentationConcurrent(JudgeDecisionPdfDocument templateData,
                                                                                CaseData caseData) {
            Assertions.assertAll(
                "Written Representation Concurrent Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getUploadDeadlineDate(),
                                   caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                       .getWrittenConcurrentRepresentationsBy()),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(templateData.getJudicialByCourtsInitiativeForWrittenRep(), StringUtils.EMPTY),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudgeRecitalText()),
                () -> assertEquals(templateData.getWrittenOrder(), caseData.getDirectionInRelationToHearingText())
            );
        }

        private String getClaimats(CaseData caseData) {
            List<String> claimantsName = new ArrayList<>();
            claimantsName.add(caseData.getClaimant1PartyName());
            if (caseData.getDefendant2PartyName() != null) {
                claimantsName.add(caseData.getClaimant2PartyName());
            }
            return String.join(", ", claimantsName);
        }

        private String getDefendats(CaseData caseData) {
            List<String> defendatsName = new ArrayList<>();
            defendatsName.add(caseData.getDefendant1PartyName());
            if (caseData.getDefendant2PartyName() != null) {
                defendatsName.add(caseData.getDefendant2PartyName());
            }
            return String.join(", ", defendatsName);
        }

        private String getApplicationType(CaseData caseData) {
            List<GeneralApplicationTypes> types = caseData.getGeneralAppType().getTypes();
            return types.stream()
                .map(GeneralApplicationTypes::getDisplayedValue).collect(Collectors.joining(", "));
        }
    }
}
