package uk.gov.hmcts.reform.civil.handler.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.data.ExternalTaskInput;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class CreateApplicationTaskHandler implements BaseExternalTaskHandler {

    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final ObjectMapper mapper;
    private final StateFlowEngine stateFlowEngine;

    private CaseData data;

    private CaseData generalAppCaseData;

    @Override
    public void handleTask(ExternalTask externalTask) {
        ExternalTaskInput variables = mapper.convertValue(externalTask.getAllVariables(), ExternalTaskInput.class);
        String caseId = variables.getCaseId();
        StartEventResponse startEventResponse = coreCaseDataService.startUpdate(caseId, variables.getCaseEvent());
        CaseData caseData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());

        List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
        if (generalApplications != null) {
            var genApps = generalApplications.stream()
                .filter(app -> app.getValue() != null && app.getValue().getBusinessProcess() != null
                    && app.getValue().getBusinessProcess().getStatus() == BusinessProcessStatus.STARTED
                    && app.getValue().getBusinessProcess().getProcessInstanceId() != null).findFirst();
            if (genApps.isPresent()) {
                GeneralApplication generalApplication = genApps.get().getValue();
                generalApplication.getBusinessProcess().setCamundaEvent(variables.getCaseEvent().name());
                generalAppCaseData = coreCaseDataService.createGeneralAppCase(
                    generalApplication.toMap(mapper)
                );
                generalApplication.getBusinessProcess().setStatus(BusinessProcessStatus.FINISHED);
                generalApplication.getBusinessProcess().setCamundaEvent(variables.getCaseEvent().name());
            }
        }
        data = coreCaseDataService.submitUpdate(caseId, coreCaseDataService.caseDataContentFromStartEventResponse(
            startEventResponse, getUpdatedCaseData(caseData, generalApplications)));
    }

    @Override
    public VariableMap getVariableMap() {
        VariableMap variables = Variables.createVariables();
        var stateFlow = stateFlowEngine.evaluate(data);
        variables.putValue(FLOW_STATE, stateFlow.getState().getName());
        variables.putValue(FLOW_FLAGS, stateFlow.getFlags());
        variables.putValue("generalApplicationCaseId", generalAppCaseData.getCcdCaseReference());
        return variables;
    }

    private Map<String, Object> getUpdatedCaseData(CaseData caseData,
                                                   List<Element<GeneralApplication>> generalApplications) {
        Map<String, Object> output = caseData.toMap(mapper);
        output.put("generalApplications", generalApplications);

        return output;
    }
}
