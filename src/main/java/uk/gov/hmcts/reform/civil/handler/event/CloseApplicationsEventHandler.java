package uk.gov.hmcts.reform.civil.handler.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.event.CloseApplicationsEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAIN_CASE_CLOSED;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloseApplicationsEventHandler {

    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;

    @EventListener
    public void triggerApplicationClosedEvent(CloseApplicationsEvent event) {
        CaseData caseData = caseDetailsConverter.toCaseData(coreCaseDataService.getCase(event.getCaseId()));
        Long caseId = caseData.getCcdCaseReference();
        try {
            log.info("Triggering MAIN_CASE_CLOSED event to close the application: [{}]", caseId);
            coreCaseDataService.triggerGaEvent(caseData.getCcdCaseReference(), MAIN_CASE_CLOSED, Map.of());
        } catch (Exception e) {
            log.error("Could not trigger event to close application [{}]", caseId, e);
        }
    }
}
