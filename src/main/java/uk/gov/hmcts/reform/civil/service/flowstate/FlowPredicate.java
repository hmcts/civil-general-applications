package uk.gov.hmcts.reform.civil.service.flowstate;

import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.function.Predicate;

import static uk.gov.hmcts.reform.civil.enums.PaymentStatus.SUCCESS;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.FREE_FORM_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.LIST_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_AN_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;

public class FlowPredicate {

    private FlowPredicate() {
        //Utility class
    }

    public static final Predicate<CaseData> withOutNoticeApplication = caseData ->
        caseData.getGeneralAppInformOtherParty() != null
            && caseData.getGeneralAppRespondentAgreement().getHasAgreed() == YES
            || (caseData.getGeneralAppInformOtherParty() != null
            && caseData.getGeneralAppInformOtherParty().getIsWithNotice() == NO);

    public static final Predicate<CaseData> withNoticeApplication = caseData ->
        caseData.getGeneralAppInformOtherParty() != null
            && caseData.getGeneralAppRespondentAgreement().getHasAgreed() == NO
            || (caseData.getGeneralAppInformOtherParty() != null
            && caseData.getGeneralAppInformOtherParty().getIsWithNotice() == YES);

    public static final Predicate<CaseData> paymentSuccess = caseData ->
        caseData.getGeneralAppPBADetails() != null
            && caseData.getGeneralAppPBADetails().getPaymentDetails() != null
            && caseData.getGeneralAppPBADetails().getPaymentDetails().getStatus() == SUCCESS;

    public static final Predicate<CaseData> judgeMadeDecision = caseData ->
        caseData.getJudicialDecision() != null;

    public static final Predicate<CaseData> judgeMadeListingForHearing = caseData ->
        caseData.getJudicialDecision() != null
            && caseData.getJudicialDecision().getDecision().equals(LIST_FOR_A_HEARING)
            && caseData.getJudicialListForHearing() != null;

    public static final Predicate<CaseData> judgeRequestAdditionalInfo = caseData ->
        caseData.getJudicialDecision() != null
            && caseData.getJudicialDecision().getDecision().equals(REQUEST_MORE_INFO);

    public static final Predicate<CaseData> judgeMadeDirections = caseData ->
        caseData.getJudicialDecision() != null
            && caseData.getJudicialDecision().getDecision().equals(MAKE_AN_ORDER)
            && caseData.getJudicialDecisionMakeOrder().getMakeAnOrder().equals(GIVE_DIRECTIONS_WITHOUT_HEARING);

    public static final Predicate<CaseData> judgeMadeOrder = caseData ->
        caseData.getJudicialDecision() != null
            && (caseData.getJudicialDecision().getDecision().equals(MAKE_AN_ORDER)
            && caseData.getJudicialDecisionMakeOrder().getMakeAnOrder().equals(APPROVE_OR_EDIT))
            || (caseData.getJudicialDecision().getDecision().equals(FREE_FORM_ORDER))
            || (caseData.getFinalOrderSelection() != null);
    
    public static final Predicate<CaseData> judgeMadeWrittenRep = caseData ->
        caseData.getJudicialDecision() != null
            && caseData.getJudicialDecision().getDecision().equals(MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS);
}
