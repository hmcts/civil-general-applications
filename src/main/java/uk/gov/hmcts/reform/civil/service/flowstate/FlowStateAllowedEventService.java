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
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPLICATION_PROCEEDS_IN_HERITAGE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPROVE_CONSENT_ORDER;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CREATE_GENERAL_APPLICATION_CASE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GENERATE_DIRECTIONS_ORDER;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.HEARING_SCHEDULED_GA;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.LINK_GENERAL_APPLICATION_CASE_TO_PARENT_CASE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAIN_CASE_CLOSED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAKE_DECISION;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.REFER_TO_JUDGE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.REFER_TO_LEGAL_ADVISOR;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_APPLICATION;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_JUDGE_ADDITIONAL_INFO;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_JUDGE_DIRECTIONS;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_JUDGE_WRITTEN_REPRESENTATION;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.APPLICATION_SUBMITTED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.DRAFT;

@Service
@RequiredArgsConstructor
public class FlowStateAllowedEventService {

    private final StateFlowEngine stateFlowEngine;

    private final CaseDetailsConverter caseDetailsConverter;

    private static final Map<String, List<CaseEvent>> ALLOWED_EVENTS_ON_FLOW_STATE = Map.ofEntries(
        entry(DRAFT.fullName(), List.of(INITIATE_GENERAL_APPLICATION,
                                        RESPOND_TO_APPLICATION,
                                        REFER_TO_JUDGE,
                                        REFER_TO_LEGAL_ADVISOR,
                                        APPROVE_CONSENT_ORDER,
                                        MAKE_DECISION,
                                        HEARING_SCHEDULED_GA,
                                        GENERATE_DIRECTIONS_ORDER,
                                        RESPOND_TO_JUDGE_WRITTEN_REPRESENTATION,
                                        RESPOND_TO_JUDGE_DIRECTIONS,
                                        RESPOND_TO_JUDGE_ADDITIONAL_INFO,
                                        APPLICATION_PROCEEDS_IN_HERITAGE,
                                        MAIN_CASE_CLOSED)),

        entry(APPLICATION_SUBMITTED.fullName(),
              List.of(CREATE_GENERAL_APPLICATION_CASE,
                  LINK_GENERAL_APPLICATION_CASE_TO_PARENT_CASE,
                      APPLICATION_PROCEEDS_IN_HERITAGE,
                      MAIN_CASE_CLOSED)
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
