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

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_DOC_UPLOAD_BUSINESS_PROCESS_GASPEC;

@Service
@Slf4j
@RequiredArgsConstructor
public class EndGaDocUploadProcessCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(END_DOC_UPLOAD_BUSINESS_PROCESS_GASPEC);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::evaluateReady);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse evaluateReady(CallbackParams callbackParams) {
        log.info("in end of doc upload callback handler for caseId {}",
                 callbackParams.getCaseData().getCcdCaseReference());
        Map<String, Object> output = callbackParams.getRequest().getCaseDetails().getData();
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(output)
            .build();
    }
}
