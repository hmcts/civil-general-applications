package uk.gov.hmcts.reform.civil.service.tasklisteners;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.handler.tasks.StartGeneralAppBusinessProcessTaskHandler;

@Component
public class StartGeneralAppBusinessProcessExternalTaskListener {

    private static final String TOPIC = "START_GENERAL_APP_BUSINESS_PROCESS";

    @Autowired
    private StartGeneralAppBusinessProcessExternalTaskListener(
        StartGeneralAppBusinessProcessTaskHandler startBusinessProcessTaskHandler,
        ExternalTaskClient client) {

        TopicSubscriptionBuilder subscriptionBuilder = client.subscribe(TOPIC);
        subscriptionBuilder.handler(startBusinessProcessTaskHandler).open();
    }
}
