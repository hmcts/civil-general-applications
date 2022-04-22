package uk.gov.hmcts.reform.civil.handler.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.service.EventEmitterService;
import uk.gov.hmcts.reform.civil.service.search.AwaitingResponseStatusSearchService;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${polling.event.emitter.enabled:true}")
public class GAResponseDeadlineTaskHandler implements BaseExternalTaskHandler {

    private final AwaitingResponseStatusSearchService caseSearchService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final EventEmitterService eventEmitterService;

    @Override
    public void handleTask(ExternalTask externalTask) {
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), 0);
        List<CaseDetails> cases = caseSearchService.getAwaitingResponseCasesThatArePastDueDate();
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), cases.size());
        //cases.stream()
        //    .map(caseDetailsConverter::toCaseData)
        //    .map(a -> a.toBuilder().businessProcess(
        //            BusinessProcess.builder().camundaEvent("").processInstanceId("").activityId("").build())
        //            .build())
        //    .forEach(mappedCase -> eventEmitterService.emitBusinessProcessCamundaEvent(mappedCase, true));
    }

    @Override
    public int getMaxAttempts() {
        return 1;
    }
}
