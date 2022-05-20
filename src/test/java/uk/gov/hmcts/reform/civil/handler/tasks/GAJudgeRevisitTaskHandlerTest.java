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
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.search.AwaitingResponseStatusSearchService;
import uk.gov.hmcts.reform.civil.service.search.DirectionOrderSearchService;
import uk.gov.hmcts.reform.civil.service.search.RequestForInformationrSearchService;
import uk.gov.hmcts.reform.civil.service.search.WrittenRepresentationSearchService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.apache.logging.log4j.util.Strings.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    GAJudgeRevisitTaskHandler.class})
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

    private CaseDetails caseDetailsDirectionOrder1;
    private CaseDetails caseDetailsDirectionOrder2;
    private CaseDetails caseDetails3;
    private CaseDetails caseDetails4;

    private final LocalDateTime dateBy = LocalDateTime.now();
    private final LocalDateTime dateByInFuture = LocalDateTime.now().plusDays(2);

    @BeforeEach
    void init() {
        caseDetailsDirectionOrder1 = CaseDetails.builder().id(1L).data(
            Map.of("makeAnOrder", GIVE_DIRECTIONS_WITHOUT_HEARING, "directionsResponseByDate", dateBy)).build();
        caseDetailsDirectionOrder2 = CaseDetails.builder().id(2L).data(
            Map.of("makeAnOrder", GIVE_DIRECTIONS_WITHOUT_HEARING, "directionsResponseByDate", dateByInFuture)).build();
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
    void shouldEmitBusinessProcessEvent_whenCasesPastDeadlineFound() {
        when(directionOrderSearchService.getGeneralApplications()).thenReturn(List.of(caseDetailsDirectionOrder1));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(directionOrderSearchService).getGeneralApplications();
        verify(coreCaseDataService).triggerEvent(1L, CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION);
        verify(coreCaseDataService).triggerEvent(2L, CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void getMaxAttemptsShouldAlwaysReturn1() {
        assertThat(gaJudgeRevisitTaskHandler.getMaxAttempts()).isEqualTo(1);
    }

    private CaseDataBuilder directionOrder(LocalDate date){
        CaseDataBuilder.builder().generalApplications(Ga)
    }
}
