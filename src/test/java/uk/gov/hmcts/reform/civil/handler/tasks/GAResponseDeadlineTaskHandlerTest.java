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
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.search.AwaitingResponseStatusSearchService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.apache.logging.log4j.util.Strings.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    GAResponseDeadlineTaskHandler.class})
class GAResponseDeadlineTaskHandlerTest {

    @MockBean
    private ExternalTask externalTask;

    @MockBean
    private ExternalTaskService externalTaskService;

    @MockBean
    private AwaitingResponseStatusSearchService searchService;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private GAResponseDeadlineTaskHandler gaResponseDeadlineTaskHandler;

    private CaseDetails caseDetails1;
    private CaseDetails caseDetails2;
    private CaseDetails caseDetails3;
    private CaseDetails caseDetails4;

    private final LocalDateTime deadlineCrossed = LocalDateTime.now().minusDays(2);
    private final LocalDateTime deadlineInFuture = LocalDateTime.now().plusDays(2);

    @BeforeEach
    void init() {
        caseDetails1 = CaseDetails.builder().id(1L).data(
            Map.of("generalAppDeadlineNotificationDate", deadlineCrossed.toString())).build();
        caseDetails2 = CaseDetails.builder().id(2L).data(
            Map.of("generalAppDeadlineNotificationDate", deadlineCrossed.toString())).build();
        caseDetails3 = CaseDetails.builder().id(3L).data(
            Map.of("generalAppDeadlineNotificationDate", deadlineInFuture.toString())).build();
        caseDetails4 = CaseDetails.builder().id(4L).data(
            Map.of("generalAppDeadlineNotificationDate", EMPTY)).build();
    }

    @Test
    void shouldNotSendMessageAndTriggerEvent_whenZeroCasesFound() {
        when(searchService.getGeneralApplications()).thenReturn(List.of());

        gaResponseDeadlineTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCasesPastDeadlineFound() {
        when(searchService.getGeneralApplications()).thenReturn(List.of(caseDetails1, caseDetails2, caseDetails3));

        gaResponseDeadlineTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verify(coreCaseDataService).triggerEvent(1L, CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION);
        verify(coreCaseDataService).triggerEvent(2L, CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCasesPastDeadlineNotFound() {
        when(searchService.getGeneralApplications()).thenReturn(List.of(caseDetails3));

        gaResponseDeadlineTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCasesFoundWithNullDeadlineDate() {
        when(searchService.getGeneralApplications()).thenReturn(List.of(caseDetails4));

        gaResponseDeadlineTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void getMaxAttemptsShouldAlwaysReturn1() {
        assertThat(gaResponseDeadlineTaskHandler.getMaxAttempts()).isEqualTo(1);
    }
}
