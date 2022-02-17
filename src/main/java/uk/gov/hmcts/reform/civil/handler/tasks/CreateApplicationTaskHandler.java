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
import uk.gov.hmcts.reform.civil.model.CaseLink;
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

    private static final String GENERAL_APPLICATION_CASE_ID = "generalApplicationCaseId";
    private static final String GENERAL_APPLICATIONS = "generalApplications";
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
        if (generalApplications != null && !generalApplications.isEmpty()) {
            var genApps = generalApplications.stream()
                .filter(application -> application.getValue() != null
                    && application.getValue().getBusinessProcess() != null
                    && application.getValue().getBusinessProcess().getStatus() == BusinessProcessStatus.STARTED
                    && application.getValue().getBusinessProcess().getProcessInstanceId() != null).findFirst();
            if (genApps.isPresent()) {
                GeneralApplication generalApplication = genApps.get().getValue();
                createGeneralApplicationCase(generalApplication);
                updateParentCaseGeneralApplication(variables, generalApplication);
            }
        }
        data = coreCaseDataService.submitUpdate(caseId, coreCaseDataService.caseDataContentFromStartEventResponse(
            startEventResponse, getUpdatedCaseData(caseData, generalApplications)));
    }

    private void updateParentCaseGeneralApplication(ExternalTaskInput variables,
                                                    GeneralApplication generalApplication) {
        generalApplication.getBusinessProcess().setStatus(BusinessProcessStatus.FINISHED);
        generalApplication.getBusinessProcess().setCamundaEvent(variables.getCaseEvent().name());
        if (generalAppCaseData != null && generalAppCaseData.getCcdCaseReference() != null) {
            generalApplication.addCaseLink(CaseLink.builder()
                              .caseReference(String.valueOf(generalAppCaseData.getCcdCaseReference())).build());
        }
    }

    private void createGeneralApplicationCase(GeneralApplication generalApplication) {
        Map<String, Object> map = generalApplication.toMap(mapper);
        map.put("generalAppDeadlineNotificationDate",
                generalApplication
                    .getGeneralAppDeadlineNotification());
        generalAppCaseData = coreCaseDataService.createGeneralAppCase(map);
    }

    @Override
    public VariableMap getVariableMap() {
        VariableMap variables = Variables.createVariables();
        var stateFlow = stateFlowEngine.evaluate(data);
        variables.putValue(FLOW_STATE, stateFlow.getState().getName());
        variables.putValue(FLOW_FLAGS, stateFlow.getFlags());
        if (generalAppCaseData != null && generalAppCaseData.getCcdCaseReference() != null) {
            variables.putValue(GENERAL_APPLICATION_CASE_ID, generalAppCaseData.getCcdCaseReference());
        }
        return variables;
    }

    private Map<String, Object> getUpdatedCaseData(CaseData caseData,
                                                   List<Element<GeneralApplication>> generalApplications) {
        Map<String, Object> output = caseData.toMap(mapper);
        output.put(GENERAL_APPLICATIONS, generalApplications);
        return output;
    }
}
