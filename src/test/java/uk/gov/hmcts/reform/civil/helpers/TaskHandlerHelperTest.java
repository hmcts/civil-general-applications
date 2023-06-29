package uk.gov.hmcts.reform.civil.helpers;

import org.camunda.bpm.client.task.ExternalTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.START_GA_BUSINESS_PROCESS;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_BUSINESS_PROCESS_STATE;

@SpringBootTest(classes = {
    TaskHandlerHelper.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class TaskHandlerHelperTest {

    @Autowired
    private TaskHandlerHelper taskHandlerHelper;
    @Mock
    private ExternalTask mockExternalTask;
    @MockBean
    private CoreCaseDataService coreCaseDataService;
    private static final String CASE_ID = "1";
    public static final String PROCESS_INSTANCE_ID = "processInstanceId";

    @BeforeEach
    void init() {
        when(mockExternalTask.getTopicName()).thenReturn("test");
        when(mockExternalTask.getWorkerId()).thenReturn("worker");
        when(mockExternalTask.getActivityId()).thenReturn("activityId");
        when(mockExternalTask.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);

        when(mockExternalTask.getAllVariables()).thenReturn(Map.of(
            "caseId", CASE_ID,
            "caseEvent", START_GA_BUSINESS_PROCESS.name()
        ));
    }

    @Test
    void should_return_gaCaseDataContent() {
        CaseData caseData = new CaseDataBuilder().atStateClaimDraft()
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.STARTED).build())
            .build();

        CaseDetails caseDetails = CaseDetailsBuilder.builder().data(caseData).build();
        StartEventResponse startEventResponse = startEventResponse(caseDetails);

        CaseDataContent caseDataContent = taskHandlerHelper
            .gaCaseDataContent(startEventResponse, caseData.getBusinessProcess());

        assertThat(caseDataContent).isEqualTo(getCaseDataContent(caseDetails, startEventResponse));

    }

    @Test
    void shouldNot_updateEventToFailedState_whenRemainingTries_moreThanOne() {
        CaseData caseData = new CaseDataBuilder().atStateClaimDraft()
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.STARTED).build())
            .build();

        CaseDetails caseDetails = CaseDetailsBuilder.builder().data(caseData).build();
        StartEventResponse startEventResponse = startEventResponse(caseDetails);
        when(coreCaseDataService.startGaUpdate(CASE_ID, UPDATE_BUSINESS_PROCESS_STATE))
            .thenReturn(startEventResponse);
        when(coreCaseDataService.submitGaUpdate(eq(CASE_ID), any(CaseDataContent.class))).thenReturn(caseData);

        taskHandlerHelper.updateEventToFailedState(mockExternalTask, 3);

        verify(coreCaseDataService, never()).startGaUpdate(CASE_ID, UPDATE_BUSINESS_PROCESS_STATE);
        CaseDataContent caseDataContentWithFinishedStatus = getCaseDataContent(caseDetails, startEventResponse);
        verify(coreCaseDataService, never()).submitGaUpdate(CASE_ID, caseDataContentWithFinishedStatus);

    }

    @Test
    void should_updateEventToFailedState_whenRemainingTries_isOne() {
        CaseData caseData = new CaseDataBuilder().atStateClaimDraft()
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.STARTED).build())
            .build();

        CaseDetails caseDetails = CaseDetailsBuilder.builder().data(caseData).build();
        StartEventResponse startEventResponse = startEventResponse(caseDetails);

        when(coreCaseDataService.startGaUpdate(CASE_ID, UPDATE_BUSINESS_PROCESS_STATE))
            .thenReturn(startEventResponse);
        when(coreCaseDataService.submitGaUpdate(eq(CASE_ID), any(CaseDataContent.class))).thenReturn(caseData);

        when(mockExternalTask.getRetries()).thenReturn(1);
        taskHandlerHelper.updateEventToFailedState(mockExternalTask, 3);

        verify(coreCaseDataService).startGaUpdate(CASE_ID, UPDATE_BUSINESS_PROCESS_STATE);
        CaseDataContent caseDataContentWithFailedStatus = getCaseDataContent(caseDetails, startEventResponse);
        verify(coreCaseDataService).submitGaUpdate(CASE_ID, caseDataContentWithFailedStatus);

    }

    private CaseDataContent getCaseDataContent(CaseDetails caseDetails, StartEventResponse value) {
        caseDetails.getData().put("businessProcess", BusinessProcess.builder()
            .status(BusinessProcessStatus.FAILED)
            .build());

        return CaseDataContent.builder()
            .eventToken(value.getToken())
            .event(Event.builder()
                       .id(value.getEventId())
                       .build())
            .data(caseDetails.getData())
            .build();
    }

    private StartEventResponse startEventResponse(CaseDetails caseDetails) {
        return StartEventResponse.builder()
            .token("1234")
            .eventId(UPDATE_BUSINESS_PROCESS_STATE.name())
            .caseDetails(caseDetails)
            .build();
    }
}
