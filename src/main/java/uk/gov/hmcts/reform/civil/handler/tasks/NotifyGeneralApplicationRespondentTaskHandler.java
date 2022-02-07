package uk.gov.hmcts.reform.civil.handler.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.data.ExternalTaskInput;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;

import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class NotifyGeneralApplicationRespondentTaskHandler implements BaseExternalTaskHandler {

    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final StateFlowEngine stateFlowEngine;
    private final ObjectMapper mapper;

    private CaseData data;

    @Override
    public void handleTask(ExternalTask externalTask) {
        ExternalTaskInput variables = mapper
            .convertValue(
                externalTask.getAllVariables(),
                ExternalTaskInput.class
            );
        String genAppId = variables.getGeneralApplicationCaseId();
        StartEventResponse startEventResponse = coreCaseDataService.startGaUpdate(genAppId, variables.getCaseEvent());
        data = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());
        Map<String, Object> dataMap = Collections.unmodifiableMap(startEventResponse.getCaseDetails().getData());
        var caseDataContent = coreCaseDataService.caseDataContentFromStartEventResponse(startEventResponse, dataMap);
        coreCaseDataService.submitGaUpdate(genAppId, caseDataContent);

    }

    @Override
    public VariableMap getVariableMap() {
        VariableMap variables = Variables.createVariables();
        var stateFlow = stateFlowEngine.evaluate(data);
        variables.putValue(FLOW_STATE, stateFlow.getState().getName());
        variables.putValue(FLOW_FLAGS, stateFlow.getFlags());
        return variables;
    }
}
