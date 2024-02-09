
package uk.gov.hmcts.reform.civil.handler.tasks;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import feign.FeignException;
import feign.Request;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
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

import static feign.Request.HttpMethod.GET;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

    public static final String EXCEPTION_MESSAGE = "Unprocessable Entity found";
    public static final String UNEXPECTED_RESPONSE_BODY = "Case data validation failed";

    Logger logger = (Logger) LoggerFactory.getLogger(GAJudgeRevisitTaskHandler.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

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
    void throwException_whenUnprocessableEntityIsFound() {
        listAppender.start();
        logger.addAppender(listAppender);
        doThrow(buildFeignExceptionWithUnprocessableEntity()).when(coreCaseDataService)
            .triggerEvent(any(), any());

        gaJudgeRevisitTaskHandler.fireEventForStateChange(CaseDetails.builder().id(1L).build());

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Error in GAJudgeRevisitTaskHandler::fireEventForStateChange: "
                         + "feign.FeignException$FeignClientException: Unprocessable Entity found",
                     logsList.get(1).getMessage());
        assertEquals(Level.ERROR, logsList.get(1).getLevel());
    }

    @Test
    void throwException_whenUnprocessableEntity() {
        listAppender.start();
        logger.addAppender(listAppender);
        CaseDetails caseDetailRequestForInformation = caseDetailsDirectionOrder.toBuilder().data(
            Map.of("generalAppConsentOrder", "maybe")).state(AWAITING_ADDITIONAL_INFORMATION.toString()).build();

        when(caseStateSearchService.getGeneralApplications(AWAITING_ADDITIONAL_INFORMATION))
            .thenReturn(List.of(caseDetailRequestForInformation));

        gaJudgeRevisitTaskHandler.getRequestForInformationCaseReadyToJudgeRevisit();

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("GAJudgeRevisitTaskHandler failed: java.lang.IllegalArgumentException: "
                         + "Cannot deserialize value of type `uk.gov.hmcts.reform.civil.enums.YesOrNo` "
                         + "from String \"maybe\": not one of the values accepted for Enum class: [No, Yes]\n"
                         + " at [Source: UNKNOWN; byte offset: #UNKNOWN] "
                         + "(through reference chain: "
                         + "uk.gov.hmcts.reform.civil.model.CaseData[\"generalAppConsentOrder\"])",
                     logsList.get(0).getMessage());
        assertEquals(Level.ERROR, logsList.get(0).getLevel());
        listAppender.stop();
    }

    @Test
    void shouldCatchException_andProceedFurther_withValidData() {
        listAppender.start();
        logger.addAppender(listAppender);
        CaseDetails requestForInformation = caseDetailsDirectionOrder.toBuilder().data(
            Map.of("generalAppConsentOrder", "maybe")).state(AWAITING_ADDITIONAL_INFORMATION.toString()).build();

        when(caseStateSearchService.getGeneralApplications(AWAITING_ADDITIONAL_INFORMATION))
            .thenReturn(List.of(caseDetailRequestForInformation, requestForInformation));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("GAJudgeRevisitTaskHandler failed: java.lang.IllegalArgumentException: "
                         + "Cannot deserialize value of type `uk.gov.hmcts.reform.civil.enums.YesOrNo` "
                         + "from String \"maybe\": not one of the values accepted for Enum class: [No, Yes]\n"
                         + " at [Source: UNKNOWN; byte offset: #UNKNOWN] "
                         + "(through reference chain: "
                         + "uk.gov.hmcts.reform.civil.model.CaseData[\"generalAppConsentOrder\"])",
                     logsList.get(2).getMessage());
        assertEquals(Level.ERROR, logsList.get(2).getLevel());

        verify(caseStateSearchService).getGeneralApplications(AWAITING_ADDITIONAL_INFORMATION);
        verify(coreCaseDataService, times(1)).triggerEvent(any(), any());
        verify(coreCaseDataService).triggerEvent(4L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
        listAppender.stop();
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
        when(caseStateSearchService.getGeneralApplications(AWAITING_DIRECTIONS_ORDER_DOCS)).thenReturn(List.of());

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_DIRECTIONS_ORDER_DOCS);
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenDirectionOrderDateIsToday() {
        when(caseStateSearchService.getGeneralApplications(AWAITING_DIRECTIONS_ORDER_DOCS))
            .thenReturn(List.of(caseDetailsDirectionOrder));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_DIRECTIONS_ORDER_DOCS);
        verify(coreCaseDataService).triggerEvent(1L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenDirectionOrderDateIsPast() {

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
    void shouldNotEmitBusinessProcessEvent_whenDirectionOrderDateIsFuture() {

        CaseDetails caseDetailsDirectionOrderWithPastDate = caseDetailsDirectionOrder.toBuilder().data(
            Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                .directionsText("Test Direction")
                .reasonForDecisionText("Test Reason")
                .makeAnOrder(GIVE_DIRECTIONS_WITHOUT_HEARING)
                .directionsResponseByDate(LocalDate.now().plusDays(2))
                .build())).state(AWAITING_DIRECTIONS_ORDER_DOCS.toString()).build();

        when(caseStateSearchService.getGeneralApplications(AWAITING_DIRECTIONS_ORDER_DOCS))
            .thenReturn(List.of(caseDetailsDirectionOrderWithPastDate));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_DIRECTIONS_ORDER_DOCS);
        verify(coreCaseDataService, times(0)).triggerEvent(1L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenWrittenRepConcurrentDateIsToday() {
        when(caseStateSearchService.getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS))
            .thenReturn(List.of(caseDetailsWrittenRepresentationC));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS);
        verify(coreCaseDataService).triggerEvent(2L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenWrittenRepConcurrentDateIsPast() {

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
    void shouldNotEmitBusinessProcessEvent_whenWrittenRepConcurrentDateIsFuture() {

        CaseDetails caseDetailsWrittenRepresentationConWithPastDate = caseDetailsWrittenRepresentationC.toBuilder().data(
            Map.of("judicialDecisionMakeAnOrderForWrittenRepresentations", GAJudicialWrittenRepresentations.builder()
                .writtenOption(CONCURRENT_REPRESENTATIONS)
                .writtenConcurrentRepresentationsBy(LocalDate.now().plusDays(1))
                .build())).state(AWAITING_WRITTEN_REPRESENTATIONS.toString()).build();

        when(caseStateSearchService.getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS))
            .thenReturn(List.of(caseDetailsWrittenRepresentationConWithPastDate));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS);
        verify(coreCaseDataService, times(0)).triggerEvent(2L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenWrittenRepSequentialDateIsToday() {
        when(caseStateSearchService.getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS))
            .thenReturn(List.of(caseDetailsWrittenRepresentationS));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS);
        verify(coreCaseDataService)
            .triggerEvent(3L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenWrittenRepSequentialDateIsPast() {

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
    void shouldNotEmitBusinessProcessEvent_whenWrittenRepSequentialDateIsFuture() {

        CaseDetails caseDetailsWrittenRepresentationSeqWithPastDate = caseDetailsWrittenRepresentationS.toBuilder().data(
            Map.of("judicialDecisionMakeAnOrderForWrittenRepresentations", GAJudicialWrittenRepresentations.builder()
                .writtenOption(SEQUENTIAL_REPRESENTATIONS)
                .sequentialApplicantMustRespondWithin(LocalDate.now().plusDays(1))
                .build())).state(AWAITING_WRITTEN_REPRESENTATIONS.toString()).build();

        when(caseStateSearchService.getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS))
            .thenReturn(List.of(caseDetailsWrittenRepresentationSeqWithPastDate));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS);
        verify(coreCaseDataService, times(0))
            .triggerEvent(3L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenRequestForInformationDateIsToday() {
        when(caseStateSearchService.getGeneralApplications(AWAITING_ADDITIONAL_INFORMATION))
            .thenReturn(List.of(caseDetailRequestForInformation));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_ADDITIONAL_INFORMATION);
        verify(coreCaseDataService).triggerEvent(4L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenRequestForInformationDateIsPast() {

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
    void shouldNotEmitBusinessProcessEvent_whenRequestForInformationDateIsFuture() {

        CaseDetails caseDetailRequestForInformationWithPastDate = caseDetailRequestForInformation.toBuilder().data(
            Map.of("judicialDecision", GAJudicialDecision.builder().decision(REQUEST_MORE_INFO).build(),
                   "judicialDecisionRequestMoreInfo", GAJudicialRequestMoreInfo.builder()
                       .requestMoreInfoOption(REQUEST_MORE_INFORMATION)
                       .judgeRequestMoreInfoByDate(LocalDate.now().plusDays(1))
                       .judgeRequestMoreInfoText("test").build()
            )).state(AWAITING_ADDITIONAL_INFORMATION.toString()).build();

        when(caseStateSearchService.getGeneralApplications(AWAITING_ADDITIONAL_INFORMATION))
            .thenReturn(List.of(caseDetailRequestForInformationWithPastDate));

        gaJudgeRevisitTaskHandler.execute(externalTask, externalTaskService);

        verify(caseStateSearchService).getGeneralApplications(AWAITING_ADDITIONAL_INFORMATION);
        verify(coreCaseDataService, times(0)).triggerEvent(4L, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void getMaxAttemptsShouldAlwaysReturn1() {
        assertThat(gaJudgeRevisitTaskHandler.getMaxAttempts()).isEqualTo(1);
    }
}
