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
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAKE_PAYMENT_SERVICE_REQ_GASPEC;
import static uk.gov.hmcts.reform.civil.handler.tasks.BaseExternalTaskHandler.FLOW_FLAGS;

@SpringBootTest(classes = {
    PaymentTaskHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    StateFlowEngine.class
})
@ExtendWith(SpringExtension.class)
class PaymentTaskHandlerTest {

    private static final String CASE_ID = "1";

    @Mock
    private ExternalTask mockExternalTask;
    @Mock
    private ExternalTaskService externalTaskService;
    @MockBean
    private CoreCaseDataService coreCaseDataService;
    @MockBean
    private FeatureToggleService featureToggleService;
    @Autowired
    private PaymentTaskHandler paymentTaskHandler;

    @BeforeEach
    void init() {
        when(mockExternalTask.getTopicName()).thenReturn("test");
        when(mockExternalTask.getWorkerId()).thenReturn("worker");
        when(mockExternalTask.getActivityId()).thenReturn("activityId");

        when(mockExternalTask.getAllVariables())
            .thenReturn(Map.of("caseId", CASE_ID, "caseEvent", MAKE_PAYMENT_SERVICE_REQ_GASPEC.name()));
    }

    @Nested
    class SuccessHandler {

        @Test
        void shouldTriggerMakePbaPaymentCCDEvent_whenHandlerIsExecuted() {
            CaseData caseData = new CaseDataBuilder()
                .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
                .build();
            VariableMap variables = Variables.createVariables();
            variables.putValue(BaseExternalTaskHandler.FLOW_STATE, "MAIN.DRAFT");
            variables.putValue(FLOW_FLAGS, Collections.emptyMap());

            CaseDetails caseDetails = CaseDetailsBuilder.builder().data(caseData).build();

            when(coreCaseDataService.startGaUpdate(CASE_ID, MAKE_PAYMENT_SERVICE_REQ_GASPEC))
                .thenReturn(StartEventResponse.builder().caseDetails(caseDetails).build());

            when(coreCaseDataService.submitGaUpdate(eq(CASE_ID), any(CaseDataContent.class))).thenReturn(caseData);

            paymentTaskHandler.execute(mockExternalTask, externalTaskService);

            verify(coreCaseDataService).startGaUpdate(CASE_ID, MAKE_PAYMENT_SERVICE_REQ_GASPEC);
            verify(coreCaseDataService).submitGaUpdate(eq(CASE_ID), any(CaseDataContent.class));
            verify(externalTaskService).complete(mockExternalTask, variables);
        }
    }
}
