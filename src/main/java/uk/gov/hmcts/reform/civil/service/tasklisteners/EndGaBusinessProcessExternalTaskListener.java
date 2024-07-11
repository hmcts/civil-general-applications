package uk.gov.hmcts.reform.civil.service.tasklisteners;

import uk.gov.hmcts.reform.civil.handler.tasks.EndJudgeMakesDecisionBusinessProcessTaskHandler;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EndGaBusinessProcessExternalTaskListener {

    private static final String TOPIC = "END_GA_BUSINESS_PROCESS";

    @Autowired
    private EndGaBusinessProcessExternalTaskListener(
        EndJudgeMakesDecisionBusinessProcessTaskHandler handler,
        ExternalTaskClient client) {
        TopicSubscriptionBuilder subscriptionBuilder = client.subscribe(TOPIC);
        subscriptionBuilder.handler(handler).open();
    }
}
