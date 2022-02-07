package uk.gov.hmcts.reform.civil.service.tasklisteners;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.handler.tasks.NotifyGeneralApplicationRespondentTaskHandler;

@Component
public class NotifyGeneralApplicationRespondentTaskListener {

    private static final String TOPIC = "notifyGeneralApplicationRespondentGAspec";

    @Autowired
    private NotifyGeneralApplicationRespondentTaskListener(
        NotifyGeneralApplicationRespondentTaskHandler notifyGeneralApplicationRespondentTaskHandler,
        ExternalTaskClient client) {
        TopicSubscriptionBuilder subscriptionBuilder = client.subscribe(TOPIC);
        subscriptionBuilder.handler(notifyGeneralApplicationRespondentTaskHandler).open();
    }
}
