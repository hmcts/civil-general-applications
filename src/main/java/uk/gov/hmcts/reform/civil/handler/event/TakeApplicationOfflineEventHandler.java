package uk.gov.hmcts.reform.civil.handler.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.event.TakeApplicationOfflineEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPLICATION_PROCEEDS_IN_HERITAGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TakeApplicationOfflineEventHandler {

    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;

    @EventListener
    public void triggerApplicationProceedsInHeritageEvent(TakeApplicationOfflineEvent event) {
        CaseData caseData = caseDetailsConverter.toCaseData(coreCaseDataService.getCase(event.getCaseId()));
        Long caseId = caseData.getCcdCaseReference();
        try {
            log.info("Triggering APPLICATION_PROCEEDS_IN_HERITAGE event to take the application offline: [{}]", caseId);
            coreCaseDataService.triggerGaEvent(
                caseData.getCcdCaseReference(),
                APPLICATION_PROCEEDS_IN_HERITAGE,
                Map.of()
            );
        } catch (Exception e) {
            log.error("Could not trigger event to take application offline [{}]", caseId, e);
        }
    }
}
