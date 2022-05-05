package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.event.DispatchBusinessProcessEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;

import static java.lang.String.format;

@Slf4j
@RequiredArgsConstructor
@Component
public class EventEmitterService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final RuntimeService runtimeService;

    public void emitBusinessProcessCamundaEvent(Long caseId, GeneralApplication application, boolean dispatchProcess) {
        var businessProcess = application.getBusinessProcess();
        var camundaEvent = businessProcess.getCamundaEvent();
        log.info(format("Emitting %s camunda event for case: %d", camundaEvent, caseId));
        try {
            runtimeService.createMessageCorrelation(camundaEvent)
                .setVariable("caseId", caseId)
                .correlateStartMessage();

            if (dispatchProcess) {
                applicationEventPublisher.publishEvent(new DispatchBusinessProcessEvent(caseId, businessProcess));
            }

            log.info("Camunda event emitted successfully");
        } catch (Exception ex) {
            log.error(format("Emitting %s camunda event failed for case: %d, message: %s",
                             camundaEvent, caseId, ex.getMessage()
            ));
        }
    }

    public void emitBusinessProcessCamundaGAEvent(CaseData caseData, boolean dispatchProcess) {
        var caseId = caseData.getCcdCaseReference();
        var judgeBusinessProcess = caseData.getBusinessProcess();
        var camundaEvent = judgeBusinessProcess.getCamundaEvent();
        log.info(format("Emitting %s camunda event for case: %d", camundaEvent, caseId));
        try {
            runtimeService.createMessageCorrelation(camundaEvent)
                .setVariable("caseId", caseId)
                .correlateStartMessage();

            if (dispatchProcess) {
                applicationEventPublisher.publishEvent(new DispatchBusinessProcessEvent(caseId, judgeBusinessProcess));
            }

            log.info("Camunda event emitted successfully");
        } catch (Exception ex) {
            log.error(format("Emitting %s camunda event failed for case: %d, message: %s",
                             camundaEvent, caseId, ex.getMessage()
            ));
        }
    }
}
