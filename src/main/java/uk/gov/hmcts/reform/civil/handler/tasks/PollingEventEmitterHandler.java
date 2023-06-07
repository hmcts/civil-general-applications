package uk.gov.hmcts.reform.civil.handler.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.EventEmitterService;
import uk.gov.hmcts.reform.civil.service.search.CaseStateSearchService;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${polling.event.emitter.enabled:true}")
public class PollingEventEmitterHandler implements BaseExternalTaskHandler {

    private final CaseStateSearchService caseSearchService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final EventEmitterService eventEmitterService;

    @Override
    public void handleTask(ExternalTask externalTask) {
        List<CaseDetails> cases = caseSearchService
            .getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.STARTED);
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), cases.size());
        cases.stream()
            .map(caseDetailsConverter::toCaseData)
            .forEach(this::emitBusinessProcess);
    }

    private void emitBusinessProcess(CaseData caseData) {
        log.info("Emitting {} camunda event for case through poller: {}",
                 caseData.getBusinessProcess().getCamundaEvent(),
                 caseData.getCcdCaseReference());
        eventEmitterService.emitBusinessProcessCamundaGAEvent(caseData, false);
    }

    @Override
    public int getMaxAttempts() {
        return 1;
    }
}
