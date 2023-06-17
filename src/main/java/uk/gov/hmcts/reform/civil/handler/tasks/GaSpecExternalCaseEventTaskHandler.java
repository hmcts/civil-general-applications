package uk.gov.hmcts.reform.civil.handler.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.TaskHandlerHelper;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.data.ExternalTaskInput;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;

@RequiredArgsConstructor
@Component
public class GaSpecExternalCaseEventTaskHandler implements BaseExternalTaskHandler  {

    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final ObjectMapper mapper;
    private final TaskHandlerHelper taskHandlerHelper;
    private final StateFlowEngine stateFlowEngine;

    private CaseData data;

    @Override
    public void handleTask(ExternalTask externalTask) {

        ExternalTaskInput variables = mapper.convertValue(externalTask.getAllVariables(), ExternalTaskInput.class);
        String caseId = variables.getCaseId();
        StartEventResponse startEventResponse = coreCaseDataService.startGaUpdate(caseId,
                                                variables.getCaseEvent());
        CaseData startEventData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());
        BusinessProcess businessProcess = startEventData
            .getBusinessProcess().toBuilder()
            .activityId(externalTask.getActivityId())
            .failedExternalTaskId(externalTask.getId()).build();

        businessProcess.setFailedExternalTaskId(externalTask.getId());

        CaseDataContent caseDataContent = taskHandlerHelper.gaCaseDataContent(startEventResponse, businessProcess);
        data = coreCaseDataService.submitGaUpdate(caseId, caseDataContent);
    }

    @Override
    public VariableMap getVariableMap() {
        VariableMap variables = Variables.createVariables();
        var stateFlow = stateFlowEngine.evaluate(data);
        variables.putValue(FLOW_STATE, stateFlow.getState().getName());
        variables.putValue(FLOW_FLAGS, stateFlow.getFlags());
        return variables;
    }

    @Override
    public void handleFailure(ExternalTask externalTask, ExternalTaskService externalTaskService, Exception e) {

        taskHandlerHelper.updateEventToFailedState(externalTask, getMaxAttempts());

        handleFailureToExternalTaskService(externalTask, externalTaskService, e);
    }
}
