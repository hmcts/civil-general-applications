package uk.gov.hmcts.reform.civil.service.flowstate;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.civil.enums.PaymentStatus;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class FlowPredicateTest {

    @Test
    public void testJudgeNotMadeDismissalOrder_noJudicialDecision() {
        CaseData caseData = CaseData.builder().build();

        boolean result = FlowPredicate.judgeMadeDismissalOrder.test(caseData);

        assertThat(result).isFalse();
    }

    @Test
    public void testJudgeNotMadeDismissalOrder_decisionRequestMoreInfo() {
        CaseData caseData = CaseData.builder()
            .judicialDecision(GAJudicialDecision.builder()
                                  .decision(GAJudgeDecisionOption.REQUEST_MORE_INFO)
                                  .build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .makeAnOrder(GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION)
                                           .build()).build();

        boolean result = FlowPredicate.judgeMadeDismissalOrder.test(caseData);

        assertThat(result).isFalse();
    }

    @Test
    public void testJudgeNotMadeDismissalOrder_approveOrEdit() {
        CaseData caseData = CaseData.builder()
            .judicialDecision(GAJudicialDecision.builder()
                                  .decision(GAJudgeDecisionOption.MAKE_AN_ORDER)
                                  .build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .makeAnOrder(GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT)
                                           .build()).build();

        boolean result = FlowPredicate.judgeMadeDismissalOrder.test(caseData);

        assertThat(result).isFalse();
    }

    @Test
    public void testJudgeMadeDismissalOrder() {
        CaseData caseData = CaseData.builder()
            .judicialDecision(GAJudicialDecision.builder()
                                  .decision(GAJudgeDecisionOption.MAKE_AN_ORDER)
                                  .build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                  .makeAnOrder(GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION)
                                  .build()).build();

        boolean result = FlowPredicate.judgeMadeDismissalOrder.test(caseData);

        assertThat(result).isTrue();
    }

    @Test
    public void testIsWelshJudgeDecision_dismissalOrder() {
        CaseData caseData = CaseData.builder()
            .isGaApplicantLip(YesOrNo.YES)
            .applicantBilingualLanguagePreference(YesOrNo.YES)
            .judicialDecision(GAJudicialDecision.builder()
                                  .decision(GAJudgeDecisionOption.MAKE_AN_ORDER)
                                  .build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .makeAnOrder(GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION)
                                           .build()).build();

        boolean result = FlowPredicate.isWelshJudgeDecision.test(caseData);

        assertThat(result).isTrue();
    }

    @Test
    public void testIsWelshJudgeDecision_ListForHearing() {
        CaseData caseData = CaseData.builder()
            .isGaApplicantLip(YesOrNo.YES)
            .applicantBilingualLanguagePreference(YesOrNo.YES)
            .judicialDecision(GAJudicialDecision.builder()
                                  .decision(GAJudgeDecisionOption.LIST_FOR_A_HEARING)
                                  .build())
            .judicialListForHearing(GAJudgesHearingListGAspec.builder()
                                        .judgeHearingCourtLocationText1("test")
                                        .judgeHearingTimeEstimateText1("test")
                                        .hearingPreferencesPreferredTypeLabel1("test")
                                        .judgeHearingSupportReqText1("test")
                                        .build()).build();

        boolean result = FlowPredicate.isWelshJudgeDecision.test(caseData);

        assertThat(result).isTrue();
    }

    @Test
    public void testIsFreeApplication() {
        CaseData caseData = CaseData.builder()
            .isGaApplicantLip(YesOrNo.YES)
            .generalAppType(GAApplicationType.builder().types(Collections.singletonList(GeneralApplicationTypes.ADJOURN_HEARING)).build())
            .generalAppPBADetails(GAPbaDetails.builder()
                                        .paymentDetails(PaymentDetails.builder()
                                                            .status(PaymentStatus.SUCCESS).build())
                                        .fee(Fee.builder().code("FREE").build()).build())
            .applicantBilingualLanguagePreference(YesOrNo.YES).build();

        boolean result = FlowPredicate.isFreeFeeWelshApplication.test(caseData);

        assertThat(result).isTrue();
    }

    @Test
    public void testIsNotFreeApplication() {
        CaseData caseData = CaseData.builder()
            .isGaApplicantLip(YesOrNo.YES)
            .generalAppType(GAApplicationType.builder().types(Collections.singletonList(GeneralApplicationTypes.ADJOURN_HEARING)).build())
            .generalAppPBADetails(GAPbaDetails.builder()
                                      .paymentDetails(PaymentDetails.builder()
                                                          .status(PaymentStatus.SUCCESS).build())
                                      .fee(Fee.builder().code("Not_Free").build()).build())
            .applicantBilingualLanguagePreference(YesOrNo.YES).build();

        boolean result = FlowPredicate.isFreeFeeWelshApplication.test(caseData);

        assertThat(result).isFalse();
    }
}
