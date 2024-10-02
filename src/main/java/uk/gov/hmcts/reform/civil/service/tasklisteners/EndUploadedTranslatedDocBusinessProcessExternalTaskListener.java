package uk.gov.hmcts.reform.civil.service.tasklisteners;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.handler.tasks.EndGaUploadedTranslatedDocProcessTaskHandler;

@Component
public class EndUploadedTranslatedDocBusinessProcessExternalTaskListener {

    private static final String TOPIC = "END_UPLOADED_TRANSLATED_DOC_BUSINESS_PROCESS_GASPEC";

    @Autowired
    private EndUploadedTranslatedDocBusinessProcessExternalTaskListener(
        EndGaUploadedTranslatedDocProcessTaskHandler handler,
        ExternalTaskClient client) {
        TopicSubscriptionBuilder subscriptionBuilder = client.subscribe(TOPIC);
        subscriptionBuilder.handler(handler).open();
    }
}
