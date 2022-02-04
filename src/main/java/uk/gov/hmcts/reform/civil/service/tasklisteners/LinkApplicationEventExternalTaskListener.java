package uk.gov.hmcts.reform.civil.service.tasklisteners;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.handler.tasks.LinkApplicationTaskHandler;

@Component
public class LinkApplicationEventExternalTaskListener {

    private static final String TOPIC = "linkApplicationEventGASpec";

    @Autowired
    private LinkApplicationEventExternalTaskListener(LinkApplicationTaskHandler linkApplicationTaskHandler,
                                                  ExternalTaskClient client) {
        TopicSubscriptionBuilder subscriptionBuilder = client.subscribe(TOPIC);
        subscriptionBuilder.handler(linkApplicationTaskHandler).open();
    }
}
