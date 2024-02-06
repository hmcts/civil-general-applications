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
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.search.CaseStateSearchService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_ADDITIONAL_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_DIRECTIONS_ORDER_DOCS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    GAJudgeRevisitTaskHandler.class
})
class GAJudgeRevisitTaskHandlerTest {

    @MockBean
    private ExternalTask externalTask;

    @MockBean
    private ExternalTaskService externalTaskService;

    @MockBean
    private CaseStateSearchService caseStateSearchService;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private GAJudgeRevisitTaskHandler gaJudgeRevisitTaskHandler;

    private CaseDetails caseDetailsDirectionOrder;
    private CaseDetails caseDetailsWrittenRepresentationS;
    private CaseDetails caseDetailsWrittenRepresentationC;
    private CaseDetails caseDetailRequestForInformation;

    @BeforeEach
    void init() {
        caseDetailsDirectionOrder = CaseDetails.builder().id(1L).data(
            Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                .directionsText("Test Direction")
                .reasonForDecisionText("Test Reason")
                .makeAnOrder(GIVE_DIRECTIONS_WITHOUT_HEARING)
                .directionsResponseByDate(LocalDate.now())
                .build())).state(AWAITING_DIRECTIONS_ORDER_DOCS.toString()).build();
        caseDetailsWrittenRepresentationC = CaseDetails.builder().id(2L).data(
            Map.of("judicialDecisionMakeAnOrderForWrittenRepresentations", GAJudicialWrittenRepresentations.builder()
                .writtenOption(CONCURRENT_REPRESENTATIONS)
                .writtenConcurrentRepresentationsBy(LocalDate.now())
                .build())).state(AWAITING_WRITTEN_REPRESENTATIONS.toString()).build();
        caseDetailsWrittenRepresentationS = CaseDetails.builder().id(3L).data(
            Map.of("judicialDecisionMakeAnOrderForWrittenRepresentations", GAJudicialWrittenRepresentations.builder()
                .writtenOption(SEQUENTIAL_REPRESENTATIONS)
                .sequentialApplicantMustRespondWithin(LocalDate.now())
                .build())).state(AWAITING_WRITTEN_REPRESENTATIONS.toString()).build();
        caseDetailRequestForInformation = CaseDetails.builder().id(4L).data(
            Map.of("judicialDecision", GAJudicialDecision.builder().decision(REQUEST_MORE_INFO).build(),
                   "judicialDecisionRequestMoreInfo", GAJudicialRequestMoreInfo.builder()
                       .requestMoreInfoOption(REQUEST_MORE_INFORMATION)
                       .judgeRequestMoreInfoByDate(LocalDate.now())
                       .judgeRequestMoreInfoText("test").build()
            )).state(AWAITING_ADDITIONAL_INFORMATION.toString()).build();
    }

    @Test
    void shouldNotSendMessageAndTriggerEvent_whenZeroCasesFound() {
        when(caseStateSearchService.getGeneralApplications(AWAITING_DIRECTIONS_ORDER_DOCS)).thenReturn(List.of());

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_DIRECTIONS_ORDER_DOCS);
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldNotEmitBusinessProcessEvent_whenDirectionOrderDateIsToday() {
        when(caseStateSearchService.getGeneralApplications(AWAITING_DIRECTIONS_ORDER_DOCS))
            .thenReturn(List.of(caseDetailsDirectionOrder));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_DIRECTIONS_ORDER_DOCS);
        verify(coreCaseDataService, times(0)).triggerEvent(1L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenDirectionOrderDateIsToday() {

        CaseDetails caseDetailsDirectionOrderWithPastDate = caseDetailsDirectionOrder.toBuilder().data(
            Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                .directionsText("Test Direction")
                .reasonForDecisionText("Test Reason")
                .makeAnOrder(GIVE_DIRECTIONS_WITHOUT_HEARING)
                .directionsResponseByDate(LocalDate.now().minusDays(2))
                .build())).state(AWAITING_DIRECTIONS_ORDER_DOCS.toString()).build();

        when(caseStateSearchService.getGeneralApplications(AWAITING_DIRECTIONS_ORDER_DOCS))
            .thenReturn(List.of(caseDetailsDirectionOrderWithPastDate));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_DIRECTIONS_ORDER_DOCS);
        verify(coreCaseDataService).triggerEvent(1L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldNotEmitBusinessProcessEvent_whenWrittenRepConcurrentDateIsToday() {
        when(caseStateSearchService.getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS))
            .thenReturn(List.of(caseDetailsWrittenRepresentationC));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS);
        verify(coreCaseDataService, times(0)).triggerEvent(2L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenWrittenRepConcurrentDateIsToday() {

        CaseDetails caseDetailsWrittenRepresentationConWithPastDate = caseDetailsWrittenRepresentationC.toBuilder().data(
            Map.of("judicialDecisionMakeAnOrderForWrittenRepresentations", GAJudicialWrittenRepresentations.builder()
                .writtenOption(CONCURRENT_REPRESENTATIONS)
                .writtenConcurrentRepresentationsBy(LocalDate.now().minusDays(1))
                .build())).state(AWAITING_WRITTEN_REPRESENTATIONS.toString()).build();

        when(caseStateSearchService.getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS))
            .thenReturn(List.of(caseDetailsWrittenRepresentationConWithPastDate));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS);
        verify(coreCaseDataService).triggerEvent(2L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldNotEmitBusinessProcessEvent_whenWrittenRepSequentialDateIsToday() {
        when(caseStateSearchService.getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS))
            .thenReturn(List.of(caseDetailsWrittenRepresentationS));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS);
        verify(coreCaseDataService, times(0))
            .triggerEvent(3L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenWrittenRepSequentialDateIsToday() {

        CaseDetails caseDetailsWrittenRepresentationSeqWithPastDate = caseDetailsWrittenRepresentationS.toBuilder().data(
            Map.of("judicialDecisionMakeAnOrderForWrittenRepresentations", GAJudicialWrittenRepresentations.builder()
                .writtenOption(SEQUENTIAL_REPRESENTATIONS)
                .sequentialApplicantMustRespondWithin(LocalDate.now().minusDays(1))
                .build())).state(AWAITING_WRITTEN_REPRESENTATIONS.toString()).build();

        when(caseStateSearchService.getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS))
            .thenReturn(List.of(caseDetailsWrittenRepresentationSeqWithPastDate));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS);
        verify(coreCaseDataService)
            .triggerEvent(3L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldNotEmitBusinessProcessEvent_whenRequestForInformationDateIsToday() {
        when(caseStateSearchService.getGeneralApplications(AWAITING_ADDITIONAL_INFORMATION))
            .thenReturn(List.of(caseDetailRequestForInformation));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_ADDITIONAL_INFORMATION);
        verify(coreCaseDataService, times(0)).triggerEvent(4L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenRequestForInformationDateIsToday() {

        CaseDetails caseDetailRequestForInformationWithPastDate = caseDetailRequestForInformation.toBuilder().data(
            Map.of("judicialDecision", GAJudicialDecision.builder().decision(REQUEST_MORE_INFO).build(),
                   "judicialDecisionRequestMoreInfo", GAJudicialRequestMoreInfo.builder()
                       .requestMoreInfoOption(REQUEST_MORE_INFORMATION)
                       .judgeRequestMoreInfoByDate(LocalDate.now().minusDays(1))
                       .judgeRequestMoreInfoText("test").build()
            )).state(AWAITING_ADDITIONAL_INFORMATION.toString()).build();

        when(caseStateSearchService.getGeneralApplications(AWAITING_ADDITIONAL_INFORMATION))
            .thenReturn(List.of(caseDetailRequestForInformationWithPastDate));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_ADDITIONAL_INFORMATION);
        verify(coreCaseDataService).triggerEvent(4L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void getMaxAttemptsShouldAlwaysReturn1() {
        assertThat(gaJudgeRevisitTaskHandler.getMaxAttempts()).isEqualTo(1);
    }
}
