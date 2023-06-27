package uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationsequentialorder;

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
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.model.genapplication.GAOrderCourtOwnInitiativeGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAOrderWithoutNoticeGAspec;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.ListGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.WRITTEN_REPRESENTATION_SEQUENTIAL;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.DATE_FORMATTER;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    WrittenRepresentationSequentailOrderGenerator.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class WrittenRepresentationSequentialGeneratorOrderTest {

    private static final String BEARER_TOKEN = "Bearer Token";
    private static final byte[] bytes = {1, 2, 3, 4, 5, 6};

    @MockBean
    private UnsecuredDocumentManagementService documentManagementService;
    @MockBean
    private DocumentGeneratorService documentGeneratorService;
    @MockBean
    private ListGeneratorService listGeneratorService;
    @Autowired
    private WrittenRepresentationSequentailOrderGenerator writtenRepresentationSequentailOrderGenerator;

    @Test
    void shouldGenerateWrittenRepresentationSequentialDocument() {
        CaseData caseData = CaseDataBuilder.builder().writtenRepresentationSequentialApplication().build();

        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class),
                                                               eq(WRITTEN_REPRESENTATION_SEQUENTIAL)))
            .thenReturn(new DocmosisDocument(WRITTEN_REPRESENTATION_SEQUENTIAL.getDocumentTitle(), bytes));

        when(listGeneratorService.applicationType(caseData)).thenReturn("Extend time");
        when(listGeneratorService.claimantsName(caseData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
        when(listGeneratorService.defendantsName(caseData)).thenReturn("Test Defendant1 Name, Test Defendant2 Name");

        writtenRepresentationSequentailOrderGenerator.generate(caseData, BEARER_TOKEN);

        verify(documentManagementService).uploadDocument(
            BEARER_TOKEN,
            new PDF(any(), any(), DocumentType.WRITTEN_REPRESENTATION_SEQUENTIAL)
        );
        verify(documentGeneratorService).generateDocmosisDocument(any(JudgeDecisionPdfDocument.class),
                                                                  eq(WRITTEN_REPRESENTATION_SEQUENTIAL));
    }

    @Nested
    class GetTemplateData {

        @Test
        void whenJudgeMakeDecision_ShouldGetWrittenRepresentationSequentialData() {
            CaseData caseData = CaseDataBuilder.builder().writtenRepresentationSequentialApplication().build()
                .toBuilder().build();

            when(listGeneratorService.applicationType(caseData)).thenReturn("Extend time");
            when(listGeneratorService.claimantsName(caseData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(caseData))
                .thenReturn("Test Defendant1 Name, Test Defendant2 Name");

            var templateData = writtenRepresentationSequentailOrderGenerator.getTemplateData(caseData);

            assertThatFieldsAreCorrect_WrittenRepresentationSequential(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_WrittenRepresentationSequential(JudgeDecisionPdfDocument templateData,
                                                                                CaseData caseData) {
            Assertions.assertAll(
                "Written Representation Sequential Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getUploadDeadlineDate(),
                                   caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                       .getWrittenSequentailRepresentationsBy()),
                () -> assertEquals(templateData.getResponseDeadlineDate(),
                                   caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                       .getSequentialApplicantMustRespondWithin()),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(templateData.getJudicialByCourtsInitiativeForWrittenRep(), caseData
                    .getOrderCourtOwnInitiativeForWrittenRep().getOrderCourtOwnInitiative() + " ".concat(
                        caseData.getOrderCourtOwnInitiativeForWrittenRep()
                            .getOrderCourtOwnInitiativeDate().format(DATE_FORMATTER))),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudgeRecitalText()),
                () -> assertEquals(templateData.getWrittenOrder(), caseData.getDirectionInRelationToHearingText())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetWrittenRepresentationSequentialData_Option2() {
            CaseData caseData = CaseDataBuilder.builder().writtenRepresentationSequentialApplication().build()
                .toBuilder().build();
            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialByCourtsInitiativeForWrittenRep(GAByCourtsInitiativeGAspec.OPTION_2)
                .orderWithoutNoticeForWrittenRep(
                    GAOrderWithoutNoticeGAspec.builder().orderWithoutNotice("abcde")
                        .orderWithoutNoticeDate(LocalDate.now()).build())
                .orderCourtOwnInitiativeForWrittenRep(
                    GAOrderCourtOwnInitiativeGAspec.builder().build()).build();

            CaseData updateData = caseDataBuilder.build();

            when(listGeneratorService.applicationType(updateData)).thenReturn("Extend time");
            when(listGeneratorService.claimantsName(updateData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(updateData))
                .thenReturn("Test Defendant1 Name, Test Defendant2 Name");

            var templateData = writtenRepresentationSequentailOrderGenerator
                .getTemplateData(updateData);

            assertThatFieldsAreCorrect_WrittenRepSequential_Option2(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_WrittenRepSequential_Option2(JudgeDecisionPdfDocument templateData,
                                                                                CaseData caseData) {
            Assertions.assertAll(
                "Written Representation Sequential Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getUploadDeadlineDate(),
                                   caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                       .getWrittenSequentailRepresentationsBy()),
                () -> assertEquals(templateData.getResponseDeadlineDate(),
                                   caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                       .getSequentialApplicantMustRespondWithin()),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(templateData.getJudicialByCourtsInitiativeForWrittenRep(), caseData
                    .getOrderWithoutNoticeForWrittenRep().getOrderWithoutNotice() + " ".concat(
                    caseData.getOrderWithoutNoticeForWrittenRep()
                        .getOrderWithoutNoticeDate().format(DATE_FORMATTER))),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudgeRecitalText()),
                () -> assertEquals(templateData.getWrittenOrder(), caseData.getDirectionInRelationToHearingText())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetWrittenRepresentationSequentialData_Option3() {
            CaseData caseData = CaseDataBuilder.builder().writtenRepresentationSequentialApplication().build()
                .toBuilder().build();
            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialByCourtsInitiativeForWrittenRep(GAByCourtsInitiativeGAspec.OPTION_3)
                .orderCourtOwnInitiativeForWrittenRep(
                    GAOrderCourtOwnInitiativeGAspec.builder().build()).build();

            CaseData updateData = caseDataBuilder.build();

            when(listGeneratorService.applicationType(updateData)).thenReturn("Extend time");
            when(listGeneratorService.claimantsName(updateData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(updateData))
                .thenReturn("Test Defendant1 Name, Test Defendant2 Name");

            var templateData = writtenRepresentationSequentailOrderGenerator
                .getTemplateData(updateData);

            assertThatFieldsAreCorrect_WrittenRepSequential_Option3(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_WrittenRepSequential_Option3(JudgeDecisionPdfDocument templateData,
                                                                             CaseData caseData) {
            Assertions.assertAll(
                "Written Representation Sequential Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getUploadDeadlineDate(),
                                   caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                       .getWrittenSequentailRepresentationsBy()),
                () -> assertEquals(templateData.getResponseDeadlineDate(),
                                   caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                       .getSequentialApplicantMustRespondWithin()),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(StringUtils.EMPTY, templateData.getJudicialByCourtsInitiativeForWrittenRep()),
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
