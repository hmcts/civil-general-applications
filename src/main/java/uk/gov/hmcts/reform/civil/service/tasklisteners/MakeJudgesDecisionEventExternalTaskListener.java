package uk.gov.hmcts.reform.civil.service.tasklisteners;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.handler.tasks.MakeJudgesDecisionTaskHandler;

@Component
public class MakeJudgesDecisionEventExternalTaskListener {

    private static final String TOPIC = "makeDecisionEventGASpec";

    @Autowired
    private MakeJudgesDecisionEventExternalTaskListener(MakeJudgesDecisionTaskHandler makeJudgesDecisionTaskHandler,
                                                        ExternalTaskClient client) {
        TopicSubscriptionBuilder subscriptionBuilder = client.subscribe(TOPIC);
        subscriptionBuilder.handler(makeJudgesDecisionTaskHandler).open();
    }
}
