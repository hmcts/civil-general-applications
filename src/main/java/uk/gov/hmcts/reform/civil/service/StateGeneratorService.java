package uk.gov.hmcts.reform.civil.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.model.CaseData;

import static org.apache.commons.lang.StringUtils.isBlank;
import static uk.gov.hmcts.reform.civil.enums.CaseState.*;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_AN_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION;

@Service
public class StateGeneratorService {

    public CaseState getCaseStateForEndJudgeBusinessProcess(CaseData data) {
        GAJudgeDecisionOption decision;
        String directionsText;
        Boolean hasDismissedCase = false;
        if (data.getJudicialDecisionMakeOrder() != null) {
            directionsText = data.getJudicialDecisionMakeOrder().getDirectionsText();
            hasDismissedCase = data.getJudicialDecisionMakeOrder().getMakeAnOrder().getDisplayedValue() == DISMISS_THE_APPLICATION.getDisplayedValue() ? true : false;
        } else {
            directionsText = null;
        }
        if (data.getJudicialDecision() != null) {
            decision = data.getJudicialDecision().getDecision();
        } else {
            decision = null;
        }

        if (hasDismissedCase ){
            return APPLICATION_DISMISSED;
        } else if (decision == MAKE_AN_ORDER && !isBlank(directionsText)) {
            return AWAITING_DIRECTIONS_ORDER_DOCS;
        } else if (decision == REQUEST_MORE_INFO) {
            return AWAITING_ADDITIONAL_INFORMATION;
        } else if (decision == MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS) {
            return AWAITING_WRITTEN_REPRESENTATIONS;
        }
        return data.getCcdState();
    }
}
