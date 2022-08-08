package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
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
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.START_BUSINESS_PROCESS_MAKE_DECISION;

@Service
@RequiredArgsConstructor
public class StartBusinessProcessCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(START_BUSINESS_PROCESS_MAKE_DECISION);
    public static final String BUSINESS_PROCESS = "businessProcess";

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::startBusinessProcess);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private final CaseDetailsConverter caseDetailsConverter;

    private CallbackResponse startBusinessProcess(CallbackParams callbackParams) {
        CaseDetails caseDetails = callbackParams.getRequest().getCaseDetails();
        CaseData data = caseDetailsConverter.toCaseData(caseDetails);
        Map<String, Object> output = caseDetails.getData();
        BusinessProcess businessProcess = data.getBusinessProcess();

        switch (businessProcess.getStatusOrDefault()) {
            case READY:
            case DISPATCHED: {
                output.put(BUSINESS_PROCESS, businessProcess.start());
                return AboutToStartOrSubmitCallbackResponse.builder()
                    .data(output)
                    .build();
            }
            default:
                return AboutToStartOrSubmitCallbackResponse.builder()
                    .errors(List.of("Concurrency Error"))
                    .build();
        }
    }
}
