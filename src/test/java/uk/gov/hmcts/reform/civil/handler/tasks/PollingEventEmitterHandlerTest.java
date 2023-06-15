package uk.gov.hmcts.reform.civil.handler.tasks;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.TaskHandlerHelper;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.EventEmitterService;
import uk.gov.hmcts.reform.civil.service.search.CaseStateSearchService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_BUSINESS_PROCESS_STATE;
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
    @MockBean
    private CoreCaseDataService coreCaseDataService;
    @MockBean
    private TaskHandlerHelper taskHandlerHelper;

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
            new HashMap<>(Map.of("businessProcess",
                                 BusinessProcess.builder()
                                     .status(BusinessProcessStatus.FAILED)
                                     .camundaEvent("MAKE_DECISION")
                                     .activityId("CreateClaimPaymentSuccessfulNotifyRespondentSolicitor1").build()))).build();
        caseDetails2 = CaseDetails.builder().id(2L).data(
            new HashMap<>(Map.of("businessProcess",
                   BusinessProcess.builder()
                       .status(BusinessProcessStatus.FAILED)
                       .camundaEvent("MAKE_DECISION")
                       .activityId("CreateClaimPaymentSuccessfulNotifyRespondentSolicitor1").build()))).build();
        caseDetails3 = CaseDetails.builder().id(3L).data(
            new HashMap<>(Map.of("businessProcess",
                   BusinessProcess.builder()
                       .status(BusinessProcessStatus.FINISHED)
                       .camundaEvent("MAKE_DECISION")
                       .activityId("CreateClaimPaymentSuccessfulNotifyRespondentSolicitor1").build()))).build();

        caseData1 = getCaseData(1L);
        caseData2 = getCaseData(2L);
        caseData3 = getCaseData(3L);

    }

    @Test
    void shouldNotSendMessageAndTriggerEvent_whenZeroCasesFound() {
        when(searchService.getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.FAILED))
            .thenReturn(List.of());

        pollingEventEmitterHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.FAILED);
        verifyNoInteractions(eventEmitterService);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCases_withStatusFailed() {

        when(searchService.getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.FAILED))
            .thenReturn(List.of(caseDetails1, caseDetails2));

        when(coreCaseDataService.startGaUpdate(String.valueOf(1L), UPDATE_BUSINESS_PROCESS_STATE))
            .thenReturn(getStartEventResponse(caseDetails1));

        when(coreCaseDataService.startGaUpdate(String.valueOf(2L), UPDATE_BUSINESS_PROCESS_STATE))
            .thenReturn(getStartEventResponse(caseDetails1));

        when(caseDetailsConverter.toCaseData(caseDetails1)).thenReturn(caseData1);
        when(caseDetailsConverter.toCaseData(caseDetails2)).thenReturn(caseData2);
        when(caseDetailsConverter.toCaseData(caseDetails3)).thenReturn(caseData3);

        when(taskHandlerHelper.gaCaseDataContent(getStartEventResponse(caseDetails2), caseData2.getBusinessProcess()))
            .thenReturn(gaCaseDataContent(getStartEventResponse(caseDetails2), caseData2.getBusinessProcess()));

        when(taskHandlerHelper.gaCaseDataContent(getStartEventResponse(caseDetails1), caseData1.getBusinessProcess()))
            .thenReturn(gaCaseDataContent(getStartEventResponse(caseDetails1), caseData1.getBusinessProcess()));

        pollingEventEmitterHandler.execute(externalTask, externalTaskService);
        verify(searchService).getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.FAILED);
        verify(coreCaseDataService).startGaUpdate(String.valueOf(1L), UPDATE_BUSINESS_PROCESS_STATE);

        verify(coreCaseDataService)
            .submitGaUpdate(String.valueOf(1L),
                            gaCaseDataContent(getStartEventResponse(caseDetails1), caseData1.getBusinessProcess()));

        verify(coreCaseDataService).startGaUpdate(String.valueOf(2L), UPDATE_BUSINESS_PROCESS_STATE);
        verify(coreCaseDataService)
            .submitGaUpdate(String.valueOf(2L),
                            gaCaseDataContent(getStartEventResponse(caseDetails2), caseData2.getBusinessProcess()));

        verify(eventEmitterService).emitBusinessProcessCamundaGAEvent(getCaseData(1L), true);
        verify(eventEmitterService).emitBusinessProcessCamundaGAEvent(getCaseData(2L), true);
        verifyNoMoreInteractions(eventEmitterService);

    }

    @Test
    void shouldNotEmitBusinessProcessEvent_whenEvent_isNotCorrect() {

        when(searchService.getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.FAILED))
            .thenReturn(List.of(caseDetails1, caseDetails2));

        when(coreCaseDataService.startGaUpdate(String.valueOf(1L), UPDATE_BUSINESS_PROCESS_STATE))
            .thenReturn(getStartEventResponse(caseDetails1));

        when(coreCaseDataService.startGaUpdate(String.valueOf(2L), UPDATE_BUSINESS_PROCESS_STATE))
            .thenReturn(getStartEventResponse(caseDetails1));

        BusinessProcess businessProcess = BusinessProcess
            .builder()
            .camundaEvent("INITIATE_GENERAL_APPLICATION")
            .build();

        when(caseDetailsConverter.toCaseData(caseDetails1))
            .thenReturn(caseData1.toBuilder().businessProcess(businessProcess).build());
        when(caseDetailsConverter.toCaseData(caseDetails2))
            .thenReturn(caseData2.toBuilder().businessProcess(businessProcess).build());

        when(taskHandlerHelper.gaCaseDataContent(getStartEventResponse(caseDetails2), caseData2.getBusinessProcess()))
            .thenReturn(gaCaseDataContent(getStartEventResponse(caseDetails2), caseData2.getBusinessProcess()));

        when(taskHandlerHelper.gaCaseDataContent(getStartEventResponse(caseDetails1), caseData1.getBusinessProcess()))
            .thenReturn(gaCaseDataContent(getStartEventResponse(caseDetails1), caseData1.getBusinessProcess()));

        pollingEventEmitterHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplicationsWithBusinessProcess(BusinessProcessStatus.FAILED);
        verify(coreCaseDataService, never()).startGaUpdate(String.valueOf(1L), UPDATE_BUSINESS_PROCESS_STATE);

        verify(coreCaseDataService, never())
            .submitGaUpdate(String.valueOf(1L),
                            gaCaseDataContent(getStartEventResponse(caseDetails1), caseData1.getBusinessProcess()));

        verify(coreCaseDataService, never()).startGaUpdate(String.valueOf(2L), UPDATE_BUSINESS_PROCESS_STATE);
        verify(coreCaseDataService, never())
            .submitGaUpdate(String.valueOf(2L),
                            gaCaseDataContent(getStartEventResponse(caseDetails2), caseData2.getBusinessProcess()));

        verify(eventEmitterService, never()).emitBusinessProcessCamundaGAEvent(getCaseData(1L), true);
        verify(eventEmitterService, never()).emitBusinessProcessCamundaGAEvent(getCaseData(2L), true);

    }

    @Test
    void getMaxAttemptsShouldAlwaysReturn1() {
        assertThat(pollingEventEmitterHandler.getMaxAttempts()).isEqualTo(1);
    }

    private CaseData getCaseData(Long ccdId) {
        return CaseDataBuilder.builder()
            .ccdCaseReference(ccdId)
            .ccdState(ORDER_MADE)
            .generalAppParentCaseLink(GeneralAppParentCaseLink.builder().caseReference(String.valueOf(ccdId)).build())
            .businessProcess(BusinessProcess.builder()
                                 .status(BusinessProcessStatus.STARTED)
                                 .activityId("CreateClaimPaymentSuccessfulNotifyRespondentSolicitor1")
                                 .camundaEvent("MAKE_DECISION")
                                 .build())
            .build();
    }

    private StartEventResponse getStartEventResponse(CaseDetails caseDetails) {
        return StartEventResponse.builder()
            .eventId(UPDATE_BUSINESS_PROCESS_STATE.name())
            .caseDetails(caseDetails)
            .token("1234")
            .build();
    }

    private CaseDataContent gaCaseDataContent(StartEventResponse startGaEventResponse,
                                              BusinessProcess businessProcess) {
        Map<String, Object> objectDataMap = startGaEventResponse.getCaseDetails().getData();
        objectDataMap.put("businessProcess", businessProcess);

        return CaseDataContent.builder()
            .eventToken(startGaEventResponse.getToken())
            .event(Event.builder().id(startGaEventResponse.getEventId())
                       .build())
            .data(objectDataMap)
            .build();

    }
}
