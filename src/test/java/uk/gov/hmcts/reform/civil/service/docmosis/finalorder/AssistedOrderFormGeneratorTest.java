package uk.gov.hmcts.reform.civil.service.docmosis.finalorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.*;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;
import uk.gov.hmcts.reform.civil.model.genapplication.HearingLength;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.*;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
    AssistedOrderFormGenerator.class
})
class AssistedOrderFormGeneratorTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(" d MMMM yyyy");
    private static final String FILE_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String LINE_BREAKER = "\n\n";
    private static final String ORDER_MADE_ON_NONE_TEXT = "This order was not made on the court’s own initiative"
        +" or without notice.";
    private static final String CLAIMANT_DEFENDANT_BOTH_ATTENDED_TEXT = "The Judge heard from %s and %s.";
    private static final String CLAIMANT_OR_DEFENDANT_ATTENDED_TEXT = "The Judge heard from %s.";

    private static final String CLAIMANT_NOT_ATTENDED_TEXT = "The claimant did not attend the trial";
    private static final String DEFENDANT_NOT_ATTENDED_TEXT = "The defendant did not attend the trial";
    private static final String JUDGE_SATISFIED_TO_PROCEED_TEXT = ", but the Judge was satisfied that they had received"
        + " notice of the trial and it was reasonable to proceed in their absence.";
    private static final String JUDGE_SATISFIED_NOTICE_OF_TRIAL_TEXT = " and whilst the Judge was satisfied that they"
        + " had received notice of the trial it was not reasonable to proceed in their absence.";
    private static final String JUDGE_NOT_SATISFIED_NOTICE_OF_TRIAL_TEXT = ", but the Judge was not satisfied that they"
        + " had received notice of the hearing and it was not reasonable to proceed in their absence.";

    private static final String JUDGE_HEARD_FROM_TEXT = "The Judge heard other representation: %s";
    private static final String JUDGE_CONSIDERED_PAPERS_TEXT = "The judge considered the papers.";
    private static final String RECITAL_RECORDED_TEXT = "It is recorded that %s.";
    private static final String COST_IN_CASE_TEXT = "Costs in the case have been ordered.";
    private static final String NO_ORDER_COST_TEXT = "No order as to costs has been made.";
    private static final String COSTS_RESERVED_TEXT = "Costs reserved:%s.";
    private static final String COST_AMOUNT_TEXT = "Amount: %s ";
    private static final String COST_PAID_BY_TEXT =  "To be paid by: %s.";
    private static final String COST_PARTY_HAS_BENEFIT_TEXT = "The paying party has the benefit of cost protection"
        + " under section 26 Sentencing and Punishment Offenders Act 2012. The amount of the costs pay shall"
        + " be determined on an application by the receiving party under Legal Aid (Costs) Regulations 2013."
        + " Any objection by the paying party claimed shall be dealt with on that occasion.";
    private static final String COST_PARTY_NO_BENEFIT_TEXT =  "The paying party does not have cost protection.";
    private static final String COST_BESPOKE_TEXT = "Bespoke costs orders: %s ";

    private static final String FURTHER_HEARING_TAKE_PLACE_AFTER_TEXT = "A further hearing will take place after: %s ";
    private static final String FURTHER_HEARING_TAKE_PLACE_BEFORE_TEXT = "It will take place before: %s";
    private static final String FURTHER_HEARING_LENGTH_TEXT = "The length of new hearing will be: %s";
    private static final String FURTHER_HEARING_LENGTH_OTHER = " %s/ %s/ %s";
    private static final String FURTHER_HEARING_ALTERNATIVE_HEARING_TEXT = "Alternative hearing location: %s";
    private static final String FURTHER_HEARING_METHOD_HEARING_TEXT = "Method of hearing: %s";
    private static final String PERMISSION_TO_APPEAL_TEXT = "The application for permission to appeal "
        + "for the %s is %s.";
    private static final String permissionToAppealReasonsText = "Reasons: %s ";

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
            assertThat(assistedOrderString.contains("The paying party has the benefit of cost"));
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

            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(furtherHearingShowOption)
                .assistedOrderFurtherHearingDetails(getFurtherHearingCaseData(true,
                                                                              false,
                                                                              true,
                                                                              null,
                                                                              false))

                .build();

            String assistedOrderString = generator.getFurtherHearingText(caseData);
            assertThat(assistedOrderString).contains("2/ 2/ 2");
        }

        @Test
        void shouldReturnOtherText_When_AlternativeLocation_and_HearingMethodVideo() {
            DynamicListElement location1 = DynamicListElement.builder()
                .code(UUID.randomUUID()).label("Site Name 2 - Address2 - 28000").build();
            DynamicList alternateDirection =DynamicList.builder().listItems(List.of(location1)).build();
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
                                                                             boolean isHearingMethod){
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
                .assistedOrderRepresentation(getHeardRepresentation(false, true, false , false, false))
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
                .assistedOrderRepresentation(getHeardRepresentation(false, false, true , false, false))
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
                .assistedOrderRepresentation(getHeardRepresentation(false, false, false , true, false))
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
                .assistedOrderRepresentation(getHeardRepresentation(false, false, false , false, true))
                .build();
            String assistedOrderString = generator.generalJudgeHeardFromText(caseData);

            assertThat(assistedOrderString).contains(TEST_TEXT);
            assertThat(assistedOrderString).contains(JUDGE_CONSIDERED_PAPERS_TEXT);
        }

        private AssistedOrderHeardRepresentation getHeardRepresentation(boolean esBothAttended,
                                                                        boolean esClaimantAttendDefendantNot,
                                                                        boolean esClaimantNotDefendantAttended,
                                                                        boolean esBothNotAttended,
                                                                        boolean esOtherRepresentationType){

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
            }else {
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
    @Test
    void getOrderMadeDate() {
    }

    @Test
    void getReasonText() {
    }

    @Test
    void getFileName() {
    }

    @Test
    void getReference() {
    }

    @Test
    void getCostText() {
    }

    @Test
    void getJudgeSatisfiedText() {
    }

    @Test
    void getIsProtectionDateText() {
    }

    @Test
    void getTemplate() {
    }
}
