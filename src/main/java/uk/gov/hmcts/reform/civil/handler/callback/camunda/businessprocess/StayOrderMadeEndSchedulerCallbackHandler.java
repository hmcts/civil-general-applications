package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

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
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_SCHEDULER_DEADLINE_STAY_ORDER;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_GA_CASE_DATA;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;

@Slf4j
@Service
@RequiredArgsConstructor
public class StayOrderMadeEndSchedulerCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = singletonList(END_SCHEDULER_DEADLINE_STAY_ORDER);
    private final CoreCaseDataService coreCaseDataService;
    private final ObjectMapper mapper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(SUBMITTED), this::validateApplicationState
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse validateApplicationState(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        Long caseId = caseData.getCcdCaseReference();
        caseData = updateCaseData(caseData);
        coreCaseDataService.triggerGaEvent(caseId, UPDATE_GA_CASE_DATA, getUpdatedCaseDataMapper(caseData));
        log.info("Checking state for caseId: {}", caseId);
        return AboutToStartOrSubmitCallbackResponse.builder()
                .state(ORDER_MADE.toString())
            .build();
    }

    private CaseData updateCaseData(CaseData caseData) {
        GAJudicialMakeAnOrder judicialDecisionMakeOrder = caseData.getJudicialDecisionMakeOrder();
        caseData = caseData.toBuilder()
            .judicialDecisionMakeOrder(
                judicialDecisionMakeOrder.toBuilder().esOrderProcessedByStayScheduler(YesOrNo.YES).build())
            .build();
        return caseData;
    }

    private Map<String, Object> getUpdatedCaseDataMapper(CaseData caseData) {
        Map<String, Object> output = caseData.toMap(mapper);
        return output;
    }
}
