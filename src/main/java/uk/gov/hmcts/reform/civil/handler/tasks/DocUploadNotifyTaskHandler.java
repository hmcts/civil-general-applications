package uk.gov.hmcts.reform.civil.handler.tasks;

import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DocUploadNotifyTaskHandler implements BaseExternalTaskHandler {
    @Override
    public void handleTask(ExternalTask externalTask) {
        List<CaseData> cases = null;
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), 0);
    }
}
