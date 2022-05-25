package uk.gov.hmcts.reform.civil.handler.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.data.ExternalTaskInput;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class MakeJudgesDecisionTaskHandler implements BaseExternalTaskHandler {

    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final ObjectMapper mapper;
    private final StateFlowEngine stateFlowEngine;

    private CaseData data;

    @Override
    public void handleTask(ExternalTask externalTask) {
        ExternalTaskInput variables = mapper.convertValue(externalTask.getAllVariables(), ExternalTaskInput.class);
        String generalApplicationCaseId = variables.getCaseId();
        StartEventResponse startEventResponse = coreCaseDataService.startGaUpdate(generalApplicationCaseId,
            variables.getCaseEvent());
        CaseData startEventData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());
        BusinessProcess businessProcess = startEventData.getBusinessProcess()
            .updateActivityId(externalTask.getActivityId());

        CaseDataContent caseDataContent = caseDataContent(startEventResponse, businessProcess,
            variables, startEventData.getGeneralAppParentCaseLink());
        data = coreCaseDataService.submitGaUpdate(generalApplicationCaseId, caseDataContent);
    }

    @Override
    public VariableMap getVariableMap() {
        VariableMap variables = Variables.createVariables();
        var stateFlow = stateFlowEngine.evaluate(data);
        variables.putValue(FLOW_STATE, stateFlow.getState().getName());
        variables.putValue(FLOW_FLAGS, stateFlow.getFlags());
        return variables;
    }

    private CaseDataContent caseDataContent(StartEventResponse startEventResponse,
                                            BusinessProcess businessProcess, ExternalTaskInput variables,
                                            GeneralAppParentCaseLink generalAppParentCaseLink) {
        Map<String, Object> updatedData = startEventResponse.getCaseDetails().getData();
        updatedData.put("businessProcess", businessProcess);

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder().id(startEventResponse.getEventId())
                .summary(null)
                .description(null)
                .build())
            .data(updatedData)
            .build();
    }
}
