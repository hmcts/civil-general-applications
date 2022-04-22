package uk.gov.hmcts.reform.civil.handler.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.search.AwaitingResponseStatusSearchService;

import java.util.List;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${polling.event.emitter.enabled:true}")
public class GAResponseDeadlineTaskHandler implements BaseExternalTaskHandler {

    private final AwaitingResponseStatusSearchService caseSearchService;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final CoreCaseDataService coreCaseDataService;


    @Override
    public void handleTask(ExternalTask externalTask) {
        List<CaseDetails> cases = caseSearchService.getAwaitingResponseCasesThatArePastDueDate();
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), cases.size());

        cases.forEach(this::fireEventForStateChange);
    }

    private void fireEventForStateChange(CaseDetails caseDetails) {
        coreCaseDataService.triggerEvent(caseDetails.getId(), CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION);
/*        try {
            applicationEventPublisher.publishEvent(
                new MoveAwaitingResponseToJudicialDecisionStateEvent(caseDetails.getId()));
        } catch (Exception e) {
            log.error("Updating case with id: '{}' failed", caseDetails.getId(), e);
        }*/
    }

    @Override
    public int getMaxAttempts() {
        return 1;
    }
}
