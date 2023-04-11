package uk.gov.hmcts.reform.civil.service.docmosis.finalorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.AppealOriginTypes;
import uk.gov.hmcts.reform.civil.enums.dq.AssistedCostTypesList;
import uk.gov.hmcts.reform.civil.enums.dq.ClaimantDefendantNotAttendingType;
import uk.gov.hmcts.reform.civil.enums.dq.ClaimantRepresentationType;
import uk.gov.hmcts.reform.civil.enums.dq.DefendantRepresentationType;
import uk.gov.hmcts.reform.civil.enums.dq.FinalOrderConsideredToggle;
import uk.gov.hmcts.reform.civil.enums.dq.FinalOrderShowToggle;
import uk.gov.hmcts.reform.civil.enums.dq.HeardFromRepresentationTypes;
import uk.gov.hmcts.reform.civil.enums.dq.LengthOfHearing;
import uk.gov.hmcts.reform.civil.enums.dq.OrderMadeOnTypes;
import uk.gov.hmcts.reform.civil.enums.dq.PermissionToAppealTypes;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;
import uk.gov.hmcts.reform.civil.model.genapplication.HearingLength;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.AssistedOrderAppealDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.AssistedOrderCost;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.AssistedOrderFurtherHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.AssistedOrderGiveReasonsDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.AssistedOrderHeardRepresentation;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.AssistedOrderMadeDateHeardDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.AssistedOrderRecitalRecord;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.ClaimantDefendantRepresentation;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.DetailText;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.DetailTextWithDate;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.HeardClaimantNotAttend;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.HeardDefendantNotAttend;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {
    AssistedOrderFormGenerator.class
})
class AssistedOrderFormGeneratorTest {

    private static final String ORDER_MADE_ON_NONE_TEXT = "This order was not made on the court’s own initiative"
        + " or without notice.";

    private static final String JUDGE_SATISFIED_TO_PROCEED_TEXT = ", but the Judge was satisfied that they had received"
        + " notice of the trial and it was reasonable to proceed in their absence.";
    private static final String JUDGE_SATISFIED_NOTICE_OF_TRIAL_TEXT = " and whilst the Judge was satisfied that they"
        + " had received notice of the trial it was not reasonable to proceed in their absence.";
    private static final String JUDGE_NOT_SATISFIED_NOTICE_OF_TRIAL_TEXT = ", but the Judge was not satisfied that they"
        + " had received notice of the hearing and it was not reasonable to proceed in their absence.";

    private static final String JUDGE_CONSIDERED_PAPERS_TEXT = "The judge considered the papers.";
    private static final String RECITAL_RECORDED_TEXT = "It is recorded that %s.";
    private static final String COST_IN_CASE_TEXT = "Costs in the case have been ordered.";
    private static final String NO_ORDER_COST_TEXT = "No order as to costs has been made.";
    private static final String TEST_TEXT = "Test 123";

    @MockBean
    private UnsecuredDocumentManagementService documentManagementService;

    @MockBean
    private DocumentGeneratorService documentGeneratorService;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @Autowired
    private AssistedOrderFormGenerator generator;
    private DetailText detailText;
    private AssistedOrderCost costDetails;

    @BeforeEach
    public void setUp() throws IOException {

        detailText = DetailText.builder().detailText(TEST_TEXT).build();
        costDetails = AssistedOrderCost.builder()
            .costAmount(new BigDecimal(123))
            .costPaymentDeadLine(LocalDate.now())
            .isPartyCostProtection(YesOrNo.YES)
            .build();
    }

    @Nested
    class CostTextValues {
        @Test
        void shouldReturnText_WhenSelected_CostInCaseOption() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.COSTS_IN_CASE).build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).contains(COST_IN_CASE_TEXT);
        }

        @Test
        void shouldReturnText_WhenSelected_NoOrderCostOption() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.NO_ORDER_TO_COST).build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).contains(NO_ORDER_COST_TEXT);
        }

        @Test
        void shouldReturnText_WhenSelected_CostReservedOption() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.COSTS_RESERVED)
                .costReservedDetails(detailText)
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).contains(TEST_TEXT);
        }

        @Test
        void shouldReturnText_WhenSelected_CostReservedOption_NoTextValue() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.COSTS_RESERVED)
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).isEmpty();
        }

        @Test
        void shouldReturnText_WhenSelected_DefendantStandardCostBase() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.DEFENDANT_COST_STANDARD_BASE)
                .defendantCostStandardBase(costDetails)
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).contains("To be paid by");
        }

        @Test
        void shouldReturnText_WhenSelected_DefendantStandardCostBase_NoCostDetails() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.DEFENDANT_COST_STANDARD_BASE)
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).contains("The defendant shall pay the");
        }

        @Test
        void shouldReturnText_WhenSelected_DefendantStandardCostBase_NoCostAmount() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.DEFENDANT_COST_STANDARD_BASE)
                .defendantCostStandardBase(AssistedOrderCost.builder()
                                               .costPaymentDeadLine(LocalDate.now())
                                               .isPartyCostProtection(YesOrNo.YES)
                                               .build())
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertFalse(assistedOrderString.contains("Amount:"));
        }

        @Test
        void shouldReturnText_WhenSelected_DefendantStandardCostBase_NoCostProtection() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.DEFENDANT_COST_STANDARD_BASE)
                .defendantCostStandardBase(AssistedOrderCost.builder()
                                               .costAmount(new BigDecimal(123))
                                               .costPaymentDeadLine(LocalDate.now())
                                               .isPartyCostProtection(YesOrNo.NO)
                                               .build())
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).contains("The paying party has the benefit of cost");
        }

        @Test
        void shouldReturnText_WhenSelected_DefendantStandardCostBase_NoDate() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.DEFENDANT_COST_STANDARD_BASE)
                .defendantCostStandardBase(AssistedOrderCost.builder().build())
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertFalse(assistedOrderString.contains("To be paid by"));
        }

        @Test
        void shouldReturnText_WhenSelected_ClaimantStandardCostBase() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.CLAIMANT_COST_STANDARD_BASE)
                .claimantCostStandardBase(costDetails)
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).contains("To be paid by");
        }

        @Test
        void shouldReturnText_WhenSelected_ClaimantStandardCostBase_NoCostDetails() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.CLAIMANT_COST_STANDARD_BASE)
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).contains("The claimant shall pay the");
        }

        @Test
        void shouldReturnText_WhenSelected_DefendantStandardSummarilyBase() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.DEFENDANT_COST_SUMMARILY_BASE)
                .defendantCostSummarilyBase(costDetails)
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).contains("To be paid by");
        }

        @Test
        void shouldReturnText_WhenSelected_DefendantStandardSummarilyBase_NoCostDetails() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.DEFENDANT_COST_SUMMARILY_BASE)
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).contains("The defendant shall pay the");
        }

        @Test
        void shouldReturnText_WhenSelected_ClaimantSummarilyBase() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.CLAIMANT_COST_SUMMARILY_BASE)
                .claimantCostSummarilyBase(costDetails)
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).contains("To be paid by");
        }

        @Test
        void shouldReturnText_WhenSelected_ClaimantSummarilyBase_NoCostDetails() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.CLAIMANT_COST_SUMMARILY_BASE)
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).contains("The claimant shall pay the");
        }

        @Test
        void shouldReturnText_WhenSelected_BespokeCostOrder() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.BESPOKE_COSTS_ORDER)
                .bespokeCostDetails(detailText)
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).contains(TEST_TEXT);
        }

        @Test
        void shouldReturnEmptyText_WhenSelected_BespokeCostOrder_NoDetails() {
            CaseData caseData = CaseData.builder()
                .assistedCostTypes(AssistedCostTypesList.BESPOKE_COSTS_ORDER)
                .build();
            String assistedOrderString = generator.getCostsTextValue(caseData);
            assertThat(assistedOrderString).isEmpty();
        }

    }

    @Nested
    class FurtherHearing {

        private List<FinalOrderShowToggle> furtherHearingShowOption = new ArrayList<>();

        @BeforeEach
        public void setUp() throws IOException {
            furtherHearingShowOption.add(FinalOrderShowToggle.SHOW);
        }

        @Test
        void shouldReturnNull_When_FurtherHearingOption_notSelected() {
            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(null)
                .build();
            String assistedOrderString = generator.getFurtherHearingText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_FurtherHearingOption_Null() {
            List<FinalOrderShowToggle> furtherHearingOption = new ArrayList<>();
            furtherHearingOption.add(null);
            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(furtherHearingOption)
                .build();
            String assistedOrderString = generator.getFurtherHearingText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_FurtherHearingOption_Hide() {
            List<FinalOrderShowToggle> furtherHearingOption = new ArrayList<>();
            furtherHearingOption.add(FinalOrderShowToggle.HIDE);
            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(furtherHearingOption)
                .build();
            String assistedOrderString = generator.getFurtherHearingText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldNotListToDate_When_FurtherHearingOption_NoLisToDate() {
            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(furtherHearingShowOption)
                .assistedOrderFurtherHearingDetails(getFurtherHearingCaseData(false,
                                                                              false,
                                                                              false,
                                                                              null,
                                                                              false))

                .build();
            String assistedOrderString = generator.getFurtherHearingText(caseData);
            assertFalse(assistedOrderString.contains("It will take place before:"));
        }

        @Test
        void shouldListToDate_When_FurtherHearingOption_LisToDate() {

            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(furtherHearingShowOption)
                .assistedOrderFurtherHearingDetails(getFurtherHearingCaseData(true,
                                                                              false,
                                                                              false,
                                                                              null,
                                                                              false))

                .build();
            String assistedOrderString = generator.getFurtherHearingText(caseData);
            assertThat(assistedOrderString).contains("It will take place before:");
        }

        @Test
        void shouldNotReturnText_When_LengthOfHearingNull() {

            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(furtherHearingShowOption)
                .assistedOrderFurtherHearingDetails(getFurtherHearingCaseData(true,
                                                                              false,
                                                                              false,
                                                                              null,
                                                                              false))

                .build();

            String assistedOrderString = generator.getFurtherHearingText(caseData);
            assertFalse(assistedOrderString.contains("The length of new hearing will be:"));
        }

        @Test
        void shouldReturnText_When_LengthOfHearingNotNull() {

            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(furtherHearingShowOption)
                .assistedOrderFurtherHearingDetails(getFurtherHearingCaseData(true,
                                                                              true,
                                                                              false,
                                                                              null,
                                                                              false))

                .build();

            String assistedOrderString = generator.getFurtherHearingText(caseData);
            assertThat(assistedOrderString).contains("The length of new hearing will be:");
        }

        @Test
        void shouldReturnOtherText_When_LengthOfHearingOther() {
            DynamicListElement location1 = DynamicListElement.builder()
                .code(UUID.randomUUID()).label("Site Name 2 - Address2 - 28000").build();
            DynamicList alternateDirection = DynamicList.builder().listItems(List.of(location1)).build();
            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(furtherHearingShowOption)
                .assistedOrderFurtherHearingDetails(getFurtherHearingCaseData(true,
                                                                              false,
                                                                              true,
                                                                              alternateDirection,
                                                                              false))

                .build();

            String assistedOrderString = generator.getFurtherHearingText(caseData);
            assertThat(assistedOrderString).contains("Days - 2, Hours - 2, Minutes - 2");
        }

        @Test
        void shouldReturnOtherText_When_AlternativeLocation_and_HearingMethodVideo() {
            DynamicListElement location1 = DynamicListElement.builder()
                .code(UUID.randomUUID()).label("Site Name 2 - Address2 - 28000").build();
            DynamicList alternateDirection = DynamicList.builder()
                .listItems(List.of(location1))
                .value(location1)
                .build();
            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(furtherHearingShowOption)
                .assistedOrderFurtherHearingDetails(getFurtherHearingCaseData(true,
                                                                              false,
                                                                              true,
                                                                              alternateDirection,
                                                                              true))

                .build();

            String assistedOrderString = generator.getFurtherHearingText(caseData);
            assertThat(assistedOrderString).contains("Site Name 2");
            assertThat(assistedOrderString).contains("via video");
        }

        private AssistedOrderFurtherHearingDetails getFurtherHearingCaseData(boolean isListToDate,
                                                                             boolean isLengthOfHearing,
                                                                             boolean isLengthOfHearingOther,
                                                                             DynamicList alternativeLocation,
                                                                             boolean isHearingMethod) {
            AssistedOrderFurtherHearingDetails.AssistedOrderFurtherHearingDetailsBuilder furtherHearingBuilder
                = AssistedOrderFurtherHearingDetails.builder();
            furtherHearingBuilder.listFromDate(LocalDate.now());
            if (isListToDate) {
                furtherHearingBuilder.listToDate(LocalDate.now().plusDays(5));
            }

            if (isLengthOfHearing) {
                furtherHearingBuilder.lengthOfNewHearing(LengthOfHearing.HOUR_1_5);
            }

            if (isLengthOfHearingOther) {
                furtherHearingBuilder.lengthOfNewHearing(LengthOfHearing.OTHER);
                furtherHearingBuilder.lengthOfHearingOther(HearingLength.builder()
                                                               .lengthListOtherDays(2)
                                                               .lengthListOtherMinutes(2)
                                                               .lengthListOtherHours(2)
                                                               .build());
            }

            if (alternativeLocation != null) {
                furtherHearingBuilder.alternativeHearingLocation(alternativeLocation);
            }

            if (isHearingMethod) {
                furtherHearingBuilder.hearingMethods(GAJudicialHearingType.VIDEO);
            }

            return furtherHearingBuilder.build();

        }
    }

    @Nested
    class RecitalRecord {
        @Test
        void shouldReturnNull_When_RecitalsShowOption_Null() {
            CaseData caseData = CaseData.builder()
                .assistedOrderRecitals(null)
                .build();
            String assistedOrderString = generator.getRecitalRecordedText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_RecitalsShowOption_NoSelected() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(null);
            CaseData caseData = CaseData.builder()
                .assistedOrderRecitals(recitalsOrderShowOption)
                .build();
            String assistedOrderString = generator.getRecitalRecordedText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_RecitalsShowOption_SelectedHide() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(FinalOrderShowToggle.HIDE);
            CaseData caseData = CaseData.builder()
                .assistedOrderRecitals(recitalsOrderShowOption)
                .build();
            String assistedOrderString = generator.getRecitalRecordedText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_RecitalsShowOption_SelectedShow_RecitalTextEmpty() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(FinalOrderShowToggle.SHOW);
            CaseData caseData = CaseData.builder()
                .assistedOrderRecitals(recitalsOrderShowOption)
                .build();
            String assistedOrderString = generator.getRecitalRecordedText(caseData);
            assertTrue(assistedOrderString.isEmpty());
        }

        @Test
        void shouldReturnNull_When_RecitalsShowOption_SelectedShow_WithRecitalText() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(FinalOrderShowToggle.SHOW);
            CaseData caseData = CaseData.builder()
                .assistedOrderRecitals(recitalsOrderShowOption)
                .assistedOrderRecitalsRecorded(AssistedOrderRecitalRecord.builder().text(TEST_TEXT).build())
                .build();
            String assistedOrderString = generator.getRecitalRecordedText(caseData);
            assertThat(assistedOrderString).contains(TEST_TEXT);
        }
    }

    @Nested
    class JudgeHeardFrom {

        @Test
        void shouldReturnNull_When_AssistedHeardFrom_Null() {
            CaseData caseData = CaseData.builder()
                .assistedOrderJudgeHeardFrom(null)
                .build();
            String assistedOrderString = generator.generalJudgeHeardFromText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_AssistedHeardFrom_NoTSelected() {
            List<FinalOrderShowToggle> judgeHeardFromShowOption = new ArrayList<>();
            judgeHeardFromShowOption.add(null);
            CaseData caseData = CaseData.builder()
                .assistedOrderJudgeHeardFrom(judgeHeardFromShowOption)
                .build();
            String assistedOrderString = generator.generalJudgeHeardFromText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_AssistedHeardFrom_SelectedHide() {
            List<FinalOrderShowToggle> judgeHeardFromShowOption = new ArrayList<>();
            judgeHeardFromShowOption.add(FinalOrderShowToggle.HIDE);
            CaseData caseData = CaseData.builder()
                .assistedOrderJudgeHeardFrom(judgeHeardFromShowOption)
                .build();
            String assistedOrderString = generator.generalJudgeHeardFromText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnText_When_AssistedHeardFrom_BothAttended() {
            List<FinalOrderShowToggle> judgeHeardFromShowOption = new ArrayList<>();
            judgeHeardFromShowOption.add(FinalOrderShowToggle.SHOW);
            CaseData caseData = CaseData.builder()
                .assistedOrderJudgeHeardFrom(judgeHeardFromShowOption)
                .assistedOrderRepresentation(getHeardRepresentation(true, false, false, false, false))
                .build();
            String assistedOrderString = generator.generalJudgeHeardFromText(caseData);

            assertThat(assistedOrderString).contains(DefendantRepresentationType.COST_DRAFTSMAN_FOR_THE_DEFENDANT
                                                         .getDisplayedValue().toLowerCase());
        }

        @Test
        void shouldReturnText_When_AssistedHeard_ClaimantAttendDefendantNot() {
            List<FinalOrderShowToggle> judgeHeardFromShowOption = new ArrayList<>();
            judgeHeardFromShowOption.add(FinalOrderShowToggle.SHOW);
            CaseData caseData = CaseData.builder()
                .assistedOrderJudgeHeardFrom(judgeHeardFromShowOption)
                .assistedOrderRepresentation(getHeardRepresentation(false, true, false, false, false))
                .build();
            String assistedOrderString = generator.generalJudgeHeardFromText(caseData);

            assertThat(assistedOrderString).contains(JUDGE_SATISFIED_TO_PROCEED_TEXT);
        }

        @Test
        void shouldReturnText_When_AssistedHeard_ClaimantNotDefendantAttend() {
            List<FinalOrderShowToggle> judgeHeardFromShowOption = new ArrayList<>();
            judgeHeardFromShowOption.add(FinalOrderShowToggle.SHOW);
            CaseData caseData = CaseData.builder()
                .assistedOrderJudgeHeardFrom(judgeHeardFromShowOption)
                .assistedOrderRepresentation(getHeardRepresentation(false, false, true, false, false))
                .build();
            String assistedOrderString = generator.generalJudgeHeardFromText(caseData);

            assertThat(assistedOrderString).contains(JUDGE_SATISFIED_NOTICE_OF_TRIAL_TEXT);
        }

        @Test
        void shouldReturnText_When_AssistedHeard_BothNotAttended() {
            List<FinalOrderShowToggle> judgeHeardFromShowOption = new ArrayList<>();
            judgeHeardFromShowOption.add(FinalOrderShowToggle.SHOW);
            CaseData caseData = CaseData.builder()
                .assistedOrderJudgeHeardFrom(judgeHeardFromShowOption)
                .assistedOrderRepresentation(getHeardRepresentation(false, false, false, true, false))
                .build();
            String assistedOrderString = generator.generalJudgeHeardFromText(caseData);

            assertThat(assistedOrderString).contains(JUDGE_NOT_SATISFIED_NOTICE_OF_TRIAL_TEXT);
        }

        @Test
        void shouldReturnText_When_AssistedHeard_OtherRepresentation() {
            List<FinalOrderShowToggle> judgeHeardFromShowOption = new ArrayList<>();
            judgeHeardFromShowOption.add(FinalOrderShowToggle.SHOW);
            CaseData caseData = CaseData.builder()
                .assistedOrderJudgeHeardFrom(judgeHeardFromShowOption)
                .assistedOrderRepresentation(getHeardRepresentation(false, false, false, false, true))
                .build();
            String assistedOrderString = generator.generalJudgeHeardFromText(caseData);

            assertThat(assistedOrderString).contains(TEST_TEXT);
            assertThat(assistedOrderString).contains(JUDGE_CONSIDERED_PAPERS_TEXT);
        }

        private AssistedOrderHeardRepresentation getHeardRepresentation(boolean esBothAttended,
                                                                        boolean esClaimantAttendDefendantNot,
                                                                        boolean esClaimantNotDefendantAttended,
                                                                        boolean esBothNotAttended,
                                                                        boolean esOtherRepresentationType) {

            AssistedOrderHeardRepresentation.AssistedOrderHeardRepresentationBuilder assistedRepBuilder
                = AssistedOrderHeardRepresentation.builder();
            List<FinalOrderConsideredToggle> judgePapersList = new ArrayList<>();

            if (esOtherRepresentationType) {
                judgePapersList.add(FinalOrderConsideredToggle.CONSIDERED);
                assistedRepBuilder
                    .representationType(HeardFromRepresentationTypes.OTHER_REPRESENTATION)
                    .otherRepresentation(DetailText.builder().detailText(TEST_TEXT).build())
                    .typeRepresentationJudgePapersList(judgePapersList)
                    .build();
            } else {
                assistedRepBuilder.representationType(HeardFromRepresentationTypes.CLAIMANT_AND_DEFENDANT);
                if (esBothAttended) {
                    assistedRepBuilder
                        .claimantDefendantRepresentation(ClaimantDefendantRepresentation.builder()
                                                             .defendantRepresentation(
                                                                 DefendantRepresentationType
                                                                     .COST_DRAFTSMAN_FOR_THE_DEFENDANT)
                                                             .claimantRepresentation(
                                                                 ClaimantRepresentationType.COUNSEL_FOR_CLAIMANT)
                                                             .build())
                        .build();
                } else if (esClaimantAttendDefendantNot) {
                    judgePapersList.add(FinalOrderConsideredToggle.NOT_CONSIDERED);
                    assistedRepBuilder
                        .claimantDefendantRepresentation(
                            ClaimantDefendantRepresentation.builder()
                                .defendantRepresentation(DefendantRepresentationType.DEFENDANT_NOT_ATTENDING)
                                .heardFromDefendantNotAttend(HeardDefendantNotAttend.builder()
                                                                 .listDef(ClaimantDefendantNotAttendingType
                                                                              .SATISFIED_REASONABLE_TO_PROCEED)
                                                                 .build())
                                .claimantRepresentation(ClaimantRepresentationType.COUNSEL_FOR_CLAIMANT)

                                .build())
                        .typeRepresentationJudgePapersList(judgePapersList)
                        .build();
                } else if (esClaimantNotDefendantAttended) {
                    assistedRepBuilder
                        .claimantDefendantRepresentation(
                            ClaimantDefendantRepresentation.builder()
                                .defendantRepresentation(DefendantRepresentationType.COUNSEL_FOR_DEFENDANT)
                                .claimantRepresentation(ClaimantRepresentationType.CLAIMANT_NOT_ATTENDING)
                                .heardFromClaimantNotAttend(HeardClaimantNotAttend.builder()
                                                                .listClaim(ClaimantDefendantNotAttendingType
                                                                               .SATISFIED_NOTICE_OF_TRIAL)
                                                                .build())

                                .build())
                        .build();
                } else if (esBothNotAttended) {
                    assistedRepBuilder
                        .claimantDefendantRepresentation(
                            ClaimantDefendantRepresentation.builder()
                                .claimantRepresentation(ClaimantRepresentationType.CLAIMANT_NOT_ATTENDING)
                                .heardFromClaimantNotAttend(HeardClaimantNotAttend.builder()
                                                                .listClaim(ClaimantDefendantNotAttendingType
                                                                               .SATISFIED_NOTICE_OF_TRIAL)
                                                                .build())
                                .defendantRepresentation(DefendantRepresentationType.DEFENDANT_NOT_ATTENDING)
                                .heardFromDefendantNotAttend(HeardDefendantNotAttend.builder()
                                                                 .listDef(ClaimantDefendantNotAttendingType
                                                                              .NOT_SATISFIED_NOTICE_OF_TRIAL)
                                                                 .build())
                                .build())
                        .build();
                }
            }

            return assistedRepBuilder.build();
        }
    }

    @Nested
    class OrderMadeOn {

        @Test
        void shouldReturnText_When_OrderMadeOn_CourtInitiative() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.COURTS_INITIATIVE)
                .orderMadeOnOwnInitiative(DetailTextWithDate.builder()
                                              .detailText(TEST_TEXT)
                                              .date(LocalDate.now())
                                              .build())
                .build();
            String assistedOrderString = generator.getOrderMadeOnText(caseData);
            assertThat(assistedOrderString).contains(TEST_TEXT);
        }

        @Test
        void shouldReturnText_When_OrderMadeOn_WithOutNotice() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.WITHOUT_NOTICE)
                .orderMadeOnWithOutNotice(DetailTextWithDate.builder()
                                              .detailText(TEST_TEXT)
                                              .date(LocalDate.now())
                                              .build())
                .build();
            String assistedOrderString = generator.getOrderMadeOnText(caseData);
            assertThat(assistedOrderString).contains(TEST_TEXT);
        }

        @Test
        void shouldReturnText_When_OrderMadeOn_None() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.NONE)
                .build();
            String assistedOrderString = generator.getOrderMadeOnText(caseData);
            assertThat(assistedOrderString).contains(ORDER_MADE_ON_NONE_TEXT);
        }
    }

    @Nested
    class PermissionToAppeal {

        @Test
        void shouldReturnNull_When_OrderAppeal_NotSelected() {
            CaseData caseData = CaseData.builder()
                .assistedOrderAppealToggle(null)
                .build();
            String assistedOrderString = generator.getPermissionToAppealText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_OrderAppealOption_Null() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(null);
            CaseData caseData = CaseData.builder()
                .assistedOrderAppealToggle(recitalsOrderShowOption)
                .build();
            String assistedOrderString = generator.getPermissionToAppealText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_OrderAppealOption_NotShow() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(FinalOrderShowToggle.HIDE);
            CaseData caseData = CaseData.builder()
                .assistedOrderAppealToggle(recitalsOrderShowOption)
                .build();
            String assistedOrderString = generator.getPermissionToAppealText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_AppealDetails_Null() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(FinalOrderShowToggle.SHOW);
            CaseData caseData = CaseData.builder()
                .assistedOrderAppealToggle(recitalsOrderShowOption)
                .build();
            String assistedOrderString = generator.getPermissionToAppealText(caseData);
            assertThat(assistedOrderString).isEmpty();
        }

        @Test
        void shouldReturnNull_When_AppealDetails_WithOutReasonText() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(FinalOrderShowToggle.SHOW);
            CaseData caseData = CaseData.builder()
                .assistedOrderAppealToggle(recitalsOrderShowOption)
                .assistedOrderAppealDetails(AssistedOrderAppealDetails
                                                .builder()
                                                .permissionToAppeal(PermissionToAppealTypes.GRANTED)
                                                .appealOrigin(AppealOriginTypes.CLAIMANT)
                                                .build())
                .build();
            String assistedOrderString = generator.getPermissionToAppealText(caseData);
            assertThat(assistedOrderString).contains("The application for permission to");
        }

        @Test
        void shouldReturnNull_When_AppealDetails_WithReasonText() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(FinalOrderShowToggle.SHOW);
            CaseData caseData = CaseData.builder()
                .assistedOrderAppealToggle(recitalsOrderShowOption)
                .assistedOrderAppealDetails(AssistedOrderAppealDetails
                                                .builder()
                                                .permissionToAppeal(PermissionToAppealTypes.GRANTED)
                                                .appealOrigin(AppealOriginTypes.CLAIMANT)
                                                .reasonsText(TEST_TEXT)
                                                .build())
                .build();
            String assistedOrderString = generator.getPermissionToAppealText(caseData);
            assertThat(assistedOrderString).contains(TEST_TEXT);
        }
    }

    @Nested
    class OrderMadeDate {

        @Test
        void shouldReturnNull_When_OrderMadeHeardDetails_Null() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeDateHeardDetails(null)
                .build();
            String assistedOrderString = generator.getOrderMadeDate(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_OrderMadeHeard_Date_Null() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().build())
                .build();
            String assistedOrderString = generator.getOrderMadeDate(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnText_When_OrderMadeDate() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails
                                                       .builder()
                                                       .date(LocalDate.now())
                                                       .build())
                .build();
            String assistedOrderString = generator.getOrderMadeDate(caseData);
            assertThat(assistedOrderString).isNotEmpty();
        }
    }

    @Nested
    class ReasonText {

        @Test
        void shouldReturnNull_When_GiveReasons_Null() {
            CaseData caseData = CaseData.builder()
                .assistedOrderGiveReasonsYesNo(null)
                .build();
            String assistedOrderString = generator.getOrderMadeDate(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_GiveReasons_SelectedOption_No() {
            CaseData caseData = CaseData.builder()
                .assistedOrderGiveReasonsYesNo(YesOrNo.NO)
                .build();
            String assistedOrderString = generator.getOrderMadeDate(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_GiveReasonsText_Null() {
            CaseData caseData = CaseData.builder()
                .assistedOrderGiveReasonsYesNo(YesOrNo.YES)
                .assistedOrderGiveReasonsDetails(AssistedOrderGiveReasonsDetails.builder().build())
                .build();
            String assistedOrderString = generator.getOrderMadeDate(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnText_When_GiveReasonsText() {
            CaseData caseData = CaseData.builder()
                .assistedOrderGiveReasonsYesNo(YesOrNo.YES)
                .assistedOrderGiveReasonsDetails(AssistedOrderGiveReasonsDetails
                                                     .builder()
                                                     .reasonsText(TEST_TEXT)
                                                     .build())
                .build();
            String assistedOrderString = generator.getOrderMadeDate(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_OrderMadeHeard_Date_Null() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().build())
                .build();
            String assistedOrderString = generator.getOrderMadeDate(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnText_When_OrderMadeHeard() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails
                                                       .builder()
                                                       .date(LocalDate.now())
                                                       .build())
                .build();
            String assistedOrderString = generator.getOrderMadeDate(caseData);
            assertThat(assistedOrderString).contains(DateFormatHelper
                                                         .formatLocalDate(LocalDate.now(), " d MMMM yyyy"));
        }
    }

    @Test
    void test_getCaseNumberFormatted() {
        CaseData caseData = CaseDataBuilder.builder().ccdCaseReference(1644495739087775L).build();
        String formattedCaseNumber = generator.getCaseNumberFormatted(caseData);
        assertThat(formattedCaseNumber).isEqualTo("1644-4957-3908-7775");
    }

    @Test
    void test_getFileName() {
        String name = generator.getFileName(DocmosisTemplates.ASSISTED_ORDER_FORM);
        assertThat(name).startsWith("Assisted_order_form_");
        assertThat(name).endsWith(".pdf");
    }

    @Test
    void test_getDateFormatted() {
        String dateString = generator.getDateFormatted(LocalDate.EPOCH);
        assertThat(dateString).isEqualTo(" 1 January 1970");
    }

    @Test
    void test_getReference() {
        Map<String, String> refMap = new HashMap<>();
        refMap.put("applicantSolicitor1Reference", "app1ref");
        refMap.put("respondentSolicitor1Reference", "resp1ref");
        Map<String, Object> caseDataContent = new HashMap<>();
        caseDataContent.put("solicitorReferences", refMap);
        CaseDetails caseDetails = CaseDetails.builder().data(caseDataContent).build();

        assertThat(generator.getReference(caseDetails, "applicantSolicitor1Reference")).isEqualTo("app1ref");
        assertThat(generator.getReference(caseDetails, "notExist")).isNull();
    }

    @Test
    void test_getTemplate() {
        assertThat(generator.getTemplate()).isEqualTo(DocmosisTemplates.ASSISTED_ORDER_FORM);
    }
}
