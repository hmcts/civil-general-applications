package uk.gov.hmcts.reform.civil.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import org.camunda.bpm.client.task.ExternalTask;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.data.ExternalTaskInput;

import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_BUSINESS_PROCESS_STATE;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskHandlerHelper {

    private final CoreCaseDataService coreCaseDataService;
    private final ObjectMapper mapper;
    private final CaseDetailsConverter caseDetailsConverter;

    public CaseDataContent gaCaseDataContent(StartEventResponse startGaEventResponse,
                                              BusinessProcess businessProcess) {
        Map<String, Object> objectDataMap = startGaEventResponse.getCaseDetails().getData();
        objectDataMap.put("businessProcess", businessProcess);

        return CaseDataContent.builder()
            .eventToken(startGaEventResponse.getToken())
            .event(Event.builder().id(startGaEventResponse.getEventId())
                       .build())
            .data(objectDataMap)
            .build();
    }

    public int getMaximumAttemptLeft(ExternalTask externalTask, int maxAttempts) {
        return externalTask.getRetries() == null ? maxAttempts : externalTask.getRetries();
    }

    public void updateEventToFailedState(ExternalTask externalTask, int maxAttempts) {
        int remainingRetries = getMaximumAttemptLeft(externalTask, maxAttempts);
        log.info("GeneralApplicationTaskHandler : Task id: {} , Remaining Tries: {}", externalTask.getId(), remainingRetries);
        if (remainingRetries == 1) {

            ExternalTaskInput variables = mapper.convertValue(
                externalTask.getAllVariables(),
                ExternalTaskInput.class
            );
            String caseId;
            if (Objects.nonNull(variables.getGeneralApplicationCaseId())) {
                caseId = variables.getGeneralApplicationCaseId();
            } else {
                caseId = variables.getCaseId();
            }

            StartEventResponse startEventResp = coreCaseDataService
                .startGaUpdate(caseId, UPDATE_BUSINESS_PROCESS_STATE);

            CaseData startEventData = caseDetailsConverter.toCaseData(startEventResp.getCaseDetails());
            BusinessProcess businessProcess = startEventData.getBusinessProcess().toBuilder()
                .processInstanceId(externalTask.getProcessInstanceId())
                .failedExternalTaskId(externalTask.getId())
                .status(BusinessProcessStatus.FAILED)
                .build();

            CaseDataContent caseDataContent = gaCaseDataContent(startEventResp, businessProcess);
            coreCaseDataService.submitGaUpdate(caseId, caseDataContent);
        }
    }
}
