package uk.gov.hmcts.reform.civil.service.tasklisteners;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.handler.tasks.ApplicationStatusTaskHandler;

@Component
@ConditionalOnExpression("${polling.event.emitter.enabled:true}")
public class ApplicationStatusExternalTaskListener {

    private static final String TOPIC = "GA_STATUS_CHANGE_POLLING_EVENT_EMITTER";

    @Autowired
    private ApplicationStatusExternalTaskListener(ApplicationStatusTaskHandler applicationStatusTaskHandler,
                                                  ExternalTaskClient client) {
        TopicSubscriptionBuilder subscriptionBuilder = client.subscribe(TOPIC);
        subscriptionBuilder.handler(applicationStatusTaskHandler).open();
    }
}
