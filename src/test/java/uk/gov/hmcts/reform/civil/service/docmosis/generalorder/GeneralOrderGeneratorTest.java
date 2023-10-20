package uk.gov.hmcts.reform.civil.service.docmosis.generalorder;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.reform.civil.enums.dq.FinalOrderShowToggle;
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.ListGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.GENERAL_ORDER;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.DATE_FORMATTER;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    GeneralOrderGenerator.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class GeneralOrderGeneratorTest {

    private static final String BEARER_TOKEN = "Bearer Token";
    private static final byte[] bytes = {1, 2, 3, 4, 5, 6};

    @MockBean
    private UnsecuredDocumentManagementService documentManagementService;
    @MockBean
    private DocumentGeneratorService documentGeneratorService;
    @MockBean
    ListGeneratorService listGeneratorService;
    @Autowired
    private GeneralOrderGenerator generalOrderGenerator;
    @Autowired
    private ObjectMapper mapper;
    @MockBean
    private IdamClient idamClient;
    @MockBean
    private DocmosisService docmosisService;

    @Test
    void shouldGenerateGeneralOrderDocument() {
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication().build();

        when(idamClient.getUserDetails(any()))
            .thenReturn(UserDetails.builder().surname("Mark").forename("Joe").build());
        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class), eq(GENERAL_ORDER)))
            .thenReturn(new DocmosisDocument(GENERAL_ORDER.getDocumentTitle(), bytes));
        when(listGeneratorService.claimantsName(caseData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
        when(listGeneratorService.defendantsName(caseData)).thenReturn("Test Defendant1 Name, Test Defendant2 Name");

        generalOrderGenerator.generate(caseData, BEARER_TOKEN);

        verify(documentManagementService).uploadDocument(
            BEARER_TOKEN,
            new PDF(any(), any(), DocumentType.GENERAL_ORDER)
        );
        verify(documentGeneratorService).generateDocmosisDocument(any(JudgeDecisionPdfDocument.class),
                                                                  eq(GENERAL_ORDER));
    }

    @Nested
    class GetTemplateData {

        @Test
        void whenJudgeMakeDecision_ShouldGetGeneralOrderData() {
            CaseData caseData = CaseDataBuilder.builder().generalOrderApplication().build().toBuilder()
                .build();
            when(listGeneratorService.claimantsName(caseData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(caseData))
                .thenReturn("Test Defendant1 Name, Test Defendant2 Name");

            when(docmosisService.reasonAvailable(any())).thenReturn(YesOrNo.YES);
            when(docmosisService.populateJudgeReason(any())).thenReturn("Test Reason");
            when(docmosisService.populateJudicialByCourtsInitiative(any()))
                .thenReturn("abcd ".concat(LocalDate.now().format(DATE_FORMATTER)));

            var templateData = generalOrderGenerator.getTemplateData(caseData);

            assertThatFieldsAreCorrect_GeneralOrder(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_GeneralOrder(JudgeDecisionPdfDocument templateData, CaseData caseData) {
            Assertions.assertAll(
                "GeneralOrderDocument data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getGeneralOrder(),
                                   caseData.getJudicialDecisionMakeOrder().getOrderText()),
                () -> assertEquals(YesOrNo.YES, templateData.getReasonAvailable()),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(templateData.getJudicialByCourtsInitiative(), caseData
                    .getJudicialDecisionMakeOrder().getOrderCourtOwnInitiative()
                    + " ".concat(LocalDate.now().format(DATE_FORMATTER))),
                () -> assertEquals(templateData.getJudgeRecital(),
                                   caseData.getJudicialDecisionMakeOrder().getJudgeRecitalText())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetGeneralOrderData_Option2() {
            CaseData caseData = CaseDataBuilder.builder().generalOrderApplication().build().toBuilder()
                .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                          .orderText("Test Order")
                                                          .orderWithoutNotice("abcdef")
                                                          .orderWithoutNoticeDate(LocalDate.now())
                                                          .judicialByCourtsInitiative(
                                                              GAByCourtsInitiativeGAspec.OPTION_2)
                                                          .showReasonForDecision(YesOrNo.YES)
                                                          .reasonForDecisionText("Test Reason")
                                                          .makeAnOrder(
                                                              GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT)
                                                          .showJudgeRecitalText(List.of(FinalOrderShowToggle.SHOW))
                                                          .judgeRecitalText("Test Judge's recital")
                                                          .build()).build();
            CaseData updateData = caseDataBuilder.build();

            when(listGeneratorService.claimantsName(updateData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(updateData))
                .thenReturn("Test Defendant1 Name, Test Defendant2 Name");
            when(docmosisService.reasonAvailable(any())).thenReturn(YesOrNo.YES);
            when(docmosisService.populateJudgeReason(any())).thenReturn("Test Reason");
            when(docmosisService.populateJudicialByCourtsInitiative(any()))
                .thenReturn("abcdef ".concat(LocalDate.now().format(DATE_FORMATTER)));

            var templateData = generalOrderGenerator.getTemplateData(updateData);

            assertThatFieldsAreCorrect_GeneralOrder_Option2(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_GeneralOrder_Option2(JudgeDecisionPdfDocument templateData,
                                                                     CaseData caseData) {
            Assertions.assertAll(
                "GeneralOrderDocument data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getGeneralOrder(),
                                   caseData.getJudicialDecisionMakeOrder().getOrderText()),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(templateData.getJudicialByCourtsInitiative(), caseData
                    .getJudicialDecisionMakeOrder().getOrderWithoutNotice()
                    + " ".concat(LocalDate.now().format(DATE_FORMATTER))),
                () -> assertEquals(YesOrNo.YES, templateData.getReasonAvailable()),
                () -> assertEquals(templateData.getJudgeRecital(),
                                   caseData.getJudicialDecisionMakeOrder().getJudgeRecitalText()),
                () -> assertEquals(templateData.getReasonForDecision(),
                        caseData.getJudicialDecisionMakeOrder().getReasonForDecisionText())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetGeneralOrderData_Option3() {
            CaseData caseData = CaseDataBuilder.builder().generalOrderApplication().build().toBuilder()
                .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                          .orderText("Test Order")
                                                          .judicialByCourtsInitiative(
                                                              GAByCourtsInitiativeGAspec.OPTION_3)
                                                          .showReasonForDecision(YesOrNo.YES)
                                                          .reasonForDecisionText("Test Reason")
                                                          .makeAnOrder(
                                                              GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT)
                                                          .showJudgeRecitalText(List.of(FinalOrderShowToggle.SHOW))
                                                          .judgeRecitalText("Test Judge's recital")
                                                          .build()).build();
            CaseData updateData = caseDataBuilder.build();

            when(listGeneratorService.claimantsName(updateData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(updateData))
                .thenReturn("Test Defendant1 Name, Test Defendant2 Name");
            when(docmosisService.reasonAvailable(any())).thenReturn(YesOrNo.YES);
            when(docmosisService.populateJudgeReason(any())).thenReturn("Test Reason");
            when(docmosisService.populateJudicialByCourtsInitiative(any()))
                .thenReturn(StringUtils.EMPTY);

            var templateData = generalOrderGenerator.getTemplateData(updateData);

            assertThatFieldsAreCorrect_GeneralOrder_Option3(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_GeneralOrder_Option3(JudgeDecisionPdfDocument templateData,
                                                                     CaseData caseData) {
            Assertions.assertAll(
                "GeneralOrderDocument data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimantName(), getClaimats(caseData)),
                () -> assertEquals(templateData.getDefendantName(), getDefendats(caseData)),
                () -> assertEquals(templateData.getGeneralOrder(),
                                   caseData.getJudicialDecisionMakeOrder().getOrderText()),
                () -> assertEquals(YesOrNo.YES, templateData.getReasonAvailable()),
                () -> assertEquals(templateData.getLocationName(), caseData.getLocationName()),
                () -> assertEquals(StringUtils.EMPTY, templateData.getJudicialByCourtsInitiative()),
                () -> assertEquals(templateData.getJudgeRecital(),
                                   caseData.getJudicialDecisionMakeOrder().getJudgeRecitalText()),
                () -> assertEquals(templateData.getReasonForDecision(),
                                   caseData.getJudicialDecisionMakeOrder().getReasonForDecisionText())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldHideText_whileUnchecked() {
            CaseData caseData = CaseDataBuilder.builder().generalOrderApplication().build().toBuilder()
                    .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                    .orderText("Test Order")
                    .judicialByCourtsInitiative(
                            GAByCourtsInitiativeGAspec.OPTION_3)
                    .reasonForDecisionText("Test Reason")
                    .showReasonForDecision(YesOrNo.NO)
                    .makeAnOrder(
                            GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT)
                    .judgeRecitalText("Test Judge's recital")
                    .build()).build();
            CaseData updateData = caseDataBuilder.build();

            when(listGeneratorService.applicationType(updateData)).thenReturn("Extend time");
            when(listGeneratorService.claimantsName(updateData)).thenReturn("Test Claimant1 Name, Test Claimant2 Name");
            when(listGeneratorService.defendantsName(updateData))
                    .thenReturn("Test Defendant1 Name, Test Defendant2 Name");
            when(docmosisService.populateJudgeReason(any())).thenReturn(StringUtils.EMPTY);

            var templateData = generalOrderGenerator.getTemplateData(updateData);

            assertNull(templateData.getJudgeRecital());
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
    }
}
