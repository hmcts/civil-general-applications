package uk.gov.hmcts.reform.civil.handler.tasks;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.event.DeleteExpiredResponseRespondentNotificationsEvent;
import uk.gov.hmcts.reform.civil.service.search.DeleteExpiredResponseRespondentNotificationSearchService;

@Slf4j
@RequiredArgsConstructor
@Component
public class DeleteExpiredResponseRespondentNotificationsHandler implements BaseExternalTaskHandler {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final DeleteExpiredResponseRespondentNotificationSearchService caseSearchService;

    @Override
    public void handleTask(ExternalTask externalTask) {
        List<CaseDetails> cases = caseSearchService.getApplications();
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), cases.size());

        cases.forEach(caseDetails -> {
            applicationEventPublisher.publishEvent(new DeleteExpiredResponseRespondentNotificationsEvent(caseDetails.getId()));
        });
    }
}
