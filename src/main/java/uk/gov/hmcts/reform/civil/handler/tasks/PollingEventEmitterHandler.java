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
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.EventEmitterService;
import uk.gov.hmcts.reform.civil.service.search.CaseStateSearchService;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_BUSINESS_PROCESS_STATE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_PROCESS_STATE_FOR_CIVIL_GA;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${polling.event.emitter.enabled:true}")
public class PollingEventEmitterHandler implements BaseExternalTaskHandler {

    private final CaseStateSearchService caseSearchService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final EventEmitterService eventEmitterService;
    private final CoreCaseDataService coreCaseDataService;

    @Override
    public void handleTask(ExternalTask externalTask) {
        List<CaseDetails> cases = caseSearchService
            .getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.FAILED);
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


//        if( caseData.getBusinessProcess().getCamundaEvent().equals("INITIATE_GENERAL_APPLICATION") ) {
//            startCivilEventToUpdateState(caseData);
//            eventEmitterService.emitBusinessProcessCamundaEvent(caseData.getCcdCaseReference(),caseData, true);
//        } else {
            startGAEventToUpdateState(caseData);
            eventEmitterService.emitBusinessProcessCamundaGAEvent(caseData, true);
//        }



    }

    @Override
    public int getMaxAttempts() {
        return 1;
    }

    private void startGAEventToUpdateState(CaseData caseData){

        String caseId = String.valueOf(caseData.getCcdCaseReference());
        StartEventResponse startEventResponse = coreCaseDataService
            .startGaUpdate(caseId, UPDATE_BUSINESS_PROCESS_STATE);

        CaseData startEventData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());
        BusinessProcess businessProcess = startEventData.getBusinessProcess().toBuilder()
            .status(BusinessProcessStatus.STARTED).build();

        CaseDataContent caseDataContent = gaCaseDataContent(startEventResponse, businessProcess);
        coreCaseDataService.submitGaUpdate(caseId, caseDataContent);
    }

    private void startCivilEventToUpdateState(CaseData caseData){

        String caseId = String.valueOf(caseData.getCcdCaseReference());
        StartEventResponse startEventResponse = coreCaseDataService
            .startGaUpdate(caseId, UPDATE_BUSINESS_PROCESS_STATE);

        CaseData startEventData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());
        BusinessProcess businessProcess = startEventData.getBusinessProcess().toBuilder()
            .status(BusinessProcessStatus.STARTED).build();

        CaseDataContent caseDataContent = gaCaseDataContent(startEventResponse, businessProcess);
        coreCaseDataService.submitGaUpdate(caseId, caseDataContent);
    }

    private CaseDataContent gaCaseDataContent(StartEventResponse startGaEventResponse,
                                              BusinessProcess businessProcess) {
        Map<String, Object> objectDataMap = startGaEventResponse.getCaseDetails().getData();
        objectDataMap.put("businessProcess", businessProcess);
        log.info("businessProcess {}", businessProcess);

        return CaseDataContent.builder()
            .eventToken(startGaEventResponse.getToken())
            .event(Event.builder().id(startGaEventResponse.getEventId())
                       .build())
            .data(objectDataMap)
            .build();

    }
}
