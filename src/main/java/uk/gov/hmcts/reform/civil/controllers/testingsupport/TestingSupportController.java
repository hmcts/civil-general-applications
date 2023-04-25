package uk.gov.hmcts.reform.civil.controllers.testingsupport;

import feign.FeignException;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.impl.ExternalTaskImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.civil.handler.tasks.CheckStayOrderDeadlineEndTaskHandler;
import uk.gov.hmcts.reform.civil.handler.tasks.CheckUnlessOrderDeadlineEndTaskHandler;
import uk.gov.hmcts.reform.civil.handler.tasks.GAJudgeRevisitTaskHandler;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.Objects;

import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;

@Tag(name = "TestingSupportController")
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnExpression("${testing.support.enabled:false}")
public class TestingSupportController {

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;
    private final CamundaRestEngineClient camundaRestEngineClient;
    private final FeatureToggleService featureToggleService;
    private final CheckStayOrderDeadlineEndTaskHandler checkStayOrderDeadlineEndTaskHandler;
    private final CheckUnlessOrderDeadlineEndTaskHandler checkUnlessOrderDeadlineEndTaskHandler;
    private final GAJudgeRevisitTaskHandler gaJudgeRevisitTaskHandler;

    @GetMapping("/testing-support/case/{caseId}/business-process")
    public ResponseEntity<BusinessProcessInfo> getBusinessProcess(@PathVariable("caseId") Long caseId) {
        CaseData caseData = caseDetailsConverter.toCaseData(coreCaseDataService.getCase(caseId));
        var businessProcess = caseData.getBusinessProcess();
        var caseState = caseData.getCcdState();
        var businessProcessInfo = new BusinessProcessInfo(businessProcess);

        if (businessProcess.getStatus() == STARTED) {
            try {
                camundaRestEngineClient.findIncidentByProcessInstanceId(businessProcess.getProcessInstanceId())
                    .map(camundaRestEngineClient::getIncidentMessage)
                    .ifPresent(businessProcessInfo::setIncidentMessage);
            } catch (FeignException e) {
                if (e.status() != 404) {
                    businessProcessInfo.setIncidentMessage(e.contentUTF8());
                }
            }
        }

        businessProcessInfo.setCcdState(caseState.toString());

        return new ResponseEntity<>(businessProcessInfo, HttpStatus.OK);
    }

    /*Check if Camunda Event CREATE_GENERAL_APPLICATION_CASE is Finished
    if so, generalApplicationsDetails object will be populated with GA case reference*/
    @GetMapping("/testing-support/case/{caseId}/business-process/ga")
    public ResponseEntity<BusinessProcessInfo> getGACaseReference(@PathVariable("caseId") Long caseId) {
        CaseData caseData = caseDetailsConverter.toCaseData(coreCaseDataService.getCase(caseId));

        int size = caseData.getGeneralApplications().size();

        /**
         * Check the business process status of latest GA case
         * if caseData.getGeneralApplications() collection size is more than 1
         */

        var generalApplication = caseData.getGeneralApplications().get(size - 1);

        var businessProcess = Objects.requireNonNull(generalApplication).getValue().getBusinessProcess();
        var businessProcessInfo = new BusinessProcessInfo(businessProcess);

        log.info("GA Business process status: " + businessProcess.getStatus() + " Camunda Event: " + businessProcess
            .getCamundaEvent());

        if (businessProcess.getStatus() == STARTED) {
            try {
                camundaRestEngineClient.findIncidentByProcessInstanceId(businessProcess.getProcessInstanceId())
                    .map(camundaRestEngineClient::getIncidentMessage)
                    .ifPresent(businessProcessInfo::setIncidentMessage);
            } catch (FeignException e) {
                if (e.status() != 404) {
                    businessProcessInfo.setIncidentMessage(e.contentUTF8());
                }
            }
        }

        return new ResponseEntity<>(businessProcessInfo, HttpStatus.OK);
    }

    @GetMapping("/testing-support/case/{caseId}")
    public ResponseEntity<CaseData> getCaseData(@PathVariable("caseId") Long caseId) {

        CaseData caseData = caseDetailsConverter.toCaseData(coreCaseDataService.getCase(caseId));
        return new ResponseEntity<>(caseData, HttpStatus.OK);
    }

    @Data
    private static class FeatureToggleInfo {
        private boolean isToggleEnabled;

        private FeatureToggleInfo(boolean isToggleEnabled) {
            this.isToggleEnabled = isToggleEnabled;
        }
    }

    @Data
    private static class BusinessProcessInfo {
        private BusinessProcess businessProcess;
        private String incidentMessage;
        private String ccdState;

        private BusinessProcessInfo(BusinessProcess businessProcess) {
            this.businessProcess = businessProcess;
        }
    }

    @GetMapping("/testing-support/trigger-judge-revisit-process-event/{state}/{genAppType}")
    public ResponseEntity<String> getJudgeRevisitProcessEvent(@PathVariable("state") String ccdState,
                                                              @PathVariable("genAppType") String genAppType) {

        String responseMsg = "success";
        ExternalTaskImpl externalTask = new ExternalTaskImpl();
        try {
            if (ccdState.equals("ORDER_MADE")) {
                if (genAppType.equals("STAY_THE_CLAIM")) {
                    checkStayOrderDeadlineEndTaskHandler.handleTask(externalTask);
                } else {
                    checkUnlessOrderDeadlineEndTaskHandler.handleTask(externalTask);
                }
            } else {
                gaJudgeRevisitTaskHandler.handleTask(externalTask);
            }
        } catch (Exception e) {
            responseMsg = "failed";
        }

        return new ResponseEntity<>(responseMsg, HttpStatus.OK);
    }

}
