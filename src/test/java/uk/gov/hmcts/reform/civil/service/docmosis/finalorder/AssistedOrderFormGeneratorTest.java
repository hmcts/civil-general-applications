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
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;
import uk.gov.hmcts.reform.civil.model.genapplication.HearingLength;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.*;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    private static final String RECITAL_RECORDED_TEXT = "It is recorded that";
    private static final String CLAIMANT_SUMMARILY_ASSESSED_TEXT = "The claimant shall pay the defendant's costs (both fixed and summarily assessed as appropriate) in the sum of £789.00. " +
        "Such a sum shall be made by 4pm on";
    private static final String DEFENDANT_SUMMARILY_ASSESSED_TEXT = "The defendant shall pay the claimant's costs (both fixed and summarily assessed as appropriate) in the sum of £789.00. " +
        "Such a sum shall be made by 4pm on";
    private static final String CLAIMANT_DETAILED_INDEMNITY_ASSESSED_TEXT = "The claimant shall pay the defendant's costs to be subject to a detailed assessment on the indemnity basis if not agreed";
    private static final String CLAIMANT_DETAILED_STANDARD_ASSESSED_TEXT = "The claimant shall pay the defendant's costs to be subject to a detailed assessment on the standard basis if not agreed";
    private static final String DEFENDANT_DETAILED_INDEMNITY_ASSESSED_TEXT = "The defendant shall pay the claimant's costs to be subject to a detailed assessment on the indemnity basis if not agreed";
    private static final String DEFENDANT_DETAILED_STANDARD_ASSESSED_TEXT = "The defendant shall pay the claimant's costs to be subject to a detailed assessment on the standard basis if not agreed";
    private static final String TEST_TEXT = "Test 123";
    private static final String OTHER_ORIGIN_TEXT ="test other origin text";

    private static final String INTERIM_PAYMENT_TEXT ="An interim payment of £500.00 on account of costs shall be paid by 4pm on";

    @MockBean
    private DocumentManagementService documentManagementService;

    @MockBean
    private DocumentGeneratorService documentGeneratorService;

    @MockBean
    private DocmosisService docmosisService;
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
        void shouldReturnText_WhenSelected_Claimant_SummarilyAssessed() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.CLAIMANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.COSTS)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900)).build()).build();
            String assistedOrderString = generator.getSummarilyAssessed(caseData);

            assertThat(assistedOrderString).contains(CLAIMANT_SUMMARILY_ASSESSED_TEXT);
        }

        @Test
        void shouldReturnText_WhenSelected_Defendant_SummarilyAssessed() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.COSTS)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900)).build()).build();
            String assistedOrderString = generator.getSummarilyAssessed(caseData);

            assertThat(assistedOrderString).contains(DEFENDANT_SUMMARILY_ASSESSED_TEXT);
        }

        @Test
        void shouldReturnNull_WhenMakeAnOrderForCostsEmpty() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder().build()).build();
            String assistedOrderString = generator.getSummarilyAssessed(caseData);

            assertThat(assistedOrderString).isNull();
        }

        @Test
        void shouldReturnNull_WhenMakeAnOrderForCostsListIsEmpty() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS).build();
            String assistedOrderString = generator.getSummarilyAssessed(caseData);

            assertThat(assistedOrderString).isNull();
        }

        @Test
        void shouldReturnDifferentText_WhenSelected_Defendant_SubjectSummarilyAssessed() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.SUBJECT_DETAILED_ASSESSMENT)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900)).build()).build();
            String assistedOrderString = generator.getSummarilyAssessed(caseData);

            assertThat(assistedOrderString).isNull();
        }

        @Test
        void shouldReturnText_WhenSelected_Claimant_SummarilyAssessedDate() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.COSTS)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900))
                                                      .assistedOrderCostsFirstDropdownDate(LocalDate.now().minusDays(5)).build()).build();
            LocalDate assistedOrderDropdownDate = generator.getSummarilyAssessedDate(caseData);

            assertThat(assistedOrderDropdownDate).isEqualTo(LocalDate.now().minusDays(5));
        }

        @Test
        void shouldReturnNull_When_MakeAnOrderForCostsIsNull_SummarilyAssessedDate() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .build()).build();
            LocalDate assistedOrderDropdownDate = generator.getSummarilyAssessedDate(caseData);

            assertThat(assistedOrderDropdownDate).isNull();
        }

        @Test
        void shouldReturnNull_When_MakeAnOrderForCostsListIsNull_SummarilyAssessedDate() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.COSTS)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900))
                                                      .assistedOrderCostsFirstDropdownDate(LocalDate.now().minusDays(5)).build()).build();
            LocalDate assistedOrderDropdownDate = generator.getSummarilyAssessedDate(caseData);

            assertThat(assistedOrderDropdownDate).isNull();
        }

        @Test
        void shouldReturnNull_When_MakeAnOrderForCostsListDropdownIsNotCosts_SummarilyAssessedDate() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.SUBJECT_DETAILED_ASSESSMENT)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900))
                                                      .assistedOrderCostsFirstDropdownDate(LocalDate.now().minusDays(5)).build()).build();
            LocalDate assistedOrderDropdownDate = generator.getSummarilyAssessedDate(caseData);

            assertThat(assistedOrderDropdownDate).isNull();
        }

        @Test
        void shouldReturnText_WhenSelected_Claimant_DetailedAssessed() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.CLAIMANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.SUBJECT_DETAILED_ASSESSMENT)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900))
                                                      .assistedOrderAssessmentSecondDropdownList1(
                                                          AssistedOrderCostDropdownList.INDEMNITY_BASIS).build()).build();
            String assistedOrderString = generator.getDetailedAssessment(caseData);

            assertThat(assistedOrderString).contains(CLAIMANT_DETAILED_INDEMNITY_ASSESSED_TEXT);
        }

        @Test
        void shouldReturnText_WhenSelected_Claimant_StandardDetailedAssessed() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.CLAIMANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.SUBJECT_DETAILED_ASSESSMENT)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900))
                                                      .assistedOrderAssessmentSecondDropdownList1(
                                                          AssistedOrderCostDropdownList.STANDARD_BASIS).build()).build();
            String assistedOrderString = generator.getDetailedAssessment(caseData);

            assertThat(assistedOrderString).contains(CLAIMANT_DETAILED_STANDARD_ASSESSED_TEXT);
        }

        @Test
        void shouldReturnText_WhenSelected_Defendant_StandardDetailedAssessed() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.SUBJECT_DETAILED_ASSESSMENT)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900))
                                                      .assistedOrderAssessmentSecondDropdownList1(
                                                          AssistedOrderCostDropdownList.STANDARD_BASIS).build()).build();
            String assistedOrderString = generator.getDetailedAssessment(caseData);

            assertThat(assistedOrderString).contains(DEFENDANT_DETAILED_STANDARD_ASSESSED_TEXT);
        }

        @Test
        void shouldReturnText_WhenSelected_Defendant_IndemnityDetailedAssessed() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.SUBJECT_DETAILED_ASSESSMENT)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900))
                                                      .assistedOrderAssessmentSecondDropdownList1(
                                                          AssistedOrderCostDropdownList.INDEMNITY_BASIS).build()).build();
            String assistedOrderString = generator.getDetailedAssessment(caseData);

            assertThat(assistedOrderString).contains(DEFENDANT_DETAILED_INDEMNITY_ASSESSED_TEXT);
        }

        @Test
        void shouldReturnNull_WhenMakeAnOrderForCostsEmpty_Detailed_Assessment() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder().build()).build();
            String assistedOrderString = generator.getDetailedAssessment(caseData);

            assertThat(assistedOrderString).isNull();
        }

        @Test
        void shouldReturnNull_WhenMakeAnOrderForCostsListIsEmpty_Detailed_Assessment() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS).build();
            String assistedOrderString = generator.getDetailedAssessment(caseData);

            assertThat(assistedOrderString).isNull();
        }

        @Test
        void shouldReturnDifferentText_WhenSelected_Defendant_CostsDetailed_Assessment() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.COSTS)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900)).build()).build();
            String assistedOrderString = generator.getDetailedAssessment(caseData);

            assertThat(assistedOrderString).isNull();
        }

        @Test
        void shouldReturnNull_WhenMakeAnOrderForCostsEmpty_InterimPayment() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder().build()).build();
            String assistedOrderString = generator.getInterimPayment(caseData);

            assertThat(assistedOrderString).isNull();
        }

        @Test
        void shouldReturnNull_WhenMakeAnOrderForCostsListIsEmpty_InterimPayment() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS).build();
            String assistedOrderString = generator.getInterimPayment(caseData);

            assertThat(assistedOrderString).isNull();
        }

        @Test
        void shouldReturnDifferentText_WhenSelected_Defendant_CostsInterimPayment() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.COSTS)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900)).build()).build();
            String assistedOrderString = generator.getInterimPayment(caseData);

            assertThat(assistedOrderString).isNull();
        }

        @Test
        void shouldReturnDifferentText_WhenSelected_Defendant_CostsInterimPayment_No() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.COSTS)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900))
                                                      .assistedOrderAssessmentSecondDropdownList2(
                                                          AssistedOrderCostDropdownList.NO).build()).build();
            String assistedOrderString = generator.getInterimPayment(caseData);

            assertThat(assistedOrderString).isNull();
        }

        @Test
        void shouldReturnText_WhenSelected_Defendant_InterimPayment() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.SUBJECT_DETAILED_ASSESSMENT)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900))
                                                      .assistedOrderAssessmentSecondDropdownList1(
                                                          AssistedOrderCostDropdownList.INDEMNITY_BASIS)
                                                      .assistedOrderAssessmentSecondDropdownList2(
                                                          AssistedOrderCostDropdownList.YES)
                                                      .assistedOrderAssessmentThirdDropdownAmount(BigDecimal.valueOf(
                                                          50000)).build()).build();
            String assistedOrderString = generator.getInterimPayment(caseData);

            assertThat(assistedOrderString).contains(INTERIM_PAYMENT_TEXT);
        }

        @Test
        void shouldReturnDate_WhenSelected_Defendant_InterimPaymentDate() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.SUBJECT_DETAILED_ASSESSMENT)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900))
                                                      .assistedOrderAssessmentSecondDropdownList1(
                                                          AssistedOrderCostDropdownList.INDEMNITY_BASIS)
                                                      .assistedOrderAssessmentSecondDropdownList2(
                                                          AssistedOrderCostDropdownList.YES)
                                                      .assistedOrderAssessmentThirdDropdownAmount(BigDecimal.valueOf(
                                                          50000))
                                                      .assistedOrderAssessmentThirdDropdownDate(LocalDate.now().plusDays(
                                                          10)).build()).build();
            LocalDate assistedOrderDate = generator.getInterimPaymentDate(caseData);

            assertThat(assistedOrderDate).isEqualTo(LocalDate.now().plusDays(10));
        }

        @Test
        void shouldReturnNull_WhenMakeAnOrderForCostsEmpty_InterimPaymentDate() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder().build()).build();
            LocalDate assistedOrderDate = generator.getInterimPaymentDate(caseData);

            assertThat(assistedOrderDate).isNull();
        }

        @Test
        void shouldReturnNull_WhenMakeAnOrderForCostsListIsEmpty_InterimPaymentDate() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS).build();
            LocalDate assistedOrderDate = generator.getInterimPaymentDate(caseData);

            assertThat(assistedOrderDate).isNull();
        }

        @Test
        void shouldReturnDifferentText_WhenSelected_Defendant_CostsInterimPaymentDate() {
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.COSTS)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900)).build()).build();
            LocalDate assistedOrderDate = generator.getInterimPaymentDate(caseData);

            assertThat(assistedOrderDate).isNull();
        }

        @Test
        void shouldReturnTrueWhenQocsProtectionEnabled(){
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.COSTS)
                                                      .makeAnOrderForCostsYesOrNo(YesOrNo.YES)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900)).build()).build();
            Boolean checkQocsFlag = generator.checkIsQocsProtectionEnabled(caseData);

            assertThat(checkQocsFlag).isTrue();
        }

        @Test
        void shouldReturnFalseWhenQocsProtectionDisabled(){
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.COSTS)
                                                      .makeAnOrderForCostsYesOrNo(YesOrNo.NO)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900)).build()).build();
            Boolean checkQocsFlag = generator.checkIsQocsProtectionEnabled(caseData);

            assertThat(checkQocsFlag).isFalse();
        }

        @Test
        void shouldReturnFalseWhenQocsProtectionDisabled_YesOrNoIsNull(){
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder()
                                                      .makeAnOrderForCostsList(AssistedOrderCostDropdownList.DEFENDANT)
                                                      .assistedOrderCostsMakeAnOrderTopList(
                                                          AssistedOrderCostDropdownList.COSTS)
                                                      .assistedOrderCostsFirstDropdownAmount(BigDecimal.valueOf(78900)).build()).build();
            Boolean checkQocsFlag = generator.checkIsQocsProtectionEnabled(caseData);

            assertThat(checkQocsFlag).isFalse();
        }

        @Test
        void shouldReturnFalseWhenQocsProtectionDisabled_MakeAnOrderForCostsIsNull(){
            CaseData caseData = CaseData.builder().assistedCostTypes(AssistedCostTypesList.MAKE_AN_ORDER_FOR_DETAILED_COSTS)
                .assistedOrderMakeAnOrderForCosts(AssistedOrderCost.builder().build()).build();
            Boolean checkQocsFlag = generator.checkIsQocsProtectionEnabled(caseData);

            assertThat(checkQocsFlag).isFalse();
        }
    }

    @Nested
    class FurtherHearing {

        private List<FinalOrderShowToggle> furtherHearingShowOption = new ArrayList<>();

        @Test
        void shouldReturnNull_When_FurtherHearing_NotSelected() {
            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(null)
                .build();
            Boolean checkToggle = generator.checkFurtherHearingToggle(caseData);
            assertThat(checkToggle).isFalse();
        }

        @Test
        void shouldReturnNull_FurtherHearingOption_Null() {
            furtherHearingShowOption.add(null);
            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(furtherHearingShowOption)
                .build();
            Boolean checkToggle = generator.checkFurtherHearingToggle(caseData);
            assertThat(checkToggle).isFalse();
        }

        @Test
        void shouldReturnNull_When_FurtherHearingOption_NotShow() {
            furtherHearingShowOption.add(FinalOrderShowToggle.HIDE);
            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(furtherHearingShowOption)
                .build();
            Boolean checkToggle = generator.checkFurtherHearingToggle(caseData);
            assertThat(checkToggle).isFalse();
        }

        @Test
        void shouldNotReturnNull_When_FurtherHearingOption_Show() {
            furtherHearingShowOption.add(FinalOrderShowToggle.SHOW);
            CaseData caseData = CaseData.builder()
                .assistedOrderFurtherHearingToggle(furtherHearingShowOption)
                .build();
            Boolean checkToggle = generator.checkFurtherHearingToggle(caseData);
            assertThat(checkToggle).isTrue();
        }
  /*
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
                .code(String.valueOf(UUID.randomUUID())).label("Site Name 2 - Address2 - 28000").build();
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
                .code(String.valueOf(UUID.randomUUID())).label("Site Name 2 - Address2 - 28000").build();
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

        }*/
    }

    @Nested
    class Recitals {
        @Test
        void shouldReturnNull_When_Recitals_NotSelected() {
            CaseData caseData = CaseData.builder()
                .assistedOrderRecitals(null)
                .build();
            Boolean checkToggle = generator.checkRecitalsToggle(caseData);
            assertThat(checkToggle).isFalse();
        }

        @Test
        void shouldReturnNull_RecitalOption_Null() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(null);
            CaseData caseData = CaseData.builder()
                .assistedOrderRecitals(recitalsOrderShowOption)
                .build();
            Boolean checkToggle = generator.checkRecitalsToggle(caseData);
            assertThat(checkToggle).isFalse();
        }

        @Test
        void shouldReturnNull_When_RecitalOption_NotShow() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(FinalOrderShowToggle.HIDE);
            CaseData caseData = CaseData.builder()
                .assistedOrderRecitals(recitalsOrderShowOption)
                .build();
            Boolean checkToggle = generator.checkRecitalsToggle(caseData);
            assertThat(checkToggle).isFalse();
        }

        @Test
        void shouldNotReturnNull_When_RecitalOption_Show() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(FinalOrderShowToggle.SHOW);
            CaseData caseData = CaseData.builder()
                .assistedOrderRecitals(recitalsOrderShowOption)
                .build();
            Boolean checkToggle = generator.checkRecitalsToggle(caseData);
            assertThat(checkToggle).isTrue();
        }

        @Test
        void shouldReturnNull_When_recitalsRecordedIsNull() {
            CaseData caseData = CaseData.builder()
                .assistedOrderRecitalsRecorded(null)
                .build();
            String assistedOrderString = generator.getRecitalRecordedText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnText_When_recitalsRecordedIsNotNull() {
            CaseData caseData = CaseData.builder()
                .assistedOrderRecitalsRecorded(AssistedOrderRecitalRecord.builder().text(RECITAL_RECORDED_TEXT).build())
                .build();
            String assistedOrderString = generator.getRecitalRecordedText(caseData);
            assertThat(assistedOrderString).contains(RECITAL_RECORDED_TEXT);
        }
    }

    @Nested
    class JudgeHeardFrom {

   /*     @Test
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
        } */
    }

    @Nested
    class OrderMadeOn {

        @Test
        void shouldReturnTrueWhenOrderMadeWithInitiative() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.COURTS_INITIATIVE)
                .orderMadeOnOwnInitiative(DetailTextWithDate.builder()
                                              .detailText(TEST_TEXT)
                                              .date(LocalDate.now())
                                              .build())
                .build();
            Boolean checkInitiativeOrWithoutNotice = generator.checkInitiativeOrWithoutNotice(caseData);
            assertThat(checkInitiativeOrWithoutNotice).isTrue();
        }

        @Test
        void shouldReturnTrueWhenOrderMadeWithWithoutNotice() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.WITHOUT_NOTICE)
                .build();
            Boolean checkInitiativeOrWithoutNotice = generator.checkInitiativeOrWithoutNotice(caseData);
            assertThat(checkInitiativeOrWithoutNotice).isTrue();
        }

        @Test
        void shouldReturnFalseWhenOrderMadeWithOherTypeExceptWithOutNoticeOrInitiative() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.NONE)
                .build();
            Boolean checkInitiativeOrWithoutNotice = generator.checkInitiativeOrWithoutNotice(caseData);
            assertThat(checkInitiativeOrWithoutNotice).isFalse();
        }

        @Test
        void shouldReturnTrueWhen_OrderMadeWithInitiative() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.COURTS_INITIATIVE)
                .orderMadeOnOwnInitiative(DetailTextWithDate.builder()
                                              .detailText(TEST_TEXT)
                                              .date(LocalDate.now())
                                              .build())
                .build();
            Boolean checkInitiative = generator.checkInitiative(caseData);
            assertThat(checkInitiative).isTrue();
        }

        @Test
        void shouldReturnFalseWhenOrderMadeWithOherTypeExceptInitiative() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.WITHOUT_NOTICE)
                .build();
            Boolean checkInitiativeOrWithoutNotice = generator.checkInitiative(caseData);
            assertThat(checkInitiativeOrWithoutNotice).isFalse();
        }

        @Test
        void shouldReturnTextWhen_OrderMadeWithInitiative() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.COURTS_INITIATIVE)
                .orderMadeOnOwnInitiative(DetailTextWithDate.builder()
                                              .detailText(TEST_TEXT)
                                              .date(LocalDate.now())
                                              .build())
                .build();
            String orderMadeOnText = generator.getOrderMadeOnText(caseData);
            assertThat(orderMadeOnText).contains(TEST_TEXT);
        }

        @Test
        void shouldReturnTextWhen_OrderMadeWithWithoutNotice() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.WITHOUT_NOTICE)
                .orderMadeOnWithOutNotice(DetailTextWithDate.builder()
                                              .detailText(TEST_TEXT)
                                              .date(LocalDate.now())
                                              .build())
                .build();
            String orderMadeOnText = generator.getOrderMadeOnText(caseData);
            assertThat(orderMadeOnText).contains(TEST_TEXT);
        }

        @Test
        void shouldReturnNullWhen_OrderMadeWithNone() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.NONE)
                .build();
            String orderMadeOnText = generator.getOrderMadeOnText(caseData);
            assertThat(orderMadeOnText).contains("");
        }

        @Test
        void shouldReturnInitiativeDateWhen_OrderMadeWithInitiative() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.COURTS_INITIATIVE)
                .orderMadeOnOwnInitiative(DetailTextWithDate.builder()
                                              .detailText(TEST_TEXT)
                                              .date(LocalDate.now())
                                              .build())
                .build();
            LocalDate orderMadeDate = generator.getOrderMadeCourtInitiativeDate(caseData);
            assertThat(orderMadeDate).isEqualTo(LocalDate.now());
        }

        @Test
        void shouldReturnInitiativeDateNullWhen_OrderMadeWithoutNotice() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.WITHOUT_NOTICE)
                .orderMadeOnWithOutNotice(DetailTextWithDate.builder()
                                              .detailText(TEST_TEXT)
                                              .date(LocalDate.now())
                                              .build())
                .build();
            LocalDate orderMadeDate = generator.getOrderMadeCourtInitiativeDate(caseData);
            assertThat(orderMadeDate).isNull();
        }

        @Test
        void shouldReturnWithoutNoticeDateWhen_OrderMadeWithoutNotice() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.WITHOUT_NOTICE)
                .orderMadeOnWithOutNotice(DetailTextWithDate.builder()
                                              .detailText(TEST_TEXT)
                                              .date(LocalDate.now())
                                              .build())
                .build();
            LocalDate orderMadeDate = generator.getOrderMadeCourtWithOutNoticeDate(caseData);
            assertThat(orderMadeDate).isEqualTo(LocalDate.now());
        }

        @Test
        void shouldReturnWithoutNoticeDateNull_When_OrderMadeWithInitiative() {
            CaseData caseData = CaseData.builder()
                .orderMadeOnOption(OrderMadeOnTypes.COURTS_INITIATIVE)
                .orderMadeOnOwnInitiative(DetailTextWithDate.builder()
                                              .detailText(TEST_TEXT)
                                              .date(LocalDate.now())
                                              .build())
                .build();
            LocalDate orderMadeDate = generator.getOrderMadeCourtWithOutNoticeDate(caseData);
            assertThat(orderMadeDate).isNull();
        }
    }

    @Nested
    class AppealSection {

        @Test
        void shouldReturnNull_When_OrderAppeal_NotSelected() {
            CaseData caseData = CaseData.builder()
                .assistedOrderAppealToggle(null)
                .build();
            Boolean checkToggle = generator.checkAppealToggle(caseData);
            assertThat(checkToggle).isFalse();
        }

        @Test
        void shouldReturnNull_When_OrderAppealOption_Null() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(null);
            CaseData caseData = CaseData.builder()
                .assistedOrderAppealToggle(recitalsOrderShowOption)
                .build();
            Boolean checkToggle = generator.checkAppealToggle(caseData);
            assertThat(checkToggle).isFalse();
        }

        @Test
        void shouldReturnNull_When_OrderAppealOption_NotShow() {
            List<FinalOrderShowToggle> recitalsOrderShowOption = new ArrayList<>();
            recitalsOrderShowOption.add(FinalOrderShowToggle.HIDE);
            CaseData caseData = CaseData.builder()
                .assistedOrderAppealToggle(recitalsOrderShowOption)
                .build();
            Boolean checkToggle = generator.checkAppealToggle(caseData);
            assertThat(checkToggle).isFalse();
        }

        @Test
        void shouldReturnText_WhenSelected_ClaimantOrDefendantAppeal_Claimant() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .appealOrigin(AppealOriginTypes.CLAIMANT).build()).build();
            String assistedOrderString = generator.getClaimantOrDefendantAppeal(caseData);

            assertThat(assistedOrderString).contains("claimant");
        }

        @Test
        void shouldReturnText_WhenSelected_ClaimantOrDefendantAppeal_Defendant() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .appealOrigin(AppealOriginTypes.DEFENDANT).build()).build();
            String assistedOrderString = generator.getClaimantOrDefendantAppeal(caseData);

            assertThat(assistedOrderString).contains("defendant");
        }

        @Test
        void shouldReturnText_WhenSelected_ClaimantOrDefendantAppeal_Other() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .appealOrigin(AppealOriginTypes.OTHER)
                                                                                  .otherOriginText("test other origin text").build()).build();
            String assistedOrderString = generator.getClaimantOrDefendantAppeal(caseData);

            assertThat(assistedOrderString).contains(OTHER_ORIGIN_TEXT);
        }

        @Test
        void shouldReturnNull_When_AppealDetails_Null() {
            CaseData caseData = CaseData.builder().build();
            String assistedOrderString = generator.getClaimantOrDefendantAppeal(caseData);

            assertThat(assistedOrderString).contains("");
        }

        @Test
        void shouldReturnNull_When_AppealOrigin_isNull() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .build()).build();
            String assistedOrderString = generator.getClaimantOrDefendantAppeal(caseData);

            assertThat(assistedOrderString).contains("");
        }

        @Test
        void shouldReturnTrueWhenIsAppealGranted() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .appealOrigin(AppealOriginTypes.DEFENDANT)
                                                                                  .permissionToAppeal(PermissionToAppealTypes.GRANTED).build()).build();
            Boolean isAppealGranted= generator.isAppealGranted(caseData);

            assertThat(isAppealGranted).isTrue();
        }

        @Test
        void shouldReturnFalseWhenAppealIsRefused() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .appealOrigin(AppealOriginTypes.DEFENDANT)
                                                                                  .permissionToAppeal(PermissionToAppealTypes.REFUSED).build()).build();
            Boolean isAppealGranted= generator.isAppealGranted(caseData);

            assertThat(isAppealGranted).isFalse();
        }

        @Test
        void shouldReturnFalseWhenIsAppealNotGranted() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .build()).build();
            Boolean isAppealGranted= generator.isAppealGranted(caseData);

            assertThat(isAppealGranted).isFalse();
        }

        @Test
        void shouldReturnText_WhentableAorBIsSelected_Granted() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .appealOrigin(AppealOriginTypes.DEFENDANT)
                                                                                  .permissionToAppeal(PermissionToAppealTypes.GRANTED)
                                                                                  .appealTypeChoicesForGranted(
                                                                                      AppealTypeChoices.builder()
                                                                                          .assistedOrderAppealJudgeSelection(PermissionToAppealTypes.CIRCUIT_COURT_JUDGE).build()).build()).build();
            String assistedOrderString= generator.checkCircuitOrHighCourtJudge(caseData);

            assertThat(assistedOrderString).contains("A");
        }

        @Test
        void shouldReturnText_WhentableAorBIsSelected_Refused() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .appealOrigin(AppealOriginTypes.DEFENDANT)
                                                                                  .permissionToAppeal(PermissionToAppealTypes.REFUSED)
                                                                                  .appealTypeChoicesForRefused(
                                                                                      AppealTypeChoices.builder()
                                                                                          .assistedOrderAppealJudgeSelectionRefuse(PermissionToAppealTypes.CIRCUIT_COURT_JUDGE).build()).build()).build();
            String assistedOrderString= generator.checkCircuitOrHighCourtJudge(caseData);

            assertThat(assistedOrderString).contains("A");
        }

        @Test
        void shouldReturnText_WhentableBIsSelected_Refused() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .appealOrigin(AppealOriginTypes.DEFENDANT)
                                                                                  .permissionToAppeal(PermissionToAppealTypes.REFUSED)
                                                                                  .appealTypeChoicesForRefused(
                                                                                      AppealTypeChoices.builder()
                                                                                          .assistedOrderAppealJudgeSelectionRefuse(PermissionToAppealTypes.HIGH_COURT_JUDGE).build()).build()).build();
            String assistedOrderString= generator.checkCircuitOrHighCourtJudge(caseData);

            assertThat(assistedOrderString).contains("B");
        }

        @Test
        void shouldReturnText_WhentableBIsSelected_Granted() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .appealOrigin(AppealOriginTypes.DEFENDANT)
                                                                                  .permissionToAppeal(PermissionToAppealTypes.GRANTED)
                                                                                  .appealTypeChoicesForGranted(
                                                                                      AppealTypeChoices.builder()
                                                                                          .assistedOrderAppealJudgeSelection(PermissionToAppealTypes.HIGH_COURT_JUDGE).build()).build()).build();
            String assistedOrderString= generator.checkCircuitOrHighCourtJudge(caseData);

            assertThat(assistedOrderString).contains("B");
        }

        @Test
        void shouldReturnAppealDate_WhentableA_Granted() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .appealOrigin(AppealOriginTypes.DEFENDANT)
                                                                                  .permissionToAppeal(PermissionToAppealTypes.GRANTED)
                                                                                  .appealTypeChoicesForGranted(
                                                                                      AppealTypeChoices.builder()
                                                                                          .assistedOrderAppealJudgeSelection(PermissionToAppealTypes.CIRCUIT_COURT_JUDGE)
                                                                                          .appealChoiceOptionA(
                                                                                              AppealTypeChoiceList.builder().appealGrantedRefusedDate(LocalDate.now().plusDays(14)).build()).build()).build()).build();
            LocalDate assistedOrderAppealDate= generator.getAppealDate(caseData);

            assertThat(assistedOrderAppealDate).isEqualTo(LocalDate.now().plusDays(14));
        }

        @Test
        void shouldReturnAppealDate_WhentableB_Granted() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .appealOrigin(AppealOriginTypes.DEFENDANT)
                                                                                  .permissionToAppeal(PermissionToAppealTypes.GRANTED)
                                                                                  .appealTypeChoicesForGranted(
                                                                                      AppealTypeChoices.builder()
                                                                                          .assistedOrderAppealJudgeSelection(PermissionToAppealTypes.HIGH_COURT_JUDGE)
                                                                                          .appealChoiceOptionB(
                                                                                              AppealTypeChoiceList.builder().appealGrantedRefusedDate(LocalDate.now().plusDays(14)).build()).build()).build()).build();
            LocalDate assistedOrderAppealDate= generator.getAppealDate(caseData);

            assertThat(assistedOrderAppealDate).isEqualTo(LocalDate.now().plusDays(14));
        }
        @Test
        void shouldReturnAppealDate_WhentableA_Refused() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .appealOrigin(AppealOriginTypes.DEFENDANT)
                                                                                  .permissionToAppeal(PermissionToAppealTypes.REFUSED)
                                                                                  .appealTypeChoicesForRefused(
                                                                                      AppealTypeChoices.builder()
                                                                                          .assistedOrderAppealJudgeSelectionRefuse(PermissionToAppealTypes.CIRCUIT_COURT_JUDGE)
                                                                                          .appealChoiceOptionA(
                                                                                              AppealTypeChoiceList.builder().appealGrantedRefusedDate(LocalDate.now().plusDays(14)).build())
                                                                                          .build()).build()).build();
            LocalDate assistedOrderAppealDate= generator.getAppealDate(caseData);

            assertThat(assistedOrderAppealDate).isEqualTo(LocalDate.now().plusDays(14));
        }

        @Test
        void shouldReturnAppealDate_WhentableB_Refused() {
            CaseData caseData = CaseData.builder().assistedOrderAppealDetails(AssistedOrderAppealDetails.builder()
                                                                                  .appealOrigin(AppealOriginTypes.DEFENDANT)
                                                                                  .permissionToAppeal(PermissionToAppealTypes.REFUSED)
                                                                                  .appealTypeChoicesForRefused(
                                                                                      AppealTypeChoices.builder()
                                                                                          .assistedOrderAppealJudgeSelectionRefuse(PermissionToAppealTypes.HIGH_COURT_JUDGE)
                                                                                          .appealChoiceOptionB(
                                                                                              AppealTypeChoiceList.builder().appealGrantedRefusedDate(LocalDate.now().plusDays(14)).build())
                                                                                          .build()).build()).build();
            LocalDate assistedOrderAppealDate= generator.getAppealDate(caseData);

            assertThat(assistedOrderAppealDate).isEqualTo(LocalDate.now().plusDays(14));
        }

        @Test
        void shouldReturnNullAppealDate_WhenAssistedOrderAppealDetailsAreNull() {
            CaseData caseData = CaseData.builder().build();
            LocalDate assistedOrderAppealDate= generator.getAppealDate(caseData);

            assertThat(assistedOrderAppealDate).isNull();
        }
    }


    @Nested
    class OrderMadeDate {

        @Test
        void shouldReturnTrueWhenOrderMadeSelectionIsSingleDate() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().singleDateSelection(
                    AssistedOrderDateHeard.builder().singleDate(LocalDate.now()).build()).build())
                .build();
            Boolean isSingleDate = generator.checkIsSingleDate(caseData);
            assertThat(isSingleDate).isTrue();
        }

        @Test
        void shouldReturnFalseWhenOrderMadeSelectionIsNotSingleDate() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().dateRangeSelection(
                    AssistedOrderDateHeard.builder().dateRangeFrom(LocalDate.now().minusDays(10)).build()).build())
                .build();
            Boolean isSingleDate = generator.checkIsSingleDate(caseData);
            assertThat(isSingleDate).isFalse();
        }

        @Test
        void shouldReturnFalseWhenOrderMadeSelectionIsNotExist() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.NO)
                .build();
            Boolean isSingleDate = generator.checkIsSingleDate(caseData);
            assertThat(isSingleDate).isFalse();
        }

        @Test
        void shouldReturnSingleDateWhenOrderMadeSelectionIsSingleDate() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().singleDateSelection(
                    AssistedOrderDateHeard.builder().singleDate(LocalDate.now()).build()).build())
                .build();
            LocalDate assistedOrderDate = generator.getOrderMadeSingleDate(caseData);
            assertThat(assistedOrderDate).isEqualTo(LocalDate.now());
        }

        @Test
        void shouldReturnNullWhenOrderMadeSelectionIsNo() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.NO)
                .build();
            LocalDate assistedOrderDate = generator.getOrderMadeSingleDate(caseData);
            assertThat(assistedOrderDate).isNull();
        }

        @Test
        void shouldReturnNullWhenOrderMadeHeardsDetailsAreNull() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().build())
                .build();
            LocalDate assistedOrderDate = generator.getOrderMadeSingleDate(caseData);
            assertThat(assistedOrderDate).isNull();
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
        void shouldReturnTrueWhenOrderMadeSelectionIsDateRange() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().dateRangeSelection(
                    AssistedOrderDateHeard.builder().dateRangeFrom(LocalDate.now().minusDays(10))
                        .dateRangeTo(LocalDate.now().minusDays(5)).build()).build())
                .build();
            Boolean isSingleDate = generator.checkIsDateRange(caseData);
            assertThat(isSingleDate).isTrue();
        }

        @Test
        void shouldReturnFalseWhenOrderMadeSelectionIsNotDateRange() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().singleDateSelection(
                    AssistedOrderDateHeard.builder().singleDate(LocalDate.now().minusDays(10)).build()).build())
                .build();
            Boolean isSingleDate = generator.checkIsDateRange(caseData);
            assertThat(isSingleDate).isFalse();
        }

        @Test
        void shouldReturnFalseWhenOrderMadeSelectionIsNotExistForDateRange() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.NO)
                .build();
            Boolean isSingleDate = generator.checkIsDateRange(caseData);
            assertThat(isSingleDate).isFalse();
        }

        @Test
        void shouldReturnDateRangeFrom_WhenOrderMadeSelectionIsDateRange() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().dateRangeSelection(
                    AssistedOrderDateHeard.builder().dateRangeFrom(LocalDate.now().minusDays(10))
                        .dateRangeTo(LocalDate.now().minusDays(5)).build()).build())
                .build();
            LocalDate dateRange = generator.getOrderMadeDateRangeFrom(caseData);
            assertThat(dateRange).isEqualTo(LocalDate.now().minusDays(10));
        }

        @Test
        void shouldNotReturnDateRangeFrom_WhenOrderMadeSelectionIsDateRange() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().dateRangeSelection(
                    AssistedOrderDateHeard.builder()
                        .dateRangeTo(LocalDate.now().minusDays(5)).build()).build())
                .build();
            LocalDate dateRange = generator.getOrderMadeDateRangeFrom(caseData);
            assertThat(dateRange).isNull();
        }

        @Test
        void shouldNotReturnDateRangeFrom_WhenOrderMadeSelectionIsNull() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.NO)
                .build();
            LocalDate dateRange = generator.getOrderMadeDateRangeFrom(caseData);
            assertThat(dateRange).isNull();
        }

        @Test
        void shouldReturnDateRangeTo_WhenOrderMadeSelectionIsDateRange() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().dateRangeSelection(
                    AssistedOrderDateHeard.builder().dateRangeFrom(LocalDate.now().minusDays(10))
                        .dateRangeTo(LocalDate.now().minusDays(5)).build()).build())
                .build();
            LocalDate dateRange = generator.getOrderMadeDateRangeTo(caseData);
            assertThat(dateRange).isEqualTo(LocalDate.now().minusDays(10));
        }

        @Test
        void shouldNotReturnDateRangeTo_WhenOrderMadeSelectionIsDateRange() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().dateRangeSelection(
                    AssistedOrderDateHeard.builder()
                        .dateRangeFrom(LocalDate.now().minusDays(5)).build()).build())
                .build();
            LocalDate dateRange = generator.getOrderMadeDateRangeTo(caseData);
            assertThat(dateRange).isNull();
        }

        @Test
        void shouldNotReturnDateRangeTo_WhenOrderMadeSelectionIsNull() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.NO)
                .build();
            LocalDate dateRange = generator.getOrderMadeDateRangeTo(caseData);
            assertThat(dateRange).isNull();
        }

        @Test
        void shouldReturnTrueWhenOrderMadeSelectionIsBeSpokeRange() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().beSpokeRangeSelection(
                    AssistedOrderDateHeard.builder().beSpokeRangeText("beSpoke text").build()).build())
                .build();
            Boolean isBeSpokeRange = generator.checkIsBeSpokeRange(caseData);
            assertThat(isBeSpokeRange).isTrue();
        }

        @Test
        void shouldReturnFalseWhenOrderMadeSelectionIsNotBeSpokeRange() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().singleDateSelection(
                    AssistedOrderDateHeard.builder().singleDate(LocalDate.now().minusDays(10)).build()).build())
                .build();
            Boolean isBeSpokeRange = generator.checkIsBeSpokeRange(caseData);
            assertThat(isBeSpokeRange).isFalse();
        }

        @Test
        void shouldReturnFalseWhenOrderMadeSelectionIsNotExistForBeSpokeRange() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.NO)
                .build();
            Boolean isBeSpokeRange = generator.checkIsBeSpokeRange(caseData);
            assertThat(isBeSpokeRange).isFalse();
        }

        @Test
        void shouldReturnBeSpokeTextWhenOrderMadeSelectionIsBeSpokeRange() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().beSpokeRangeSelection(
                    AssistedOrderDateHeard.builder().beSpokeRangeText("beSpoke text").build()).build())
                .build();
            String beSpokeRangeText = generator.getOrderMadeBeSpokeText(caseData);
            assertThat(beSpokeRangeText).contains("beSpoke text");
        }

        @Test
        void shouldNotReturnBeSpokeTextWhenOrderMadeSelectionIsNo() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.NO)
                .build();
            String beSpokeRangeText = generator.getOrderMadeBeSpokeText(caseData);
            assertThat(beSpokeRangeText).isNull();
        }

        @Test
        void shouldReturnNullWhenOrderMadeSelectionIsBeSpokeRangeAndIsNull() {
            CaseData caseData = CaseData.builder()
                .assistedOrderMadeSelection(YesOrNo.YES)
                .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().singleDateSelection(
                    AssistedOrderDateHeard.builder().singleDate(LocalDate.now()).build()).build())
                .build();
            String beSpokeRangeText = generator.getOrderMadeBeSpokeText(caseData);
            assertThat(beSpokeRangeText).isNull();
        }
    }

    @Nested
    class ReasonText {

        @Test
        void shouldReturnNull_When_GiveReasons_Null() {
            CaseData caseData = CaseData.builder()
                .assistedOrderGiveReasonsYesNo(null)
                .build();
            String assistedOrderString = generator.getReasonText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_GiveReasons_SelectedOption_No() {
            CaseData caseData = CaseData.builder()
                .assistedOrderGiveReasonsYesNo(YesOrNo.NO)
                .build();
            String assistedOrderString = generator.getReasonText(caseData);
            assertNull(assistedOrderString);
        }

        @Test
        void shouldReturnNull_When_GiveReasonsText_Null() {
            CaseData caseData = CaseData.builder()
                .assistedOrderGiveReasonsYesNo(YesOrNo.YES)
                .assistedOrderGiveReasonsDetails(AssistedOrderGiveReasonsDetails.builder().build())
                .build();
            String assistedOrderString = generator.getReasonText(caseData);
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
            String assistedOrderString = generator.getReasonText(caseData);
            assertNotNull(assistedOrderString);
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
        assertThat(name).startsWith("General_order_for_application_");
        assertThat(name).endsWith(".pdf");
    }

    @Test
    void test_getDateFormatted() {
        String dateString = generator.getDateFormatted(LocalDate.EPOCH);
        assertThat(dateString).isEqualTo(" 1 January 1970");
    }

    @Test
    void test_getTemplate() {
        assertThat(generator.getTemplate()).isEqualTo(DocmosisTemplates.ASSISTED_ORDER_FORM);
    }
}
