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
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.Time;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPLICATION_PROCEEDS_IN_HERITAGE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAIN_CASE_CLOSED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_CLOSED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_DISMISSED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.LISTING_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PROCEEDS_IN_HERITAGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationProceedsInHeritageEventCallbackHandler extends CallbackHandler {

    private final ObjectMapper objectMapper;
    private final Time time;

    private static final List<CaseEvent> EVENTS = singletonList(APPLICATION_PROCEEDS_IN_HERITAGE);
    private static  final List<CaseState> NON_LIVE_STATES =
        List.of(APPLICATION_CLOSED, PROCEEDS_IN_HERITAGE, ORDER_MADE, LISTING_FOR_A_HEARING, APPLICATION_DISMISSED);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::changeApplicationState
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse changeApplicationState(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        Long caseId = caseData.getCcdCaseReference();

        if (!NON_LIVE_STATES.contains(caseData.getCcdState())) {
            log.info("Changing state to APPLICATION_CLOSED for caseId: {}", caseId);

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            caseDataBuilder
                .businessProcess(
                    BusinessProcess.builder()
                        .camundaEvent(APPLICATION_PROCEEDS_IN_HERITAGE.name())
                        .status(BusinessProcessStatus.FINISHED)
                        .build())
                .applicationTakenOfflineDate(time.now());

            return AboutToStartOrSubmitCallbackResponse.builder()
                .state(PROCEEDS_IN_HERITAGE.toString())
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
        } else {
            return emptyCallbackResponse(callbackParams);
        }
    }
}
