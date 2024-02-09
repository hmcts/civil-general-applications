package uk.gov.hmcts.reform.civil.handler.tasks;

import feign.FeignException;
import feign.Request;
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
import uk.gov.hmcts.reform.civil.service.search.CaseStateSearchService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static feign.Request.HttpMethod.GET;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.logging.log4j.util.Strings.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;

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
    private CaseStateSearchService searchService;

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
    public static final String EXCEPTION_MESSAGE = "Unprocessable Entity";
    public static final String UNEXPECTED_RESPONSE_BODY = "Case data validation failed";

    @BeforeEach
    void init() {
        caseDetails1 = CaseDetails.builder().id(1L).data(
            Map.of("generalAppNotificationDeadlineDate", deadlineCrossed.toString())).build();
        caseDetails2 = CaseDetails.builder().id(2L).data(
            Map.of("generalAppNotificationDeadlineDate", deadlineCrossed.toString())).build();
        caseDetails3 = CaseDetails.builder().id(3L).data(
            Map.of("generalAppNotificationDeadlineDate", deadlineInFuture.toString())).build();
        caseDetails4 = CaseDetails.builder().id(4L).data(
            Map.of("generalAppNotificationDeadlineDate", EMPTY)).build();
    }

    @Test
    void throwException_whenUnprocessableEntityIsFound() {
        doThrow(buildFeignExceptionWithUnprocessableEntity()).when(coreCaseDataService)
            .triggerEvent(any(), any());

        when(searchService.getGeneralApplications(AWAITING_RESPONDENT_RESPONSE))
            .thenReturn(List.of(caseDetails1, caseDetails2, caseDetails3));

        Exception e = assertThrows(FeignException.class, () -> coreCaseDataService
            .triggerEvent(any(), any()));

        gaResponseDeadlineTaskHandler.execute(externalTask, externalTaskService);

        assertThat(e.getMessage()).contains(EXCEPTION_MESSAGE);
    }

    @Test
    void throwException_whenUnprocessableEntity() {
        CaseDetails caseDetailsRespondentResponse = CaseDetails.builder().id(1L).data(
            Map.of("field", "outdatedField")).state(AWAITING_RESPONDENT_RESPONSE.toString()).build();

        when(searchService.getGeneralApplications(AWAITING_RESPONDENT_RESPONSE))
            .thenReturn(List.of(caseDetailsRespondentResponse));

        gaResponseDeadlineTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications(AWAITING_RESPONDENT_RESPONSE);
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    private FeignException buildFeignExceptionWithUnprocessableEntity() {
        return buildFeignException(422, UNEXPECTED_RESPONSE_BODY.getBytes(UTF_8));
    }

    private FeignException.FeignClientException buildFeignException(int status, byte[] body) {
        return new FeignException.FeignClientException(
            status,
            EXCEPTION_MESSAGE,
            Request.create(GET, "", Map.of(), new byte[]{}, UTF_8, null),
            body,
            Map.of()
        );
    }

    @Test
    void shouldNotSendMessageAndTriggerEvent_whenZeroCasesFound() {
        when(searchService.getGeneralApplications(AWAITING_RESPONDENT_RESPONSE)).thenReturn(List.of());

        gaResponseDeadlineTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications(AWAITING_RESPONDENT_RESPONSE);
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCasesPastDeadlineFound() {
        when(searchService.getGeneralApplications(AWAITING_RESPONDENT_RESPONSE))
            .thenReturn(List.of(caseDetails1, caseDetails2, caseDetails3));

        gaResponseDeadlineTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications(AWAITING_RESPONDENT_RESPONSE);
        verify(coreCaseDataService).triggerEvent(1L, CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION);
        verify(coreCaseDataService).triggerEvent(2L, CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCasesPastDeadlineNotFound() {
        when(searchService.getGeneralApplications(AWAITING_RESPONDENT_RESPONSE)).thenReturn(List.of(caseDetails3));

        gaResponseDeadlineTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications(AWAITING_RESPONDENT_RESPONSE);
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCasesFoundWithNullDeadlineDate() {
        when(searchService.getGeneralApplications(AWAITING_RESPONDENT_RESPONSE)).thenReturn(List.of(caseDetails4));

        gaResponseDeadlineTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications(AWAITING_RESPONDENT_RESPONSE);
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void getMaxAttemptsShouldAlwaysReturn1() {
        assertThat(gaResponseDeadlineTaskHandler.getMaxAttempts()).isEqualTo(1);
    }
}
