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
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.model.genapplication.GACaseLocation;
import uk.gov.hmcts.reform.civil.model.genapplication.GAOrderCourtOwnInitiativeGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAOrderWithoutNoticeGAspec;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.ListGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
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
    @MockBean
    private IdamClient idamClient;
    @Autowired
    private WrittenRepresentationSequentailOrderGenerator writtenRepresentationSequentailOrderGenerator;
    @MockBean
    private DocmosisService docmosisService;

    @Test
    void shouldGenerateWrittenRepresentationSequentialDocument() {
        CaseData caseData = CaseDataBuilder.builder().writtenRepresentationSequentialApplication().build();

        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class),
                                                               eq(WRITTEN_REPRESENTATION_SEQUENTIAL)))
            .thenReturn(new DocmosisDocument(WRITTEN_REPRESENTATION_SEQUENTIAL.getDocumentTitle(), bytes));

        when(listGeneratorService.applicationType(caseData)).thenReturn("Extend time");
        when(idamClient
                .getUserDetails(any()))
                .thenReturn(UserDetails.builder().forename("John").surname("Doe").build());
        when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
            .thenReturn(LocationRefData.builder().epimmsId("2").venueName("London").build());

        writtenRepresentationSequentailOrderGenerator.generate(caseData, BEARER_TOKEN);

        verify(documentManagementService).uploadDocument(
            BEARER_TOKEN,
            new PDF(any(), any(), DocumentType.WRITTEN_REPRESENTATION_SEQUENTIAL)
        );
        verify(documentGeneratorService).generateDocmosisDocument(any(JudgeDecisionPdfDocument.class),
                                                                  eq(WRITTEN_REPRESENTATION_SEQUENTIAL));
    }

    @Test
    void shouldThrowExceptionWhenNoLocationMatch() {
        CaseData caseData = CaseDataBuilder.builder()
            .writtenRepresentationConcurrentApplication()
            .caseManagementLocation(GACaseLocation.builder().baseLocation("8").build())
            .build();

        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class),
                                                               eq(WRITTEN_REPRESENTATION_SEQUENTIAL)))
            .thenReturn(new DocmosisDocument(WRITTEN_REPRESENTATION_SEQUENTIAL.getDocumentTitle(), bytes));

        when(listGeneratorService.applicationType(caseData)).thenReturn("Extend time");
        when(idamClient
                 .getUserDetails(any()))
            .thenReturn(UserDetails.builder().forename("John").surname("Doe").build());
        doThrow(new IllegalArgumentException("Court Name is not found in location data"))
            .when(docmosisService).getCaseManagementLocationVenueName(any(), any());

        Exception exception =
            assertThrows(IllegalArgumentException.class, ()
                -> writtenRepresentationSequentailOrderGenerator.generate(caseData, BEARER_TOKEN));
        String expectedMessage = "Court Name is not found in location data";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Nested
    class GetTemplateData {

        @Test
        void whenJudgeMakeDecision_ShouldGetWrittenRepresentationSequentialData() {
            CaseData caseData = CaseDataBuilder.builder().writtenRepresentationSequentialApplication().build()
                .toBuilder().build();

            when(listGeneratorService.applicationType(caseData)).thenReturn("Extend time");
            when(idamClient
                    .getUserDetails(any()))
                    .thenReturn(UserDetails.builder().forename("John").surname("Doe").build());
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").venueName("Reading").build());

            var templateData = writtenRepresentationSequentailOrderGenerator.getTemplateData(caseData, "auth");

            assertThatFieldsAreCorrect_WrittenRepresentationSequential(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_WrittenRepresentationSequential(JudgeDecisionPdfDocument templateData,
                                                                                CaseData caseData) {
            Assertions.assertAll(
                "Written Representation Sequential Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), caseData.getClaimant2PartyName()),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(templateData.getDefendant2Name(), caseData.getDefendant2PartyName()),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(templateData.getCourtName(), "Reading"),
                () -> assertEquals(templateData.getJudicialByCourtsInitiativeForWrittenRep(), caseData
                    .getOrderCourtOwnInitiativeForWrittenRep().getOrderCourtOwnInitiative() + " ".concat(
                        caseData.getOrderCourtOwnInitiativeForWrittenRep()
                            .getOrderCourtOwnInitiativeDate().format(DATE_FORMATTER))),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudgeRecitalText()),
                () -> assertEquals(templateData.getWrittenOrder(), caseData.getDirectionInRelationToHearingText()),
                () -> assertEquals(templateData.getJudgeNameTitle(), "John Doe"),
                () -> assertEquals(templateData.getAddress(), caseData.getCaseManagementLocation().getAddress()),
                () -> assertEquals(templateData.getSiteName(), caseData.getCaseManagementLocation().getSiteName()),
                () -> assertEquals(templateData.getPostcode(), caseData.getCaseManagementLocation().getPostcode())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetWrittenRepresentationSequentialData_Option2() {
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").venueName("Manchester").build());
            CaseData caseData = CaseDataBuilder.builder().writtenRepresentationSequentialApplication()
                .build()
                .toBuilder()
                .caseManagementLocation(GACaseLocation.builder().baseLocation("3").build())
                .build();
            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialByCourtsInitiativeForWrittenRep(GAByCourtsInitiativeGAspec.OPTION_2)
                .orderWithoutNoticeForWrittenRep(
                    GAOrderWithoutNoticeGAspec.builder().orderWithoutNotice("abcde")
                        .orderWithoutNoticeDate(LocalDate.now()).build())
                .orderCourtOwnInitiativeForWrittenRep(
                    GAOrderCourtOwnInitiativeGAspec.builder().build()).build();

            CaseData updateData = caseDataBuilder.build();

            when(listGeneratorService.applicationType(updateData)).thenReturn("Extend time");
            when(idamClient
                    .getUserDetails(any()))
                    .thenReturn(UserDetails.builder().forename("John").surname("Doe").build());

            var templateData = writtenRepresentationSequentailOrderGenerator
                .getTemplateData(updateData, "auth");

            assertThatFieldsAreCorrect_WrittenRepSequential_Option2(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_WrittenRepSequential_Option2(JudgeDecisionPdfDocument templateData,
                                                                                CaseData caseData) {
            Assertions.assertAll(
                "Written Representation Sequential Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), caseData.getClaimant2PartyName()),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(templateData.getDefendant2Name(), caseData.getDefendant2PartyName()),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(templateData.getCourtName(), "Manchester"),
                () -> assertEquals(templateData.getJudicialByCourtsInitiativeForWrittenRep(), caseData
                    .getOrderWithoutNoticeForWrittenRep().getOrderWithoutNotice() + " ".concat(
                    caseData.getOrderWithoutNoticeForWrittenRep()
                        .getOrderWithoutNoticeDate().format(DATE_FORMATTER))),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudgeRecitalText()),
                () -> assertEquals(templateData.getWrittenOrder(), caseData.getDirectionInRelationToHearingText()),
                () -> assertEquals("John Doe", templateData.getJudgeNameTitle())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetWrittenRepresentationSequentialData_Option3_1v1() {
            CaseData caseData = CaseDataBuilder.builder().writtenRepresentationSequentialApplication().build()
                .toBuilder().isMultiParty(YesOrNo.YES)
                .caseManagementLocation(GACaseLocation.builder().baseLocation("2").build())
                .build();
            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialByCourtsInitiativeForWrittenRep(GAByCourtsInitiativeGAspec.OPTION_3)
                .orderCourtOwnInitiativeForWrittenRep(
                    GAOrderCourtOwnInitiativeGAspec.builder().build()).build();

            CaseData updateData = caseDataBuilder.build();

            when(listGeneratorService.applicationType(updateData)).thenReturn("Extend time");
            when(idamClient
                    .getUserDetails(any()))
                    .thenReturn(UserDetails.builder().forename("John").surname("Doe").build());
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").venueName("London").build());

            var templateData = writtenRepresentationSequentailOrderGenerator
                .getTemplateData(updateData, "auth");

            assertThatFieldsAreCorrect_WrittenRepSequential_Option3(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_WrittenRepSequential_Option3(JudgeDecisionPdfDocument templateData,
                                                                             CaseData caseData) {
            Assertions.assertAll(
                "Written Representation Sequential Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), caseData.getClaimant2PartyName()),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(templateData.getDefendant2Name(), caseData.getDefendant2PartyName()),
                () -> assertEquals(YES, templateData.getIsMultiParty()),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(StringUtils.EMPTY, templateData.getJudicialByCourtsInitiativeForWrittenRep()),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudgeRecitalText()),
                () -> assertEquals(templateData.getCourtName(), "London"),
                () -> assertEquals(templateData.getWrittenOrder(), caseData.getDirectionInRelationToHearingText()),
                () -> assertEquals("John Doe", templateData.getJudgeNameTitle())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetWrittenRepresentationSequentialData_Option3_1V1() {
            CaseData caseData = CaseDataBuilder.builder().writtenRepresentationSequentialApplication().build()
                .toBuilder()
                .defendant2PartyName(null)
                .claimant2PartyName(null)
                .isMultiParty(YesOrNo.NO).build();
            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialByCourtsInitiativeForWrittenRep(GAByCourtsInitiativeGAspec.OPTION_3)
                .orderCourtOwnInitiativeForWrittenRep(
                    GAOrderCourtOwnInitiativeGAspec.builder().build()).build();

            CaseData updateData = caseDataBuilder.build();

            when(listGeneratorService.applicationType(updateData)).thenReturn("Extend time");
            when(idamClient
                     .getUserDetails(any()))
                .thenReturn(UserDetails.builder().forename("John").surname("Doe").build());
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").venueName("Reading").build());

            var templateData = writtenRepresentationSequentailOrderGenerator
                .getTemplateData(updateData, "auth");

            assertThatFieldsAreCorrect_WrittenRepSequential_Option3_1V1(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_WrittenRepSequential_Option3_1V1(JudgeDecisionPdfDocument templateData,
                                                                             CaseData caseData) {
            Assertions.assertAll(
                "Written Representation Sequential Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertNull(templateData.getClaimant2Name()),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertNull(templateData.getDefendant2Name()),
                () -> assertEquals(NO, templateData.getIsMultiParty()),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(StringUtils.EMPTY, templateData.getJudicialByCourtsInitiativeForWrittenRep()),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudgeRecitalText()),
                () -> assertEquals(templateData.getWrittenOrder(), caseData.getDirectionInRelationToHearingText()),
                () -> assertEquals("John Doe", templateData.getJudgeNameTitle())
            );
        }

        private String getApplicationType(CaseData caseData) {
            List<GeneralApplicationTypes> types = caseData.getGeneralAppType().getTypes();
            return types.stream()
                .map(GeneralApplicationTypes::getDisplayedValue).collect(Collectors.joining(", "));
        }
    }
}
