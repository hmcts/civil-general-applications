package uk.gov.hmcts.reform.civil.handler.tasks;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GA_EVIDENCE_UPLOAD_CHECK;

import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.search.EvidenceUploadNotificationSearchService;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DocUploadNotifyTaskHandler implements BaseExternalTaskHandler {

    private final EvidenceUploadNotificationSearchService caseSearchService;
    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final ObjectMapper mapper;

    @Override
    public void handleTask(ExternalTask externalTask) {
        List<CaseData> cases = caseSearchService.getApplications().stream()
                .map(caseDetailsConverter::toCaseData).toList();
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), cases.size());

        cases.forEach(this::fireEventForStateChange);
    }

    private void fireEventForStateChange(CaseData caseData) {
        Long caseId = caseData.getCcdCaseReference();
        log.info("Firing event EVIDENCE_UPLOAD_CHECK to notify applications with newly "
                + "uploaded documents "
                + "for caseId: {}", caseId);

        coreCaseDataService.triggerGaEvent(caseId, GA_EVIDENCE_UPLOAD_CHECK,
                getUpdatedCaseDataMapper(updateCaseData(caseData)));
        log.info("Checking state for caseId: {}", caseId);
    }

    private CaseData updateCaseData(CaseData caseData) {
        caseData = caseData.toBuilder()
                .notificationText(null)
                .build();
        return caseData;
    }

    private Map<String, Object> getUpdatedCaseDataMapper(CaseData caseData) {
        return caseData.toMap(mapper);
    }

    @Override
    public int getMaxAttempts() {
        return 1;
    }
}
