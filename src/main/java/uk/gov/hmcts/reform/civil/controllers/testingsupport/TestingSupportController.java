package uk.gov.hmcts.reform.civil.controllers.testingsupport;

import feign.FeignException;
import io.swagger.annotations.Api;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.search.Query;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
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

        return new ResponseEntity<>(businessProcessInfo, HttpStatus.OK);
    }

    @GetMapping("/testing-support/case/{caseId}")
    public ResponseEntity<CaseDetails> getCaseData(@PathVariable("caseId") Long caseId) {

        StartEventResponse startEventResponse = coreCaseDataService.startUpdate(String.valueOf(caseId),
                                                CaseEvent.CREATE_GENERAL_APPLICATION_CASE);

        return new ResponseEntity<>(startEventResponse.getCaseDetails(), HttpStatus.OK);
    }

    @GetMapping("/testing-support/applications/{state}")
    public ResponseEntity<List<CaseDetails>> getApplicationsInAwaiting(@PathVariable("state") String state) {
        Query query = new Query(QueryBuilders.matchQuery("state", state), emptyList(), 0);

        SearchResult searchResult = coreCaseDataService.searchGeneralApplication(query);
        List<CaseDetails> listOfCases = searchResult.getCases();
        List<CaseDetails> collectedCases = listOfCases.stream().filter(a ->
                        LocalDateTime.now().isAfter(
                                LocalDateTime.parse(caseDetailsConverter.toCaseData(a).getGeneralAppDeadlineNotificationDate())))
                .collect(Collectors.toList());

        return new ResponseEntity<>(collectedCases, HttpStatus.OK);
    }

    @Data
    private static class BusinessProcessInfo {
        private BusinessProcess businessProcess;
        private String incidentMessage;

        private BusinessProcessInfo(BusinessProcess businessProcess) {
            this.businessProcess = businessProcess;
        }
    }
}
