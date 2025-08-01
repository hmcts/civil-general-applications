package uk.gov.hmcts.reform.civil.service.flowstate;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;

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
}
