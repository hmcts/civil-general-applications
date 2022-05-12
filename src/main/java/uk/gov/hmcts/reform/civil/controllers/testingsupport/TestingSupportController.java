package uk.gov.hmcts.reform.civil.controllers.testingsupport;

import feign.FeignException;
import io.swagger.annotations.Api;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.Objects;

import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;

@Api
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnExpression("${testing.support.enabled:false}")
public class TestingSupportController {

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;
    private final CamundaRestEngineClient camundaRestEngineClient;

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

        var generalApplication = caseData.getGeneralApplications().stream().findFirst()
            .orElse(null);

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
    private static class BusinessProcessInfo {
        private BusinessProcess businessProcess;
        private String incidentMessage;
        private String ccdState;

        private BusinessProcessInfo(BusinessProcess businessProcess) {
            this.businessProcess = businessProcess;
        }
    }
}
