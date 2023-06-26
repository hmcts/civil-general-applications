package uk.gov.hmcts.reform.civil.handler.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.TaskHandlerHelper;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.data.ExternalTaskInput;

import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_HEARING_SCHEDULED_PROCESS_GASPEC;

@Slf4j
@Component
@RequiredArgsConstructor
public class EndHearingScheduledBusinessProcessTaskHandler implements BaseExternalTaskHandler {

    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final TaskHandlerHelper taskHandlHelper;
    private final ObjectMapper mapper;

    @Override
    public void handleTask(ExternalTask externalTask) {
        ExternalTaskInput externalTaskInput = mapper.convertValue(externalTask.getAllVariables(),
                                                                  ExternalTaskInput.class);
        String caseId = externalTaskInput.getCaseId();
        StartEventResponse startEventResponse = coreCaseDataService
            .startGaUpdate(caseId, END_HEARING_SCHEDULED_PROCESS_GASPEC);
        CaseData data = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());
        BusinessProcess businessProcess = data.getBusinessProcess();
        coreCaseDataService.submitGaUpdate(caseId, caseDataContent(startEventResponse, businessProcess));
    }

    @Override
    public void handleFailure(ExternalTask externalTask, ExternalTaskService externalTaskServ, Exception e) {
        taskHandlHelper.updateEventToFailedState(externalTask, getMaxAttempts());
        handleFailureToExternalTaskService(externalTask, externalTaskServ, e);
    }

    private CaseDataContent caseDataContent(StartEventResponse startEventResponse, BusinessProcess businessProcess) {
        Map<String, Object> data = startEventResponse.getCaseDetails().getData();
        data.put("businessProcess", businessProcess.reset());

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder().id(startEventResponse.getEventId()).build())
            .data(data)
            .build();
    }
}
