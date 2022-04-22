package uk.gov.hmcts.reform.civil.handler.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;

@Service
@RequiredArgsConstructor
public class MoveAwaitingRespToJudicialDecEventCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = singletonList(CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION);

    private final ObjectMapper objectMapper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::dispatchBusinessProcess
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse dispatchBusinessProcess(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .state(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION.toString())
            .build();
    }
}
