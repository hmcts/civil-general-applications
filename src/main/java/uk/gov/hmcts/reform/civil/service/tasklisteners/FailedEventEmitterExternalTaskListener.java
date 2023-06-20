package uk.gov.hmcts.reform.civil.service.tasklisteners;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.handler.tasks.FailedEventEmitterHandler;

@Component
@ConditionalOnExpression("${polling.event.emitter.enabled:true}")
public class FailedEventEmitterExternalTaskListener {

    private static final String TOPIC = "GAFailedEventEmitterScheduler";

    @Autowired
    private FailedEventEmitterExternalTaskListener(FailedEventEmitterHandler failedEventEmitterHandler,
                                                   ExternalTaskClient client) {
        TopicSubscriptionBuilder subscriptionBuilder = client.subscribe(TOPIC);
        subscriptionBuilder.handler(failedEventEmitterHandler).open();
    }
}
