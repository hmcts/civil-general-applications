package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.Time;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPLICATION_PROCEEDS_IN_HERITAGE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PROCEEDS_IN_HERITAGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationProceedsInHeritageEventCallbackHandler extends CallbackHandler {

    private final ObjectMapper objectMapper;
    private final Time time;

    private static final List<CaseEvent> EVENTS = singletonList(APPLICATION_PROCEEDS_IN_HERITAGE);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START), this::emptyCallbackResponse,
            callbackKey(ABOUT_TO_SUBMIT), this::changeApplicationState,
            callbackKey(SUBMITTED), this::emptySubmittedCallbackResponse
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse changeApplicationState(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        Long caseId = caseData.getCcdCaseReference();

        log.info("Changing state to APPLICATION_CLOSED for caseId: {}", caseId);

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder
            .businessProcess(BusinessProcess.ready(APPLICATION_PROCEEDS_IN_HERITAGE))
            .applicationTakenOfflineDate(time.now());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .state(PROCEEDS_IN_HERITAGE.toString())
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }
}
