package uk.gov.hmcts.reform.civil.service.tasklisteners;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.handler.tasks.EndGeneralAppBusinessProcessTaskHandler;

@Component
public class EndGeneralAppBusinessProcessExternalTaskListener {

    private static final String TOPIC = "END_GENERAL_APP_BUSINESS_PROCESS";

    @Autowired
    private EndGeneralAppBusinessProcessExternalTaskListener(
        EndGeneralAppBusinessProcessTaskHandler handler,
        ExternalTaskClient client) {
        TopicSubscriptionBuilder subscriptionBuilder = client.subscribe(TOPIC);
        subscriptionBuilder.handler(handler).open();
    }
}
