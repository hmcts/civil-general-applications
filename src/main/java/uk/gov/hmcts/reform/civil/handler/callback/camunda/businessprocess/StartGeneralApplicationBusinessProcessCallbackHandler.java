package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.START_BUSINESS_PROCESS_MAKE_DECISION;

@Service
@RequiredArgsConstructor
public class StartGeneralApplicationBusinessProcessCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(START_BUSINESS_PROCESS_MAKE_DECISION);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::startGeneralApplicationMakeDecisionBusinessProcess);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private final CaseDetailsConverter caseDetailsConverter;

    private CallbackResponse startGeneralApplicationMakeDecisionBusinessProcess(CallbackParams callbackParams) {
        CaseData data = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());

        if (data.getBusinessProcess() != null) {
            if (data.getBusinessProcess().getStatus() == BusinessProcessStatus.FINISHED) {
                BusinessProcess businessProcess = data.getBusinessProcess();
                businessProcess.start();
            }
        }
        return evaluateReady(callbackParams, data);
    }

    private AboutToStartOrSubmitCallbackResponse throwConcurrenyError() {
        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(List.of("Concurrency Error"))
            .build();
    }

    private CallbackResponse evaluateReady(CallbackParams callbackParams,
                                           CaseData data) {
        Map<String, Object> output = callbackParams.getRequest().getCaseDetails().getData();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(output)
            .build();
    }
}
