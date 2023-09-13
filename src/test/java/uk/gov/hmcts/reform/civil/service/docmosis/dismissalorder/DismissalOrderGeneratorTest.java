package uk.gov.hmcts.reform.civil.service.docmosis.dismissalorder;

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

import uk.gov.hmcts.reform.civil.enums.YesOrNo;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.DISMISSAL_ORDER;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.DATE_FORMATTER;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    DismissalOrderGenerator.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class DismissalOrderGeneratorTest {

    private static final String BEARER_TOKEN = "Bearer Token";
    private static final byte[] bytes = {1, 2, 3, 4, 5, 6};
    private static final String REASON_PREFIX = "Reasons for decision: \n";

    @MockBean
    private UnsecuredDocumentManagementService documentManagementService;
    @MockBean
    private DocumentGeneratorService documentGeneratorService;
    @MockBean
    private ListGeneratorService listGeneratorService;
    @Autowired
    private DismissalOrderGenerator dismissalOrderGenerator;

    @Test
    void shouldGenerateDismissalOrderDocument() {
        CaseData caseData = CaseDataBuilder.builder().dismissalOrderApplication().build();

        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class), eq(DISMISSAL_ORDER)))
            .thenReturn(new DocmosisDocument(DISMISSAL_ORDER.getDocumentTitle(), bytes));

        when(listGeneratorService.applicationType(caseData)).thenReturn("Extend time");
        when(listGeneratorService.claimantsName(caseData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
        when(listGeneratorService.defendantsName(caseData)).thenReturn("Test Defendant1 Name, Test Defendant2 Name");

        dismissalOrderGenerator.generate(caseData, BEARER_TOKEN);

        verify(documentManagementService).uploadDocument(
            BEARER_TOKEN,
            new PDF(any(), any(), DocumentType.DISMISSAL_ORDER)
        );
        verify(documentGeneratorService).generateDocmosisDocument(any(JudgeDecisionPdfDocument.class),
                                                                  eq(DISMISSAL_ORDER));
    }

    @Nested
    class GetTemplateData {

        @Test
        void whenJudgeMakeDecision_ShouldGetDissmisalOrderData() {
            CaseData caseData = CaseDataBuilder.builder().dismissalOrderApplication().build().toBuilder()
                .build();

            when(listGeneratorService.applicationType(caseData)).thenReturn("Extend time");
            when(listGeneratorService.claimantsName(caseData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(caseData))
                .thenReturn("Test Defendant1 Name, Test Defendant2 Name");

            var templateData = dismissalOrderGenerator.getTemplateData(caseData);

            assertThatFieldsAreCorrect_DismissalOrder(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_DismissalOrder(JudgeDecisionPdfDocument templateData,
                                                               CaseData caseData) {
            Assertions.assertAll(
                "Dismissal Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getApplicantName(), caseData.getApplicantPartyName()),
                () -> assertEquals(templateData.getApplicationDate(), caseData.getCreatedDate().toLocalDate()),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(templateData.getJudicialByCourtsInitiative(), caseData
                    .getJudicialDecisionMakeOrder().getOrderCourtOwnInitiative()
                    + " ".concat(LocalDate.now().format(DATE_FORMATTER))),
                () -> assertEquals(templateData.getDismissalOrder(),
                                   caseData.getJudicialDecisionMakeOrder().getDismissalOrderText()));
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetDissmisalOrderData_Option2() {
            CaseData caseData = CaseDataBuilder.builder().dismissalOrderApplication().build().toBuilder()
                .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                           .dismissalOrderText("Test Dismissal")
                                                           .reasonForDecisionText("Test Reason")
                                                           .showReasonForDecision(YesOrNo.YES)
                                                           .orderWithoutNotice("abcdef")
                                                           .orderWithoutNoticeDate(LocalDate.now())
                                                           .judicialByCourtsInitiative(
                                                               GAByCourtsInitiativeGAspec.OPTION_2)
                                                           .makeAnOrder(DISMISS_THE_APPLICATION)
                                                           .build()).build();
            CaseData updateData = caseDataBuilder.build();

            when(listGeneratorService.applicationType(updateData)).thenReturn("Extend time");
            when(listGeneratorService.claimantsName(updateData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(updateData))
                .thenReturn("Test Defendant1 Name, Test Defendant2 Name");

            var templateData = dismissalOrderGenerator.getTemplateData(updateData);

            assertThatFieldsAreCorrect_DismissalOrder_Option2(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_DismissalOrder_Option2(JudgeDecisionPdfDocument templateData,
                                                               CaseData caseData) {
            Assertions.assertAll(
                "Dismissal Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getApplicantName(), caseData.getApplicantPartyName()),
                () -> assertEquals(templateData.getApplicationDate(), caseData.getCreatedDate().toLocalDate()),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(templateData.getJudicialByCourtsInitiative(), caseData
                    .getJudicialDecisionMakeOrder().getOrderWithoutNotice()
                    + " ".concat(LocalDate.now().format(DATE_FORMATTER))),
                () -> assertEquals(templateData.getDismissalOrder(),
                                   caseData.getJudicialDecisionMakeOrder().getDismissalOrderText()),
                () -> assertEquals(templateData.getReasonForDecision(),
                    REASON_PREFIX + caseData.getJudicialDecisionMakeOrder().getReasonForDecisionText()));
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetDissmisalOrderData_Option3() {
            CaseData caseData = CaseDataBuilder.builder().dismissalOrderApplication().build().toBuilder()
                .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                          .dismissalOrderText("Test Dismissal")
                                                          .showReasonForDecision(YesOrNo.YES)
                                                          .reasonForDecisionText("Test Reason")
                                                          .judicialByCourtsInitiative(
                                                              GAByCourtsInitiativeGAspec.OPTION_3)
                                                          .makeAnOrder(DISMISS_THE_APPLICATION)
                                                          .build()).build();
            CaseData updateData = caseDataBuilder.build();

            when(listGeneratorService.applicationType(updateData)).thenReturn("Extend time");
            when(listGeneratorService.claimantsName(updateData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(updateData))
                .thenReturn("Test Defendant1 Name, Test Defendant2 Name");

            var templateData = dismissalOrderGenerator.getTemplateData(updateData);

            assertThatFieldsAreCorrect_DismissalOrder_Option3(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_DismissalOrder_Option3(JudgeDecisionPdfDocument templateData,
                                                               CaseData caseData) {
            Assertions.assertAll(
                "Dismissal Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getApplicationType(), getApplicationType(caseData)),
                () -> assertEquals(templateData.getApplicantName(), caseData.getApplicantPartyName()),
                () -> assertEquals(templateData.getApplicationDate(), caseData.getCreatedDate().toLocalDate()),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(StringUtils.EMPTY, templateData.getJudicialByCourtsInitiative()),
                () -> assertEquals(templateData.getDismissalOrder(),
                                   caseData.getJudicialDecisionMakeOrder().getDismissalOrderText()),
                () -> assertEquals(templateData.getReasonForDecision(),
                        REASON_PREFIX + caseData.getJudicialDecisionMakeOrder().getReasonForDecisionText()));
        }

        @Test
        void whenJudgeMakeDecision_ShouldHideText_whileUnchecked() {
            CaseData caseData = CaseDataBuilder.builder().dismissalOrderApplication().build().toBuilder()
                    .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                    .dismissalOrderText("Test Dismissal")
                    .showReasonForDecision(YesOrNo.NO)
                    .reasonForDecisionText("Test Reason")
                    .judicialByCourtsInitiative(
                            GAByCourtsInitiativeGAspec.OPTION_3)
                    .makeAnOrder(DISMISS_THE_APPLICATION)
                    .build()).build();
            CaseData updateData = caseDataBuilder.build();

            when(listGeneratorService.applicationType(updateData)).thenReturn("Extend time");
            when(listGeneratorService.claimantsName(updateData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(updateData))
                    .thenReturn("Test Defendant1 Name, Test Defendant2 Name");

            var templateData = dismissalOrderGenerator.getTemplateData(updateData);

            assertEquals("", templateData.getReasonForDecision());
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
