package uk.gov.hmcts.reform.civil.service.docmosis.directionorder;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import uk.gov.hmcts.reform.civil.enums.dq.FinalOrderShowToggle;
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.ListGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.DIRECTION_ORDER;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.DATE_FORMATTER;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    DirectionOrderGenerator.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class DirectionOrderGeneratorTest {

    private static final String BEARER_TOKEN = "Bearer Token";
    private static final byte[] bytes = {1, 2, 3, 4, 5, 6};

    @MockBean
    private UnsecuredDocumentManagementService documentManagementService;
    @MockBean
    private DocumentGeneratorService documentGeneratorService;
    @MockBean
    private ListGeneratorService listGeneratorService;
    @Autowired
    private DirectionOrderGenerator directionOrderGenerator;

    @Test
    void shouldGenerateDirectionOrderDocument() {
        CaseData caseData = CaseDataBuilder.builder().directionOrderApplication().build();

        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class), eq(DIRECTION_ORDER)))
            .thenReturn(new DocmosisDocument(DIRECTION_ORDER.getDocumentTitle(), bytes));

        when(listGeneratorService.applicationType(caseData)).thenReturn("Extend time");
        when(listGeneratorService.claimantsName(caseData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
        when(listGeneratorService.defendantsName(caseData)).thenReturn("Test Defendant1 Name, Test Defendant2 Name");

        directionOrderGenerator.generate(caseData, BEARER_TOKEN);

        verify(documentManagementService).uploadDocument(
            BEARER_TOKEN,
            new PDF(any(), any(), DocumentType.DIRECTION_ORDER)
        );
        verify(documentGeneratorService).generateDocmosisDocument(any(JudgeDecisionPdfDocument.class),
                                                                  eq(DIRECTION_ORDER));
    }

    @Nested
    class GetTemplateData {

        @Test
        void whenJudgeMakeDecision_ShouldGetHearingOrderData() {
            CaseData caseData = CaseDataBuilder.builder().directionOrderApplication().build().toBuilder()
                .build();

            when(listGeneratorService.applicationType(caseData)).thenReturn("Extend time");
            when(listGeneratorService.claimantsName(caseData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(caseData))
                .thenReturn("Test Defendant1 Name, Test Defendant2 Name");

            var templateData = directionOrderGenerator.getTemplateData(caseData);

            assertThatFieldsAreCorrect_DirectionOrder(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_DirectionOrder(JudgeDecisionPdfDocument templateData,
                                                               CaseData caseData) {
            Assertions.assertAll(
                "Direction Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getJudgeDirection(),
                                   caseData.getJudicialDecisionMakeOrder().getDirectionsText()),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(templateData.getJudicialByCourtsInitiative(), caseData
                    .getJudicialDecisionMakeOrder().getOrderCourtOwnInitiative()
                    + " ".concat(LocalDate.now().format(DATE_FORMATTER))),
                () -> assertEquals(templateData.getJudgeRecital(),
                                   caseData.getJudicialDecisionMakeOrder().getJudgeRecitalText())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetHearingOrderData_Option2() {
            CaseData caseData = CaseDataBuilder.builder().directionOrderApplication().build().toBuilder()
                .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                           .directionsText("Test Direction")
                                                           .judicialByCourtsInitiative(
                                                               GAByCourtsInitiativeGAspec.OPTION_2)
                                                           .orderWithoutNotice("abcdef")
                                                           .orderWithoutNoticeDate(LocalDate.now())
                                                           .reasonForDecisionText("Test Reason")
                                                           .makeAnOrder(GIVE_DIRECTIONS_WITHOUT_HEARING)
                                                           .directionsResponseByDate(LocalDate.now())
                                                           .showJudgeRecitalText(List.of(FinalOrderShowToggle.SHOW))
                                                           .judgeRecitalText("Test Judge's recital")
                                                           .build()).build();
            CaseData updateCaseData = caseDataBuilder.build();

            when(listGeneratorService.applicationType(updateCaseData)).thenReturn("Extend time");
            when(listGeneratorService.claimantsName(updateCaseData))
                .thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(updateCaseData))
                .thenReturn("Test Defendant1 Name, Test Defendant2 Name");

            var templateData = directionOrderGenerator.getTemplateData(updateCaseData);

            assertJudicialByCourtsInitiative_Option2(templateData, updateCaseData);
        }

        private void assertJudicialByCourtsInitiative_Option2(JudgeDecisionPdfDocument templateData,
                                                               CaseData caseData) {
            Assertions.assertAll(
                "Direction Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getJudgeDirection(),
                                   caseData.getJudicialDecisionMakeOrder().getDirectionsText()),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(templateData.getJudicialByCourtsInitiative(), caseData
                    .getJudicialDecisionMakeOrder().getOrderWithoutNotice()
                    + " ".concat(LocalDate.now().format(DATE_FORMATTER))),
                () -> assertEquals(templateData.getJudgeRecital(),
                                   caseData.getJudicialDecisionMakeOrder().getJudgeRecitalText())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetHearingOrderData_Option3() {
            CaseData caseData = CaseDataBuilder.builder().directionOrderApplication().build().toBuilder()
                .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                          .directionsText("Test Direction")
                                                          .judicialByCourtsInitiative(
                                                              GAByCourtsInitiativeGAspec.OPTION_3)
                                                          .reasonForDecisionText("Test Reason")
                                                          .makeAnOrder(GIVE_DIRECTIONS_WITHOUT_HEARING)
                                                          .directionsResponseByDate(LocalDate.now())
                                                          .showJudgeRecitalText(List.of(FinalOrderShowToggle.SHOW))
                                                          .judgeRecitalText("Test Judge's recital")
                                                          .build()).build();

            CaseData updateCaseData = caseDataBuilder.build();

            when(listGeneratorService.applicationType(updateCaseData)).thenReturn("Extend time");
            when(listGeneratorService.claimantsName(updateCaseData))
                .thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(updateCaseData))
                .thenReturn("Test Defendant1 Name, Test Defendant2 Name");

            var templateData = directionOrderGenerator.getTemplateData(updateCaseData);

            assertJudicialByCourtsInitiative_Option3(templateData, updateCaseData);
        }

        private void assertJudicialByCourtsInitiative_Option3(JudgeDecisionPdfDocument templateData,
                                                      CaseData caseData) {
            Assertions.assertAll(
                "Direction Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getJudgeDirection(),
                                   caseData.getJudicialDecisionMakeOrder().getDirectionsText()),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(StringUtils.EMPTY, templateData.getJudicialByCourtsInitiative()),
                () -> assertEquals(templateData.getJudgeRecital(),
                                   caseData.getJudicialDecisionMakeOrder().getJudgeRecitalText())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldHideRecital_whileUnchecked() {
            CaseData caseData = CaseDataBuilder.builder().directionOrderApplication().build().toBuilder()
                    .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                    .directionsText("Test Direction")
                    .judicialByCourtsInitiative(
                            GAByCourtsInitiativeGAspec.OPTION_3)
                    .reasonForDecisionText("Test Reason")
                    .makeAnOrder(GIVE_DIRECTIONS_WITHOUT_HEARING)
                    .directionsResponseByDate(LocalDate.now())
                    .judgeRecitalText("Test Judge's recital")
                    .build()).build();

            CaseData updateCaseData = caseDataBuilder.build();

            when(listGeneratorService.applicationType(updateCaseData)).thenReturn("Extend time");
            when(listGeneratorService.claimantsName(updateCaseData))
                    .thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(updateCaseData))
                    .thenReturn("Test Defendant1 Name, Test Defendant2 Name");

            var templateData = directionOrderGenerator.getTemplateData(updateCaseData);

            assertNull(templateData.getJudgeRecital());
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
