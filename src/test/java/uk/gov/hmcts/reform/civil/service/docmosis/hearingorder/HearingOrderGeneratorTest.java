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
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingDuration;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.model.genapplication.GACaseLocation;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAOrderCourtOwnInitiativeGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAOrderWithoutNoticeGAspec;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.SecuredDocumentManagementService;
import uk.gov.hmcts.reform.civil.service.flowstate.FlowFlag;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.HEARING_ORDER;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.POST_JUDGE_HEARING_ORDER_LIP;
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
    private SecuredDocumentManagementService documentManagementService;
    @MockBean
    private DocumentGeneratorService documentGeneratorService;
    @Autowired
    private HearingOrderGenerator hearingOrderGenerator;
    @MockBean
    private DocmosisService docmosisService;

    @Test
    void shouldGenerateHearingOrderDocument() {

        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class), eq(HEARING_ORDER)))
            .thenReturn(new DocmosisDocument(HEARING_ORDER.getDocumentTitle(), bytes));
        when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
            .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("London").build());
        CaseData caseData = CaseDataBuilder.builder().hearingOrderApplication(YesOrNo.NO, YesOrNo.NO).build();

        hearingOrderGenerator.generate(caseData, BEARER_TOKEN);

        verify(documentManagementService).uploadDocument(
            BEARER_TOKEN,
            new PDF(any(), any(), DocumentType.HEARING_ORDER)
        );
        verify(documentGeneratorService).generateDocmosisDocument(any(JudgeDecisionPdfDocument.class),
                                                                  eq(HEARING_ORDER));
    }

    @Test
    void shouldThrowExceptionWhenNoLocationMatch() {

        CaseData caseData = CaseDataBuilder.builder()
            .hearingOrderApplication(YesOrNo.NO, YesOrNo.NO)
            .caseManagementLocation(GACaseLocation.builder().baseLocation("8").build())
            .build();

        when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class),
                                                               eq(HEARING_ORDER)))
            .thenReturn(new DocmosisDocument(HEARING_ORDER.getDocumentTitle(), bytes));
        doThrow(new IllegalArgumentException("Court Name is not found in location data"))
            .when(docmosisService).getCaseManagementLocationVenueName(any(), any());

        Exception exception =
            assertThrows(IllegalArgumentException.class, () -> hearingOrderGenerator.generate(caseData, BEARER_TOKEN));
        String expectedMessage = "Court Name is not found in location data";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Nested
    class GetTemplateDataLip {
        @Test
        void shouldGenerateHearingOrderDocument() {

            when(documentGeneratorService.generateDocmosisDocument(any(MappableObject.class), eq(POST_JUDGE_HEARING_ORDER_LIP)))
                .thenReturn(new DocmosisDocument(POST_JUDGE_HEARING_ORDER_LIP.getDocumentTitle(), bytes));
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("London").build());
            CaseData caseData = CaseDataBuilder.builder().hearingOrderApplication(YesOrNo.NO, YesOrNo.NO).build();

            hearingOrderGenerator.generate(CaseDataBuilder.builder().getCivilCaseData(),
                                           caseData,
                                           BEARER_TOKEN, FlowFlag.POST_JUDGE_ORDER_LIP_APPLICANT);

            verify(documentManagementService).uploadDocument(
                BEARER_TOKEN,
                new PDF(any(), any(), DocumentType.HEARING_ORDER)
            );
            verify(documentGeneratorService).generateDocmosisDocument(any(JudgeDecisionPdfDocument.class),
                                                                      eq(POST_JUDGE_HEARING_ORDER_LIP));
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetHearingOrderData() {
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("Reading").build());
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(YES)
                .parentClaimantIsApplicant(NO)
                .build();

            var templateData = hearingOrderGenerator.getTemplateData(CaseDataBuilder.builder().getCivilCaseData(),
                                                                     caseData,
                                                                     "auth",
                                                                     FlowFlag.POST_JUDGE_ORDER_LIP_RESPONDENT);

            assertThatFieldsAreCorrect_HearingOrder(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_HearingOrder(JudgeDecisionPdfDocument templateData, CaseData caseData) {
            Assertions.assertAll(
                "Hearing Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getJudgeHearingLocation(), "sitename - location name - D12 8997"),
                () -> assertEquals(templateData.getJudgeNameTitle(), caseData.getJudgeTitle()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), caseData.getClaimant2PartyName()),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(YES, templateData.getIsMultiParty()),
                () -> assertEquals(templateData.getCourtName(), "Reading"),
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
                () -> assertEquals(templateData.getHearingOrder(), caseData.getJudicialGOHearingDirections()),
                () -> assertEquals(templateData.getAddress(), caseData.getCaseManagementLocation().getAddress()),
                () -> assertEquals(templateData.getSiteName(), caseData.getCaseManagementLocation().getSiteName()),
                () -> assertEquals(templateData.getPostcode(), caseData.getCaseManagementLocation().getPostcode()),
                () -> assertEquals("applicant1partyname", templateData.getPartyName()),
                () -> assertEquals("address1", templateData.getPartyAddressAddressLine1()),
                () -> assertEquals("address2", templateData.getPartyAddressAddressLine2()),
                () -> assertEquals("address3", templateData.getPartyAddressAddressLine3()),
                () -> assertEquals("posttown", templateData.getPartyAddressPostTown()),
                () -> assertEquals("postcode", templateData.getPartyAddressPostCode()));
        }
    }

    @Nested
    class GetTemplateData {

        @Test
        void whenJudgeMakeDecision_ShouldGetHearingOrderData() {
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("Reading").build());
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(YES)
                .build();

            var templateData = hearingOrderGenerator.getTemplateData(null, caseData, "auth", FlowFlag.ONE_RESPONDENT_REPRESENTATIVE);

            assertThatFieldsAreCorrect_HearingOrder(templateData, caseData);
        }

        private void assertThatFieldsAreCorrect_HearingOrder(JudgeDecisionPdfDocument templateData, CaseData caseData) {
            Assertions.assertAll(
                "Hearing Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getJudgeHearingLocation(), "sitename - location name - D12 8997"),
                () -> assertEquals(templateData.getJudgeNameTitle(), caseData.getJudgeTitle()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), caseData.getClaimant2PartyName()),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(YES, templateData.getIsMultiParty()),
                () -> assertEquals(templateData.getCourtName(), "Reading"),
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
                () -> assertEquals(templateData.getHearingOrder(), caseData.getJudicialGOHearingDirections()),
                () -> assertEquals(templateData.getAddress(), caseData.getCaseManagementLocation().getAddress()),
                () -> assertEquals(templateData.getSiteName(), caseData.getCaseManagementLocation().getSiteName()),
                () -> assertEquals(templateData.getPostcode(), caseData.getCaseManagementLocation().getPostcode())
            );
        }

        @Test
        void whenJudgeMakeDecision_ShouldGetHearingOrderData_Option2() {
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("Manchester").build());
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(NO)
                .judicialListForHearing(GAJudgesHearingListGAspec.builder()
                                            .hearingPreferencesPreferredType(GAJudicialHearingType.PAPER_HEARING)
                                            .judicialTimeEstimate(GAHearingDuration.MINUTES_15)
                                            .build())
                .caseManagementLocation(GACaseLocation.builder().baseLocation("3").build())
                .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialByCourtsInitiativeListForHearing(GAByCourtsInitiativeGAspec.OPTION_2)
                .orderCourtOwnInitiativeListForHearing(GAOrderCourtOwnInitiativeGAspec.builder().build())
                .orderWithoutNoticeListForHearing(GAOrderWithoutNoticeGAspec
                                                           .builder()
                                                           .orderWithoutNotice("abcd")
                                                           .orderWithoutNoticeDate(LocalDate.now()).build()).build();

            CaseData updateData = caseDataBuilder.build();
            var templateData = hearingOrderGenerator.getTemplateData(null, updateData, "auth", FlowFlag.ONE_RESPONDENT_REPRESENTATIVE);

            assertThatFieldsAreCorrect_HearingOrder_Option2(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_HearingOrder_Option2(JudgeDecisionPdfDocument templateData,
                                                                     CaseData caseData) {
            Assertions.assertAll(
                "Hearing Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getJudgeNameTitle(), caseData.getJudgeTitle()),
                () -> assertEquals(templateData.getJudgeHearingLocation(), null),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), caseData.getClaimant2PartyName()),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(templateData.getDefendant2Name(), caseData.getDefendant2PartyName()),
                () -> assertEquals(NO, templateData.getIsMultiParty()),
                () -> assertEquals(templateData.getCourtName(), "Manchester"),
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
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("London").build());
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(NO)
                .caseManagementLocation(GACaseLocation.builder().baseLocation("2").build())
                .build();

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder.judicialByCourtsInitiativeListForHearing(GAByCourtsInitiativeGAspec.OPTION_3)
                .orderCourtOwnInitiativeListForHearing(GAOrderCourtOwnInitiativeGAspec.builder().build())
                .orderWithoutNoticeListForHearing(GAOrderWithoutNoticeGAspec
                                                      .builder().build()).build();

            CaseData updateData = caseDataBuilder.build();

            var templateData = hearingOrderGenerator.getTemplateData(null, updateData, "auth", FlowFlag.ONE_RESPONDENT_REPRESENTATIVE);

            assertThatFieldsAreCorrect_HearingOrder_Option3(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_HearingOrder_Option3(JudgeDecisionPdfDocument templateData,
                                                                     CaseData caseData) {
            Assertions.assertAll(
                "Hearing Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getJudgeNameTitle(), caseData.getJudgeTitle()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), caseData.getClaimant2PartyName()),
                () -> assertEquals(NO, templateData.getIsMultiParty()),
                () -> assertEquals(templateData.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertEquals(templateData.getCourtName(), "London"),
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
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("Reading").build());
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

            var templateData = hearingOrderGenerator.getTemplateData(null, updateData, "auth", FlowFlag.ONE_RESPONDENT_REPRESENTATIVE);

            assertThatFieldsAreCorrect_HearingOrder_Option3_1v1(templateData, updateData);
        }

        private void assertThatFieldsAreCorrect_HearingOrder_Option3_1v1(JudgeDecisionPdfDocument templateData,
                                                                     CaseData caseData) {
            Assertions.assertAll(
                "Hearing Order Document data should be as expected",
                () -> assertEquals(templateData.getClaimNumber(), caseData.getCcdCaseReference().toString()),
                () -> assertEquals(templateData.getJudgeNameTitle(), caseData.getJudgeTitle()),
                () -> assertEquals(templateData.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(templateData.getClaimant2Name(), caseData.getClaimant2PartyName()),
                () -> assertEquals(YES, templateData.getIsMultiParty()),
                () -> assertEquals(templateData.getCourtName(), "Reading"),
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
        void whenJudgeSpecifiesOtherHearingDuration1_ShouldGetHearingOrderData() {
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("Reading").build());
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(YES)
                .build();
            caseData = caseData.toBuilder()
                .judicialListForHearing(caseData.getJudicialListForHearing().toBuilder()
                                            .judicialTimeEstimate(GAHearingDuration.OTHER)
                                            .judicialTimeEstimateDays("2")
                                            .judicialTimeEstimateHours("2")
                                            .judicialTimeEstimateMinutes("30").build())
                .build();

            var templateData = hearingOrderGenerator.getTemplateData(null, caseData, "auth", FlowFlag.ONE_RESPONDENT_REPRESENTATIVE);

            assertEquals("2 days, 2 hours and 30 minutes", templateData.getEstimatedHearingLength());
        }

        @Test
        void whenJudgeSpecifiesOtherHearingDuration2_ShouldGetHearingOrderData() {
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("Reading").build());
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(YES)
                .build();
            caseData = caseData.toBuilder()
                .judicialListForHearing(caseData.getJudicialListForHearing().toBuilder()
                                            .judicialTimeEstimate(GAHearingDuration.OTHER)
                                            .judicialTimeEstimateDays("1")
                                            .judicialTimeEstimateMinutes("30").build())
                .build();

            var templateData = hearingOrderGenerator.getTemplateData(null, caseData, "auth", FlowFlag.ONE_RESPONDENT_REPRESENTATIVE);

            assertEquals("1 day and 30 minutes", templateData.getEstimatedHearingLength());
        }

        @Test
        void whenJudgeSpecifiesOtherHearingDuration3_ShouldGetHearingOrderData() {
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("Reading").build());
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(YES)
                .build();
            caseData = caseData.toBuilder()
                .judicialListForHearing(caseData.getJudicialListForHearing().toBuilder()
                                            .judicialTimeEstimate(GAHearingDuration.OTHER)
                                            .judicialTimeEstimateDays("2")
                                            .judicialTimeEstimateHours("6").build())
                .build();

            var templateData = hearingOrderGenerator.getTemplateData(null, caseData, "auth", FlowFlag.ONE_RESPONDENT_REPRESENTATIVE);

            assertEquals("2 days and 6 hours", templateData.getEstimatedHearingLength());
        }

        @Test
        void whenJudgeSpecifiesOtherHearingDuration4_ShouldGetHearingOrderData() {
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("Reading").build());
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(YES)
                .build();
            caseData = caseData.toBuilder()
                .judicialListForHearing(caseData.getJudicialListForHearing().toBuilder()
                                            .judicialTimeEstimate(GAHearingDuration.OTHER)
                                            .judicialTimeEstimateHours("2")
                                            .judicialTimeEstimateMinutes("30").build())
                .build();

            var templateData = hearingOrderGenerator.getTemplateData(null, caseData, "auth", FlowFlag.ONE_RESPONDENT_REPRESENTATIVE);

            assertEquals("2 hours and 30 minutes", templateData.getEstimatedHearingLength());
        }

        @Test
        void whenJudgeSpecifiesOtherHearingDuration5_ShouldGetHearingOrderData() {
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("Reading").build());
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(YES)
                .build();
            caseData = caseData.toBuilder()
                .judicialListForHearing(caseData.getJudicialListForHearing().toBuilder()
                                            .judicialTimeEstimate(GAHearingDuration.OTHER)
                                            .judicialTimeEstimateHours("6").build())
                .build();

            var templateData = hearingOrderGenerator.getTemplateData(null, caseData, "auth", FlowFlag.ONE_RESPONDENT_REPRESENTATIVE);

            assertEquals("6 hours", templateData.getEstimatedHearingLength());
        }

        @Test
        void whenJudgeSpecifiesOtherHearingDuration6_ShouldGetHearingOrderData() {
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("Reading").build());
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(YES)
                .build();
            caseData = caseData.toBuilder()
                .judicialListForHearing(caseData.getJudicialListForHearing().toBuilder()
                                            .judicialTimeEstimate(GAHearingDuration.OTHER)
                                            .judicialTimeEstimateDays("3").build())
                .build();

            var templateData = hearingOrderGenerator.getTemplateData(null, caseData, "auth", FlowFlag.ONE_RESPONDENT_REPRESENTATIVE);

            assertEquals("3 days", templateData.getEstimatedHearingLength());
        }

        @Test
        void whenJudgeSpecifiesOtherHearingDuration7_ShouldGetHearingOrderData() {
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").externalShortName("Reading").build());
            CaseData caseData = CaseDataBuilder.builder()
                .hearingOrderApplication(YesOrNo.NO, YesOrNo.YES).build().toBuilder()
                .isMultiParty(YES)
                .build();
            caseData = caseData.toBuilder()
                .judicialListForHearing(caseData.getJudicialListForHearing().toBuilder()
                                            .judicialTimeEstimate(GAHearingDuration.OTHER)
                                            .judicialTimeEstimateMinutes("45").build())
                .build();

            var templateData = hearingOrderGenerator.getTemplateData(null, caseData, "auth", FlowFlag.ONE_RESPONDENT_REPRESENTATIVE);

            assertEquals("45 minutes", templateData.getEstimatedHearingLength());
        }
    }
}
