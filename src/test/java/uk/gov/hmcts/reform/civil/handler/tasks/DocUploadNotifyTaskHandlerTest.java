package uk.gov.hmcts.reform.civil.handler.tasks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GA_EVIDENCE_UPLOAD_CHECK;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.search.EvidenceUploadNotificationSearchService;

import java.util.List;
import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    DocUploadNotifyTaskHandler.class})
public class DocUploadNotifyTaskHandlerTest {

    @MockBean
    private ExternalTask externalTask;

    @MockBean
    private ExternalTaskService externalTaskService;

    @MockBean
    private EvidenceUploadNotificationSearchService searchService;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private DocUploadNotifyTaskHandler handler;

    @Test
    void shouldNotSendMessageAndTriggerGaEvent_whenZeroCasesFound() {
        when(searchService.getApplications()).thenReturn(List.of());

        handler.execute(externalTask, externalTaskService);

        verify(searchService).getApplications();
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCasesFound() {
        long caseId = 1L;
        when(searchService.getApplications())
                .thenReturn(List.of(CaseDetails.builder().build()));

        when(caseDetailsConverter.toCaseData(any()))
                .thenReturn(CaseDataBuilder.builder().ccdCaseReference(caseId).build());

        handler.execute(externalTask, externalTaskService);

        verify(searchService).getApplications();
        verify(coreCaseDataService).triggerGaEvent(1L, GA_EVIDENCE_UPLOAD_CHECK,
                Map.of());
        verify(externalTaskService).complete(externalTask);
    }
}
