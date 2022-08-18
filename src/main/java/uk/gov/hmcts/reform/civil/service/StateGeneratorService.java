package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.model.CaseData;

import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_ADD_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_DISMISSED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_ADDITIONAL_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_DIRECTIONS_ORDER_DOCS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.LISTING_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PROCEEDS_IN_HERITAGE;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.LIST_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_AN_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STRIKE_OUT;

@Service
@RequiredArgsConstructor
public class StateGeneratorService {

    private final JudicialDecisionHelper judicialDecisionHelper;

    public CaseState getCaseStateForEndJudgeBusinessProcess(CaseData data) {
        GAJudgeDecisionOption decision;
        if (data.getJudicialDecision() != null) {
            decision = data.getJudicialDecision().getDecision();
        } else {
            decision = null;
        }

        if (isCaseDismissed(data)) {
            return APPLICATION_DISMISSED;
        } else if (decision == MAKE_AN_ORDER && data.getJudicialDecisionMakeOrder()
            .getMakeAnOrder().equals(GIVE_DIRECTIONS_WITHOUT_HEARING)) {
            return AWAITING_DIRECTIONS_ORDER_DOCS;
        } else if (decision == REQUEST_MORE_INFO) {

            if (judicialDecisionHelper.isApplicationUncloakedWithAdditionalFee(data)
                && data.getGeneralAppPBADetails().getAdditionalPaymentDetails() == null) {
                return APPLICATION_ADD_PAYMENT;
            }
            return AWAITING_ADDITIONAL_INFORMATION;
        } else if (decision == MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS) {
            return AWAITING_WRITTEN_REPRESENTATIONS;
        } else if (decision == LIST_FOR_A_HEARING) {
            return  LISTING_FOR_A_HEARING;
        } else if (decision == MAKE_AN_ORDER && data.getJudicialDecisionMakeOrder() != null
            && APPROVE_OR_EDIT.equals(data.getJudicialDecisionMakeOrder().getMakeAnOrder())) {
            if (YesOrNo.YES.equals(data.getParentClaimantIsApplicant())
                && data.getGeneralAppType().getTypes().contains(STRIKE_OUT)) {
                return PROCEEDS_IN_HERITAGE;
            } else {
                return ORDER_MADE;
            }
        }
        return data.getCcdState();
    }

    private boolean isCaseDismissed(CaseData caseData) {
        boolean isJudicialDecisionNotNull = caseData.getJudicialDecisionMakeOrder() != null
            && caseData
                .getJudicialDecisionMakeOrder()
                .getMakeAnOrder() != null;

        boolean isJudicialDecisionMakeOrderIsDismissed = isJudicialDecisionNotNull
            && caseData
                .getJudicialDecisionMakeOrder()
                .getMakeAnOrder()
                .equals(DISMISS_THE_APPLICATION);

        return isJudicialDecisionNotNull && isJudicialDecisionMakeOrderIsDismissed;
    }

}
