package uk.gov.hmcts.reform.civil.utils;

import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import org.camunda.bpm.client.task.ExternalTask;

import java.util.Map;

public class TaskHandlerUtil {

    private TaskHandlerUtil() {
        // NO-OP
    }

    public static CaseDataContent gaCaseDataContent(StartEventResponse startGaEventResponse,
                                              BusinessProcess businessProcess) {
        Map<String, Object> objectDataMap = startGaEventResponse.getCaseDetails().getData();
        objectDataMap.put("businessProcess", businessProcess);

        return CaseDataContent.builder()
            .eventToken(startGaEventResponse.getToken())
            .event(Event.builder().id(startGaEventResponse.getEventId())
                       .build())
            .data(objectDataMap)
            .build();
    }

    public static int getMaximumAttemptLeft(ExternalTask externalTask, int maxAttempts) {
        return externalTask.getRetries() == null ? maxAttempts : externalTask.getRetries();
    }
}
