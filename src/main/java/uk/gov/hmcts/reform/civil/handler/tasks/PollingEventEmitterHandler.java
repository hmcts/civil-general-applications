package uk.gov.hmcts.reform.civil.handler.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.TaskHandlerHelper;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.EventEmitterService;
import uk.gov.hmcts.reform.civil.service.search.CaseStateSearchService;
import uk.gov.hmcts.reform.civil.utils.ElementUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_BUSINESS_PROCESS_STATE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_CASE_WITH_GA_STATE;
import static uk.gov.hmcts.reform.civil.utils.TaskHandlerUtil.gaCaseDataContent;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${polling.event.emitter.enabled:true}")
public class PollingEventEmitterHandler implements BaseExternalTaskHandler {

    private final CaseStateSearchService caseSearchService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final EventEmitterService eventEmitterService;
    private final CoreCaseDataService coreCaseDataService;
    private final TaskHandlerHelper taskHandlerHelper;
    private final ObjectMapper mapper;

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
        String parentCaseId = caseData.getGeneralAppParentCaseLink().getCaseReference();
        StartEventResponse startEventResponse = coreCaseDataService
                .startUpdate(parentCaseId, UPDATE_CASE_WITH_GA_STATE);

        CaseData civilCaseData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());
        if( caseData.getBusinessProcess().getCamundaEvent().equals("INITIATE_GENERAL_APPLICATION") ) {
            Element<GeneralApplication> applicationElement = null;
            int idx = -1;
            for (Element<GeneralApplication> element : civilCaseData.getGeneralApplications()) {
                idx++;
                if (findGeneralApplicationElement(element,
                        caseData.getCcdCaseReference().toString())) {
                    applicationElement = element;
                    applicationElement = ElementUtils.element(applicationElement.getValue().toBuilder()
                            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build()).build());
                }
            }
            Map<String, Object> output = civilCaseData.toMap(mapper);
            civilCaseData.getGeneralApplications().remove(idx);
            civilCaseData.getGeneralApplications().add(applicationElement);
            output.put("generalApplications", civilCaseData.getGeneralApplications());
            CaseDataContent caseDataContent = coreCaseDataService.caseDataContentFromStartEventResponse(
                    startEventResponse, output);

            civilCaseData = coreCaseDataService.submitUpdate(parentCaseId, caseDataContent);
            eventEmitterService.emitBusinessProcessCamundaEvent(
                    Long.parseLong(parentCaseId), applicationElement.getValue(), true);
        } else {
            startGAEventToUpdateState(caseData);
            eventEmitterService.emitBusinessProcessCamundaGAEvent(caseData, true);
        }
    }

    private boolean findGeneralApplicationElement(Element<GeneralApplication> applicationElement,
                                                  String caseId) {
        if (Objects.nonNull(applicationElement.getValue().getCaseLink())
            && applicationElement.getValue().getCaseLink().getCaseReference().equals(caseId)) {
            return true;
        } else if (Objects.isNull(applicationElement.getValue().getCaseLink())
                && Objects.nonNull(applicationElement.getValue().getBusinessProcess())
            && applicationElement.getValue().getBusinessProcess()
                .getStatus().equals(BusinessProcessStatus.FINISHED)) {
            return true;
        }
        return false;
    }

    @Override
    public int getMaxAttempts() {
        return 1;
    }

    private void startGAEventToUpdateState(CaseData caseData) {

        String caseId = String.valueOf(caseData.getCcdCaseReference());
        StartEventResponse startEventResponse = coreCaseDataService
            .startGaUpdate(caseId, UPDATE_BUSINESS_PROCESS_STATE);

        CaseData startEventData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());
        BusinessProcess businessProcess = startEventData.getBusinessProcess().toBuilder()
            .status(BusinessProcessStatus.STARTED).build();

        CaseDataContent caseDataContent = taskHandlerHelper.gaCaseDataContent(startEventResponse, businessProcess);
        coreCaseDataService.submitGaUpdate(caseId, caseDataContent);
    }

    private void startCivilEventToUpdateState(CaseData caseData) {

        String caseId = String.valueOf(caseData.getCcdCaseReference());
        StartEventResponse startEventResponse = coreCaseDataService
            .startGaUpdate(caseId, UPDATE_BUSINESS_PROCESS_STATE);

        CaseData startEventData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());
        BusinessProcess businessProcess = startEventData.getBusinessProcess().toBuilder()
            .status(BusinessProcessStatus.STARTED).build();

        CaseDataContent caseDataContent = taskHandlerHelper.gaCaseDataContent(startEventResponse, businessProcess);
        coreCaseDataService.submitGaUpdate(caseId, caseDataContent);
    }

}
