package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.START_HEARING_SCHEDULED_BUSINESS_PROCESS;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartHearingScheduledBusinessProcessCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(START_HEARING_SCHEDULED_BUSINESS_PROCESS);
    public static final String BUSINESS_PROCESS = "businessProcess";

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::startHearingScheduledBusinessProcess);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private final CaseDetailsConverter caseDetailsConverter;

    private CallbackResponse startHearingScheduledBusinessProcess(CallbackParams callbackParams) {
        CaseData data = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        BusinessProcess businessProcess = data.getBusinessProcess();

        log.info("Start {} business process for caseId: {}", businessProcess, callbackParams.getCaseData().getCcdCaseReference());

        switch (businessProcess.getStatusOrDefault()) {
            case READY:
            case DISPATCHED:
                return evaluateReady(callbackParams, businessProcess);
            default:
                return AboutToStartOrSubmitCallbackResponse.builder()
                    .errors(List.of("Concurrency Error"))
                    .build();

        }
    }

    private CallbackResponse evaluateReady(CallbackParams callbackParams, BusinessProcess businessProcess) {
        Map<String, Object> output = callbackParams.getRequest().getCaseDetails().getData();
        output.put(BUSINESS_PROCESS, businessProcess.start());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(output)
            .build();
    }
}
