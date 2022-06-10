package uk.gov.hmcts.reform.civil.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.model.CaseData;

import static org.apache.commons.lang.StringUtils.isBlank;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_DISMISSED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_ADDITIONAL_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_DIRECTIONS_ORDER_DOCS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.LISTING_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_AN_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.LIST_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION;

@Service
public class StateGeneratorService {

    public CaseState getCaseStateForEndJudgeBusinessProcess(CaseData data) {
        GAJudgeDecisionOption decision;
        String directionsText;
        if (data.getJudicialDecisionMakeOrder() != null) {
            directionsText = data.getJudicialDecisionMakeOrder().getDirectionsText();
        } else {
            directionsText = null;
        }
        if (data.getJudicialDecision() != null) {
            decision = data.getJudicialDecision().getDecision();
        } else {
            decision = null;
        }

        if (isCaseDismissed(data)) {
            return APPLICATION_DISMISSED;
        } else if (decision == MAKE_AN_ORDER && !isBlank(directionsText)) {
            return AWAITING_DIRECTIONS_ORDER_DOCS;
        } else if (decision == REQUEST_MORE_INFO) {
            return AWAITING_ADDITIONAL_INFORMATION;
        } else if (decision == MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS) {
            return AWAITING_WRITTEN_REPRESENTATIONS;
        } else if (decision == LIST_FOR_A_HEARING) {
            return  LISTING_FOR_A_HEARING;
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
