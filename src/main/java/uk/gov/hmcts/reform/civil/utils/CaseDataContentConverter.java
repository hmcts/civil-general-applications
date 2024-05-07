package uk.gov.hmcts.reform.civil.utils;

import java.util.HashMap;
import java.util.Map;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;

public class CaseDataContentConverter {

    private CaseDataContentConverter() {
    }

    public static CaseDataContent caseDataContentFromStartEventResponse(StartEventResponse startEventResponse, Map<String, Object> contentModified) {
        HashMap<String, Object> payload = new HashMap<>(startEventResponse.getCaseDetails().getData());
        payload.putAll(contentModified);
        return CaseDataContent.builder().eventToken(startEventResponse.getToken()).event(Event.builder().id(startEventResponse.getEventId()).build()).data(payload).build();
    }
}
