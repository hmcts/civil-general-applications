package uk.gov.hmcts.reform.civil.service.tasklisteners;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.handler.tasks.GAResponseDeadlineTaskHandler;

@Component
@ConditionalOnExpression("${polling.event.emitter.enabled:true}")
public class GAResponseDeadlineProcessorExternalTaskListener {

    private static final String TOPIC = "GA_RESPONSE_DEADLINE_PROCESSOR";

    @Autowired
    private GAResponseDeadlineProcessorExternalTaskListener(GAResponseDeadlineTaskHandler taskHandler,
                                                            ExternalTaskClient client) {
        TopicSubscriptionBuilder subscriptionBuilder = client.subscribe(TOPIC);
        subscriptionBuilder.handler(taskHandler).open();
    }
}
