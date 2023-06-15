package uk.gov.hmcts.reform.civil.handler.tasks;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.TaskHandlerHelper;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_HEARING_SCHEDULED_PROCESS_GASPEC;

@SpringBootTest(classes = {
    EndHearingScheduledBusinessProcessTaskHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
})
@ExtendWith(SpringExtension.class)
class EndHearingScheduledBusinessProcessTaskHandlerTest {

    private static final String CASE_ID = "1234";
    public static final String PROCESS_INSTANCE_ID = "processInstanceId";

    @Mock
    private ExternalTask mockExternalTask;

    @Mock
    private ExternalTaskService externalTaskService;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @MockBean
    private TaskHandlerHelper taskHandlerHelper;

    @Autowired
    private EndHearingScheduledBusinessProcessTaskHandler handler;

    @BeforeEach
    void init() {
        when(mockExternalTask.getTopicName()).thenReturn("test");
        when(mockExternalTask.getWorkerId()).thenReturn("worker");
        when(mockExternalTask.getActivityId()).thenReturn("activityId");
        when(mockExternalTask.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);

        when(mockExternalTask.getAllVariables())
            .thenReturn(Map.of(
                "caseId", CASE_ID,
                "caseEvent", END_HEARING_SCHEDULED_PROCESS_GASPEC
            ));
    }

    @Test
    void shouldTriggerEndBusinessProcessCCDEventAndUpdateBusinessProcessStatusToFinished_whenCalled() {
        CaseData caseData = new CaseDataBuilder().atStateClaimDraft()
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .build();

        CaseDetails caseDetails = CaseDetailsBuilder.builder().data(caseData).build();
        StartEventResponse startEventResponse = startEventResponse(caseDetails);

        when(coreCaseDataService.startGaUpdate(CASE_ID, END_HEARING_SCHEDULED_PROCESS_GASPEC))
            .thenReturn(startEventResponse);
        when(taskHandlerHelper.gaCaseDataContent(any(), any()))
            .thenReturn(getCaseDataContent(caseDetails, startEventResponse));
        when(coreCaseDataService.submitGaUpdate(eq(CASE_ID), any(CaseDataContent.class))).thenReturn(caseData);

        CaseDataContent caseDataContentWithFinishedStatus = getCaseDataContent(caseDetails, startEventResponse);

        handler.execute(mockExternalTask, externalTaskService);

        verify(coreCaseDataService).startGaUpdate(CASE_ID, END_HEARING_SCHEDULED_PROCESS_GASPEC);
        verify(coreCaseDataService).submitGaUpdate(CASE_ID, caseDataContentWithFinishedStatus);
        verify(externalTaskService).complete(mockExternalTask);
    }

    @Test
    void shouldTrigger_HandleFailure_whenThereIsException() {

        when(coreCaseDataService.startGaUpdate(CASE_ID, END_HEARING_SCHEDULED_PROCESS_GASPEC))
            .thenAnswer(invocation -> {
                throw new Exception("errorMessage");
            });

        handler.execute(mockExternalTask, externalTaskService);

        verify(taskHandlerHelper, times(1))
            .updateEventToFailedState(mockExternalTask, 3);
    }

    private StartEventResponse startEventResponse(CaseDetails caseDetails) {
        return StartEventResponse.builder()
            .token("1234")
            .eventId(END_HEARING_SCHEDULED_PROCESS_GASPEC.name())
            .caseDetails(caseDetails)
            .build();
    }

    private CaseDataContent getCaseDataContent(CaseDetails caseDetails, StartEventResponse value) {
        caseDetails.getData().put("businessProcess", BusinessProcess.builder()
            .status(BusinessProcessStatus.FINISHED)
            .build());

        return CaseDataContent.builder()
            .eventToken(value.getToken())
            .event(Event.builder()
                       .id(value.getEventId())
                       .build())
            .data(caseDetails.getData())
            .build();
    }
}
