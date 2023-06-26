package uk.gov.hmcts.reform.civil.handler.tasks;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.controllers.testingsupport.CamundaRestEngineClient;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.search.CaseStateSearchService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    FailedEventEmitterHandler.class},
    properties =
        {"failed.event.emitter.enabled = true"})
class FailedEventEmitterHandlerTest {

    @MockBean
    private ExternalTask externalTask;
    @MockBean
    private ExternalTaskService externalTaskService;
    @MockBean
    private CaseStateSearchService searchService;
    @MockBean
    private CaseDetailsConverter caseDetailsConverter;
    @MockBean
    private CamundaRestEngineClient camundaRestEngineClient;

    @Autowired
    private FailedEventEmitterHandler failedEventEmitterHandler;

    private CaseDetails caseDetails1;
    private CaseDetails caseDetails2;
    private CaseData caseData1;
    private CaseData caseData2;

    @BeforeEach
    void init() {
        caseDetails1 = CaseDetails.builder().id(1L).data(
            new HashMap<>(Map.of("businessProcess",
                                 BusinessProcess.builder()
                                     .status(BusinessProcessStatus.FAILED)
                                     .camundaEvent("MAKE_DECISION")
                                     .activityId("CreateClaimPaymentSuccessfulNotifyRespondentSolicitor1")
                                     .failedExternalTaskId("1111-1111")
                                     .build()))).build();
        caseDetails2 = CaseDetails.builder().id(2L).data(
            new HashMap<>(Map.of("businessProcess",
                   BusinessProcess.builder()
                       .status(BusinessProcessStatus.FAILED)
                       .camundaEvent("MAKE_DECISION")
                       .activityId("CreateClaimPaymentSuccessfulNotifyRespondentSolicitor1")
                       .failedExternalTaskId("2222-2222")
                       .build()))).build();

        caseData1 = getCaseData(1L, "1111-1111");
        caseData2 = getCaseData(2L, "2222-2222");

    }

    @Test
    void shouldNotSendMessageAndTriggerEvent_whenZeroCasesFound() {
        when(searchService.getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.FAILED))
            .thenReturn(List.of());

        failedEventEmitterHandler.execute(externalTask, externalTaskService);

        verifyNoInteractions(camundaRestEngineClient);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCases_withStatusFailed() {

        when(searchService.getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.FAILED))
            .thenReturn(List.of(caseDetails1, caseDetails2));

        when(caseDetailsConverter.toCaseData(caseDetails1)).thenReturn(caseData1);
        when(caseDetailsConverter.toCaseData(caseDetails2)).thenReturn(caseData2);

        failedEventEmitterHandler.execute(externalTask, externalTaskService);
        verify(searchService).getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.FAILED);

        verify(camundaRestEngineClient).reTriggerFailedTask(Arrays.asList("1111-1111", "2222-2222"));

    }

    @Test
    void getMaxAttemptsShouldAlwaysReturn1() {
        assertThat(failedEventEmitterHandler.getMaxAttempts()).isEqualTo(1);
    }

    private CaseData getCaseData(Long ccdId, String failedTaskId) {
        return CaseDataBuilder.builder()
            .ccdCaseReference(ccdId)
            .ccdState(ORDER_MADE)
            .generalAppParentCaseLink(GeneralAppParentCaseLink.builder().caseReference(String.valueOf(ccdId)).build())
            .businessProcess(BusinessProcess.builder()
                                 .status(BusinessProcessStatus.STARTED)
                                 .activityId("CreateClaimPaymentSuccessfulNotifyRespondentSolicitor1")
                                 .camundaEvent("MAKE_DECISION")
                                 .failedExternalTaskId(failedTaskId)
                                 .build())
            .build();
    }
}
