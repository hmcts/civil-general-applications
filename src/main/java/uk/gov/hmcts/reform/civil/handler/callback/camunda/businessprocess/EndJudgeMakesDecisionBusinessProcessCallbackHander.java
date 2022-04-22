package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_JUDGE_BUSINESS_PROCESS_GASPEC;
import static uk.gov.hmcts.reform.civil.enums.CaseState.*;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.*;

@Service
@RequiredArgsConstructor
public class EndJudgeMakesDecisionBusinessProcessCallbackHander extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(END_JUDGE_BUSINESS_PROCESS_GASPEC);

    private final CaseDetailsConverter caseDetailsConverter;
    private final ParentCaseUpdateHelper parentCaseUpdateHelper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::endJudgeBusinessProcess);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse endJudgeBusinessProcess(CallbackParams callbackParams) {
        CaseData data = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        parentCaseUpdateHelper.updateParentWithGAState(data, getNewStateDependingOn(data).getDisplayedValue());
        return evaluateReady(callbackParams, getNewStateDependingOn(data));
    }

    private CaseState getNewStateDependingOn(CaseData data) {
        GAJudgeDecisionOption decision = data.getJudicialDecision().getDecision();
        String directionsText = data.getJudicialDecisionMakeOrder().getDirectionsText();

        if (decision == MAKE_AN_ORDER && !StringUtils.isBlank(directionsText)) {
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

    private CallbackResponse evaluateReady(CallbackParams callbackParams,
                                           CaseState newState) {
        Map<String, Object> output = callbackParams.getRequest().getCaseDetails().getData();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .state(newState.toString())
            .data(output)
            .build();
    }
}
