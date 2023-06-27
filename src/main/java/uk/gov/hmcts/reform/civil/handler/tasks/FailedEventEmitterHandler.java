package uk.gov.hmcts.reform.civil.handler.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.controllers.testingsupport.CamundaRestEngineClient;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.service.search.CaseStateSearchService;

import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${failed.event.emitter.enabled:true}")
public class FailedEventEmitterHandler implements BaseExternalTaskHandler {

    private final CaseStateSearchService caseSearchService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final CamundaRestEngineClient camundaRestEngineClient;

    @Override
    public void handleTask(ExternalTask externalTask) {
        List<CaseDetails> cases = caseSearchService
            .getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.FAILED);
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), cases.size());
        List<String> tasksIdList =  cases.stream()
            .map(caseDetailsConverter::toCaseData)
            .map(caseData -> caseData.getBusinessProcess().getFailedExternalTaskId())
            .filter(Objects::nonNull)
            .toList();

        emitFailedBusinessTask(tasksIdList);
    }

    private void emitFailedBusinessTask(List<String> lsFailedTasksId) {
        if (!lsFailedTasksId.isEmpty()) {
            log.info("Re triggering failed Events  {} ", lsFailedTasksId);
            camundaRestEngineClient.reTriggerFailedTask(lsFailedTasksId);
        }
    }

    @Override
    public int getMaxAttempts() {
        return 1;
    }
}
