package uk.gov.hmcts.reform.civil.handler.callback.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.event.CloseApplicationsEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.VERIFY_AND_CLOSE_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_CLOSED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_DISMISSED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.LISTING_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PROCEEDS_IN_HERITAGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyAndCloseApplicationEventCallbackHandler extends CallbackHandler {

    private final ApplicationEventPublisher applicationEventPublisher;

    private static final List<CaseEvent> EVENTS = singletonList(VERIFY_AND_CLOSE_APPLICATION);

    private static  final List<CaseState> NON_LIVE_STATES =
        List.of(APPLICATION_CLOSED, PROCEEDS_IN_HERITAGE, ORDER_MADE, LISTING_FOR_A_HEARING, APPLICATION_DISMISSED);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START), this::emptyCallbackResponse,
            callbackKey(ABOUT_TO_SUBMIT), this::emptyCallbackResponse,
            callbackKey(SUBMITTED), this::changeApplicationState
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse changeApplicationState(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        if (!NON_LIVE_STATES.contains(caseData.getCcdState())) {
            Long caseId = caseData.getCcdCaseReference();
            applicationEventPublisher.publishEvent(new CloseApplicationsEvent(caseId));
        }
        return SubmittedCallbackResponse.builder().build();
    }
}
