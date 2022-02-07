package uk.gov.hmcts.reform.civil.handler.tasks;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_GENERAL_APPLICATION_RESPONDENT;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;
import static uk.gov.hmcts.reform.civil.handler.tasks.BaseExternalTaskHandler.FLOW_FLAGS;

@SpringBootTest(classes = {
    NotifyGeneralApplicationRespondentTaskHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    StateFlowEngine.class
})
@ExtendWith(SpringExtension.class)
public class NotifyGeneralApplicationRespondentTaskHandlerTest {

    private static final String PROCESS_INSTANCE_ID = "1";
    private static final String CASE_ID = "1";
    private static final String GA_ID = "2";

    @Mock
    private ExternalTask mockTask;

    @Mock
    private ExternalTaskService externalTaskService;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @MockBean
    private CaseDataContent caseDataContentMock;

    @Autowired
    private NotifyGeneralApplicationRespondentTaskHandler handler;

    @BeforeEach
    void init() {
        when(mockTask.getTopicName()).thenReturn("test");
        when(mockTask.getWorkerId()).thenReturn("worker");
        when(mockTask.getActivityId()).thenReturn("activityId");
    }

    @Nested
    class NotifyGeneralApplicationRespondent {

        @BeforeEach
        void init() {
            when(mockTask.getAllVariables())
                .thenReturn(Map.of("caseId", CASE_ID,
                                   "caseEvent", NOTIFY_GENERAL_APPLICATION_RESPONDENT.name(),
                                   "generalApplicationCaseId",GA_ID
                ));
        }

        @Test
        void shouldTriggerCCDEvent() {

            CaseData caseData = new CaseDataBuilder()
                .businessProcess(BusinessProcess.builder().status(STARTED)
                                     .processInstanceId(PROCESS_INSTANCE_ID).build()).build();
            VariableMap variables = Variables.createVariables();
            variables.putValue(BaseExternalTaskHandler.FLOW_STATE, "MAIN.DRAFT");
            variables.putValue(FLOW_FLAGS, Collections.emptyMap());
            variables.putValue("generalApplicationCaseId", GA_ID);

            CaseDetails caseDetails = CaseDetailsBuilder.builder().data(caseData).build();
            StartEventResponse startEventResponse = StartEventResponse.builder().caseDetails(caseDetails).build();

            Event event = Event.builder().build();
            CaseDataContent caseDataContent = CaseDataContent.builder().build();
            when(coreCaseDataService.startGaUpdate(anyString(), any(CaseEvent.class)))
                .thenReturn(startEventResponse);
            when(caseDataContentMock.getEvent()).thenReturn(event);
            when(coreCaseDataService.caseDataContentFromStartEventResponse(any(StartEventResponse.class), anyMap())).thenReturn(caseDataContent);
            when(caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails()))
                .thenReturn(caseData);
            when(coreCaseDataService.submitGaUpdate(anyString(), any(CaseDataContent.class))).thenReturn(caseData);
            handler.execute(mockTask,externalTaskService);

            verify(coreCaseDataService).startGaUpdate(GA_ID,NOTIFY_GENERAL_APPLICATION_RESPONDENT);
            verify(coreCaseDataService).submitGaUpdate(GA_ID,caseDataContent);
            verify(externalTaskService).complete(mockTask,Map.of(
                BaseExternalTaskHandler.FLOW_STATE,"MAIN.DRAFT",
                FLOW_FLAGS, Collections.emptyMap()
            ));
        }
    }
}
