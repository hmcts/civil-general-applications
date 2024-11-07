package uk.gov.hmcts.reform.civil.controllers.testingsupport;

import feign.FeignException;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.LINK_GENERAL_APPLICATION_CASE_TO_PARENT_CASE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_CASE_DATA;

@Tag(name = "UpdateCaseDataController")
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnExpression("${testing.support.enabled:false}")
public class UpdateCaseDataController {

    private final CoreCaseDataService coreCaseDataService;

    @PutMapping("/testing-support/case/{caseId}")
    public void updateCaseData(@PathVariable("caseId") Long caseId, @RequestBody Map<String, Object> caseDataMap) {
        log.info("Update case data for caseId: {}", caseId);
        try {
            var startEventResponse = coreCaseDataService.startUpdate(caseId.toString(), UPDATE_CASE_DATA);
            coreCaseDataService.submitUpdate(caseId.toString(), caseDataContent(startEventResponse, caseDataMap));
        } catch (FeignException e) {
            log.error(String.format("Updating case data failed: %s", e.contentUTF8()));
            throw e;
        }
    }

    @PutMapping("/testing-support/app/case/{caseId}")
    public void updateGaCaseData(@PathVariable("caseId") Long caseId, @RequestBody Map<String, Object> caseDataMap) {
        log.info("Update GA case data for caseId: {}", caseId);
        try {
            var startEventResponse = coreCaseDataService.startGaUpdate(caseId.toString(),
                                                                       LINK_GENERAL_APPLICATION_CASE_TO_PARENT_CASE);
            coreCaseDataService.submitGaUpdate(caseId.toString(), caseDataContent(startEventResponse, caseDataMap));
        } catch (FeignException e) {
            log.error(String.format("Updating app case data failed: %s", e.contentUTF8()));
            throw e;
        }
    }

    @PostMapping("/testing-support/case/{caseId}/trigger/{eventName}")
    public void triggerGAEvent(@PathVariable("caseId") Long caseId, @PathVariable("eventName") CaseEvent eventName) {
        log.info("Trigger GA event for caseId: {}", caseId);
        try {
            coreCaseDataService.triggerGaEvent(caseId, eventName, Map.of());
        } catch (FeignException e) {
            log.error(String.format("Triggering event: %s on case %s failed due to: %n %s",
                                    eventName, caseId, e.contentUTF8()));
            throw e;
        }
    }

    private CaseDataContent caseDataContent(StartEventResponse startEventResponse, Map<String, Object> caseDataMap) {
        Map<String, Object> data = startEventResponse.getCaseDetails().getData();
        data.putAll(caseDataMap);

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder().id(startEventResponse.getEventId()).build())
            .data(data)
            .build();
    }
}
