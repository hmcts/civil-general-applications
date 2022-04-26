package uk.gov.hmcts.reform.civil.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.model.CaseData;

import static org.apache.commons.lang.StringUtils.isBlank;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_ADDITIONAL_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_DIRECTIONS_ORDER_DOCS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_AN_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;

@Service
public class StateGeneratorService {

    public CaseState getCaseStateForEndJudgeBusinessProcess(CaseData data) {
        GAJudgeDecisionOption decision = data.getJudicialDecisionOption();
        String directionsText = data.getJudicialDecisionMakeOrderDirectionsText();

        if (decision == MAKE_AN_ORDER && !isBlank(directionsText)) {
            return AWAITING_DIRECTIONS_ORDER_DOCS;
        }
        if (decision == REQUEST_MORE_INFO) {
            return AWAITING_ADDITIONAL_INFORMATION;
        }
        if (decision == MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS) {
            return AWAITING_WRITTEN_REPRESENTATIONS;
        }
        return data.getCcdState();
    }
}
