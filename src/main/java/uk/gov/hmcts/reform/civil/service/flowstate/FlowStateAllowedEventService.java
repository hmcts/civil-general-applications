package uk.gov.hmcts.reform.civil.service.flowstate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.stateflow.StateFlow;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Map.entry;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CREATE_GENERAL_APPLICATION_CASE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.LINK_GENERAL_APPLICATION_CASE_TO_PARENT_CASE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_APPLICATION;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.APPLICATION_SUBMITTED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.DRAFT;

@Service
@RequiredArgsConstructor
public class FlowStateAllowedEventService {

    private final StateFlowEngine stateFlowEngine;

    private final CaseDetailsConverter caseDetailsConverter;

    private static final Map<String, List<CaseEvent>> ALLOWED_EVENTS_ON_FLOW_STATE = Map.ofEntries(
        entry(DRAFT.fullName(), List.of(INITIATE_GENERAL_APPLICATION,
                                        RESPOND_TO_APPLICATION)),

        entry(APPLICATION_SUBMITTED.fullName(),
              List.of(CREATE_GENERAL_APPLICATION_CASE,
                  LINK_GENERAL_APPLICATION_CASE_TO_PARENT_CASE)
        )
    );

    public FlowState getFlowState(CaseData caseData) {
        StateFlow stateFlow = stateFlowEngine.evaluate(caseData);
        return FlowState.fromFullName(stateFlow.getState().getName());
    }

    public boolean isAllowed(CaseDetails caseDetails, CaseEvent caseEvent) {
        StateFlow stateFlow = stateFlowEngine.evaluate(caseDetails);
        return isAllowedOnState(stateFlow.getState().getName(), caseEvent);
    }

    public boolean isAllowedOnState(String stateFullName, CaseEvent caseEvent) {
        return ALLOWED_EVENTS_ON_FLOW_STATE
            .getOrDefault(stateFullName, emptyList())
            .contains(caseEvent);
    }
}
