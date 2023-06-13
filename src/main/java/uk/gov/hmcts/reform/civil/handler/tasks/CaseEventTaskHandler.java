package uk.gov.hmcts.reform.civil.handler.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.data.ExternalTaskInput;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;

import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_BUSINESS_PROCESS_STATE;
import static uk.gov.hmcts.reform.civil.utils.TaskHandlerUtil.gaCaseDataContent;
import static uk.gov.hmcts.reform.civil.utils.TaskHandlerUtil.getMaximumAttemptLeft;

@RequiredArgsConstructor
@Component
public class CaseEventTaskHandler implements BaseExternalTaskHandler {

    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final ObjectMapper mapper;
    private final StateFlowEngine stateFlowEngine;

    private CaseData data;

    @Override
    public void handleTask(ExternalTask externalTask) {
        ExternalTaskInput variables = mapper.convertValue(externalTask.getAllVariables(), ExternalTaskInput.class);
        String caseId = variables.getCaseId();
        StartEventResponse startEventResponse = coreCaseDataService.startUpdate(caseId,
                                                                                variables.getCaseEvent());
        CaseData startEventData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());
        BusinessProcess businessProcess = startEventData.getBusinessProcess()
            .updateActivityId(externalTask.getActivityId());

        String flowState = externalTask.getVariable(FLOW_STATE);
        CaseDataContent caseDataContent = caseDataContent(startEventResponse, businessProcess, flowState);
        data = coreCaseDataService.submitUpdate(caseId, caseDataContent);
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
                                            BusinessProcess businessProcess,
                                            String flowState) {
        Map<String, Object> data = startEventResponse.getCaseDetails().getData();
        data.put("businessProcess", businessProcess);

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder().id(startEventResponse.getEventId())
                       .summary(getSummary(startEventResponse.getEventId(), flowState))
                       .description(getDescription(startEventResponse.getEventId(), data))
                       .build())
            .data(data)
            .build();
    }

    @Override
    public void handleFailure(ExternalTask externalTask, ExternalTaskService externalTaskService, Exception e) {

        int remainingRetries = getMaximumAttemptLeft(externalTask, getMaxAttempts());

        if(remainingRetries == 1) {
            ExternalTaskInput variables = mapper.convertValue(externalTask.getAllVariables(), ExternalTaskInput.class);
            String caseId = variables.getCaseId();

            StartEventResponse startEventResp = coreCaseDataService
                .startGaUpdate(caseId, UPDATE_BUSINESS_PROCESS_STATE);

            CaseData startEventData = caseDetailsConverter.toCaseData(startEventResp.getCaseDetails());
            BusinessProcess businessProcess = startEventData.getBusinessProcess().toBuilder()
                .processInstanceId(externalTask.getProcessInstanceId())
                .status(BusinessProcessStatus.FAILED)
                .build();

            CaseDataContent caseDataContent = gaCaseDataContent(startEventResp, businessProcess);
            coreCaseDataService.submitGaUpdate(caseId, caseDataContent);
        }

        handleFailureToExternalTaskService(externalTask, externalTaskService, e);
    }

    private String getSummary(String eventId, String state) {

        return null;
    }

    private String getDescription(String eventId, Map data) {

        return null;
    }
}
