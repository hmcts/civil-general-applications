package uk.gov.hmcts.reform.civil.handler.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.controllers.testingsupport.CamundaRestEngineClient;
import uk.gov.hmcts.reform.civil.controllers.testingsupport.TestingSupportController;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.EventEmitterService;
import uk.gov.hmcts.reform.civil.service.search.CaseStateSearchService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_BUSINESS_PROCESS_STATE;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${polling.event.emitter.enabled:true}")
public class PollingEventEmitterHandler implements BaseExternalTaskHandler {

    private final CaseStateSearchService caseSearchService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final EventEmitterService eventEmitterService;
    private final CamundaRestEngineClient camundaRestEngineClient;

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

        var businessProcess = caseData.getBusinessProcess();

               Optional<String> incidentMessage = camundaRestEngineClient.findIncidentByProcessInstanceId(businessProcess.getProcessInstanceId())
                    .map(camundaRestEngineClient::getIncidentMessage);
               if(incidentMessage.isPresent()){
                   eventEmitterService.emitBusinessProcessCamundaGAEvent(caseData, true);
               }
    }

    @Override
    public int getMaxAttempts() {
        return 1;
    }

}
