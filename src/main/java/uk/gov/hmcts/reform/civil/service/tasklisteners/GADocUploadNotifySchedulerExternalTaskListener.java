package uk.gov.hmcts.reform.civil.service.tasklisteners;

import uk.gov.hmcts.reform.civil.handler.tasks.DocUploadNotifyTaskHandler;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GADocUploadNotifySchedulerExternalTaskListener {

    private static final String TOPIC = "GADocUploadNotifyScheduler";

    @Autowired
    private GADocUploadNotifySchedulerExternalTaskListener(DocUploadNotifyTaskHandler taskHandler,
                                                           ExternalTaskClient client) {
        TopicSubscriptionBuilder subscriptionBuilder = client.subscribe(TOPIC);
        subscriptionBuilder.handler(taskHandler).open();
    }
}
