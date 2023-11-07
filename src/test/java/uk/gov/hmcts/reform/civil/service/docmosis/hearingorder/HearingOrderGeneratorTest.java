package uk.gov.hmcts.reform.civil.service.docmosis.hearingorder;

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
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.HEARING_ORDER;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.DATE_FORMATTER;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    HearingOrderGenerator.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class HearingOrderGeneratorTest {

    private static final String BEARER_TOKEN = "Bearer Token";
    private static final byte[] bytes = {1, 2, 3, 4, 5, 6};

    @MockBean
    private UnsecuredDocumentManagementService documentManagementService;
    @MockBean
    private DocumentGeneratorService documentGeneratorService;
    @Autowired
    private HearingOrderGenerator hearingOrderGenerator;
    @MockBean
    private IdamClient idamClient;

    @Test
    void shouldGenerateHearingOrderDocument() {
        CaseData caseData = CaseDataBuilder.builder().hearingOrderApplication(YesOrNo.NO, YesOrNo.NO).build();

        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class), eq(HEARING_ORDER)))
            .thenReturn(new DocmosisDocument(HEARING_ORDER.getDocumentTitle(), bytes));
        when(idamClient.getUserDetails(any()))
            .thenReturn(UserDetails.builder().surname("Mark").forename("Joe").build());

        hearingOrderGenerator.generate(caseData, BEARER_TOKEN);

        verify(documentManagementService).uploadDocument(
            BEARER_TOKEN,
            new PDF(any(), any(), DocumentType.HEARING_ORDER)
        );
        verify(documentGeneratorService).generateDocmosisDocument(any(JudgeDecisionPdfDocument.class),
                                                                  eq(HEARING_ORDER));
    }

    @Nested
    class GetTemplateData {

        @Test
        void whenJudgeMakeDecision_ShouldGetHearingOrderData() {
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(YES)
                .build();

            var templateData = hearingOrderGenerator.getTemplateData(caseData);

            assertThatFieldsAreCorrect_HearingOrder(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_HearingOrder(JudgeDecisionPdfDocument templateData, CaseData caseData) {
            Assertions.assertAll(
                "Hearing Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), caseData.getClaimant2PartyName()),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(templateData.getIsMultiParty(), YES),
                () -> assertEquals(templateData.getDefendant2Name(), caseData.getDefendant2PartyName()),
                () -> assertEquals(templateData.getHearingPrefType(), caseData.getJudicialListForHearing()
                    .getHearingPreferencesPreferredType().getDisplayedValue()),
                () -> assertEquals(templateData.getJudicialByCourtsInitiativeListForHearing(), caseData
                    .getOrderCourtOwnInitiativeListForHearing().getOrderCourtOwnInitiative()
                    + " ".concat(caseData.getOrderCourtOwnInitiativeListForHearing()
                                     .getOrderCourtOwnInitiativeDate().format(DATE_FORMATTER))),
                () -> assertEquals(templateData.getEstimatedHearingLength(),
                                   caseData.getJudicialListForHearing().getJudicialTimeEstimate().getDisplayedValue()),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudicialGeneralHearingOrderRecital()),
                () -> assertEquals(templateData.getHearingOrder(), caseData.getJudicialGOHearingDirections())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetHearingOrderData_Option2() {
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(NO)
                .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialByCourtsInitiativeListForHearing(GAByCourtsInitiativeGAspec.OPTION_2)
                .orderCourtOwnInitiativeListForHearing(GAOrderCourtOwnInitiativeGAspec.builder().build())
                .orderWithoutNoticeListForHearing(GAOrderWithoutNoticeGAspec
                                                           .builder()
                                                           .orderWithoutNotice("abcd")
                                                           .orderWithoutNoticeDate(LocalDate.now()).build()).build();

            CaseData updateData = caseDataBuilder.build();
            var templateData = hearingOrderGenerator.getTemplateData(updateData);

            assertThatFieldsAreCorrect_HearingOrder_Option2(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_HearingOrder_Option2(JudgeDecisionPdfDocument templateData,
                                                                     CaseData caseData) {
            Assertions.assertAll(
                "Hearing Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), caseData.getClaimant2PartyName()),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(templateData.getDefendant2Name(), caseData.getDefendant2PartyName()),
                () -> assertEquals(templateData.getIsMultiParty(), NO),
                () -> assertEquals(templateData.getHearingPrefType(), caseData.getJudicialListForHearing()
                    .getHearingPreferencesPreferredType().getDisplayedValue()),
                () -> assertEquals(templateData.getJudicialByCourtsInitiativeListForHearing(), caseData
                    .getOrderWithoutNoticeListForHearing().getOrderWithoutNotice()
                    + " ".concat(caseData.getOrderWithoutNoticeListForHearing()
                                     .getOrderWithoutNoticeDate().format(DATE_FORMATTER))),
                () -> assertEquals(templateData.getEstimatedHearingLength(),
                                   caseData.getJudicialListForHearing().getJudicialTimeEstimate().getDisplayedValue()),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudicialGeneralHearingOrderRecital()),
                () -> assertEquals(templateData.getHearingOrder(), caseData.getJudicialGOHearingDirections())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetHearingOrderData_Option3() {
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(NO)
                .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialByCourtsInitiativeListForHearing(GAByCourtsInitiativeGAspec.OPTION_3)
                .orderCourtOwnInitiativeListForHearing(GAOrderCourtOwnInitiativeGAspec.builder().build())
                .orderWithoutNoticeListForHearing(GAOrderWithoutNoticeGAspec
                                                      .builder().build()).build();

            CaseData updateData = caseDataBuilder.build();

            var templateData = hearingOrderGenerator.getTemplateData(updateData);

            assertThatFieldsAreCorrect_HearingOrder_Option3(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_HearingOrder_Option3(JudgeDecisionPdfDocument templateData,
                                                                     CaseData caseData) {
            Assertions.assertAll(
                "Hearing Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), caseData.getClaimant2PartyName()),
                () -> assertEquals(templateData.getIsMultiParty(), NO),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(templateData.getDefendant2Name(), caseData.getDefendant2PartyName()),
                () -> assertEquals(templateData.getHearingPrefType(), caseData.getJudicialListForHearing()
                    .getHearingPreferencesPreferredType().getDisplayedValue()),
                () -> assertEquals(StringUtils.EMPTY, templateData.getJudicialByCourtsInitiativeListForHearing()),
                () -> assertEquals(templateData.getEstimatedHearingLength(),
                                   caseData.getJudicialListForHearing().getJudicialTimeEstimate().getDisplayedValue()),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudicialGeneralHearingOrderRecital()),
                () -> assertEquals(templateData.getHearingOrder(), caseData.getJudicialGOHearingDirections())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetHearingOrderData_Option3_1v1() {
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(YES)
                .defendant2PartyName(null)
                .claimant2PartyName(null)
                .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialByCourtsInitiativeListForHearing(GAByCourtsInitiativeGAspec.OPTION_3)
                .orderCourtOwnInitiativeListForHearing(GAOrderCourtOwnInitiativeGAspec.builder().build())
                .orderWithoutNoticeListForHearing(GAOrderWithoutNoticeGAspec
                                                      .builder().build()).build();

            CaseData updateData = caseDataBuilder.build();

            var templateData = hearingOrderGenerator.getTemplateData(updateData);

            assertThatFieldsAreCorrect_HearingOrder_Option3_1v1(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_HearingOrder_Option3_1v1(JudgeDecisionPdfDocument templateData,
                                                                     CaseData caseData) {
            Assertions.assertAll(
                "Hearing Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), caseData.getClaimant2PartyName()),
                () -> assertEquals(templateData.getIsMultiParty(), YES),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(templateData.getDefendant2Name(), caseData.getDefendant2PartyName()),
                () -> assertEquals(templateData.getHearingPrefType(), caseData.getJudicialListForHearing()
                    .getHearingPreferencesPreferredType().getDisplayedValue()),
                () -> assertEquals(StringUtils.EMPTY, templateData.getJudicialByCourtsInitiativeListForHearing()),
                () -> assertEquals(templateData.getEstimatedHearingLength(),
                                   caseData.getJudicialListForHearing().getJudicialTimeEstimate().getDisplayedValue()),
                () -> assertEquals(templateData.getJudgeRecital(), caseData.getJudicialGeneralHearingOrderRecital()),
                () -> assertEquals(templateData.getHearingOrder(), caseData.getJudicialGOHearingDirections())
            );
        }
    }
}
