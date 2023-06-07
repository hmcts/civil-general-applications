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
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.EventEmitterService;
import uk.gov.hmcts.reform.civil.service.search.CaseStateSearchService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    PollingEventEmitterHandler.class})
class PollingEventEmitterHandlerTest {

    @MockBean
    private ExternalTask externalTask;

    @MockBean
    private ExternalTaskService externalTaskService;

    @MockBean
    private CaseStateSearchService searchService;

    @MockBean
    private EventEmitterService eventEmitterService;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @Autowired
    private PollingEventEmitterHandler pollingEventEmitterHandler;

    private CaseDetails caseDetails1;
    private CaseDetails caseDetails2;
    private CaseDetails caseDetails3;
    private CaseData caseData1;
    private CaseData caseData2;
    private CaseData caseData3;

    @BeforeEach
    void init() {
        caseDetails1 = CaseDetails.builder().id(1L).data(
            Map.of("businessProcess",
                   BusinessProcess.builder()
                       .status(BusinessProcessStatus.STARTED)
                       .activityId("CreateClaimPaymentSuccessfulNotifyRespondentSolicitor1").build())).build();
        caseDetails2 = CaseDetails.builder().id(2L).data(
            Map.of("businessProcess",
                   BusinessProcess.builder()
                       .status(BusinessProcessStatus.STARTED)
                       .activityId("CreateClaimPaymentSuccessfulNotifyRespondentSolicitor1").build())).build();
        caseDetails3 = CaseDetails.builder().id(3L).data(
            Map.of("businessProcess",
                   BusinessProcess.builder()
                       .status(BusinessProcessStatus.FINISHED)
                       .activityId("CreateClaimPaymentSuccessfulNotifyRespondentSolicitor1").build())).build();

        caseData1 = getCaseData(1l);
        caseData2 = getCaseData(2l);
        caseData3 = getCaseData(3l);

    }

    @Test
    void shouldNotSendMessageAndTriggerEvent_whenZeroCasesFound() {
        when(searchService.getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.STARTED))
            .thenReturn(List.of());

        pollingEventEmitterHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.STARTED);
        verifyNoInteractions(eventEmitterService);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCases_withStartedStatusSTARTED() {
        when(searchService.getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.STARTED))
            .thenReturn(List.of(caseDetails1, caseDetails2, caseDetails3));

        when(caseDetailsConverter.toCaseData(caseDetails1)).thenReturn(caseData1);
        when(caseDetailsConverter.toCaseData(caseDetails2)).thenReturn(caseData2);
        when(caseDetailsConverter.toCaseData(caseDetails3)).thenReturn(caseData3);

        pollingEventEmitterHandler.execute(externalTask, externalTaskService);
        verify(searchService).getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.STARTED);
        verify(eventEmitterService).emitBusinessProcessCamundaGAEvent(getCaseData(1l), false);
        verify(eventEmitterService).emitBusinessProcessCamundaGAEvent(getCaseData(2l), false);
        verify(eventEmitterService).emitBusinessProcessCamundaGAEvent(getCaseData(3l), false);
        verifyNoMoreInteractions(eventEmitterService);

    }

    @Test
    void getMaxAttemptsShouldAlwaysReturn1() {
        assertThat(pollingEventEmitterHandler.getMaxAttempts()).isEqualTo(1);
    }

    private CaseData getCaseData(Long ccdId) {
        return CaseDataBuilder.builder()
            .ccdCaseReference(ccdId)
            .ccdState(ORDER_MADE)
            .businessProcess(BusinessProcess.builder()
                                 .status(BusinessProcessStatus.STARTED)
                                 .activityId("CreateClaimPaymentSuccessfulNotifyRespondentSolicitor1")
                                 .build())
            .build();
    }
}
