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
import uk.gov.hmcts.reform.civil.service.search.DirectionOrderSearchService;
import uk.gov.hmcts.reform.civil.service.search.RequestForInformationrSearchService;
import uk.gov.hmcts.reform.civil.service.search.WrittenRepresentationSearchService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
    private WrittenRepresentationSearchService writtenRepresentationSearchService;

    @MockBean
    private DirectionOrderSearchService directionOrderSearchService;

    @MockBean
    private RequestForInformationrSearchService requestForInformationrSearchService;

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
        when(directionOrderSearchService.getGeneralApplications()).thenReturn(List.of());

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(directionOrderSearchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenDirectionOrderDateIsToday() {
        when(directionOrderSearchService.getGeneralApplications()).thenReturn(List.of(caseDetailsDirectionOrder));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(directionOrderSearchService).getGeneralApplications();
        verify(coreCaseDataService).triggerEvent(1L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenWrittenRepConcurrentDateIsToday() {
        when(writtenRepresentationSearchService.getGeneralApplications())
            .thenReturn(List.of(caseDetailsWrittenRepresentationC));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(writtenRepresentationSearchService).getGeneralApplications();
        verify(coreCaseDataService).triggerEvent(2L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenWrittenRepSequentialDateIsToday() {
        when(writtenRepresentationSearchService.getGeneralApplications())
            .thenReturn(List.of(caseDetailsWrittenRepresentationS));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(writtenRepresentationSearchService).getGeneralApplications();
        verify(coreCaseDataService).triggerEvent(3L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenRequestForInformationDateIsToday() {
        when(requestForInformationrSearchService.getGeneralApplications())
            .thenReturn(List.of(caseDetailRequestForInformation));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(requestForInformationrSearchService).getGeneralApplications();
        verify(coreCaseDataService).triggerEvent(4L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void getMaxAttemptsShouldAlwaysReturn1() {
        assertThat(gaJudgeRevisitTaskHandler.getMaxAttempts()).isEqualTo(1);
    }
}
