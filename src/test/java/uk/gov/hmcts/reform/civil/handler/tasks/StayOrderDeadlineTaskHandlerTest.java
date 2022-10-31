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
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.search.OrderMadeSearchService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_SCHEDULER_DEADLINE_STAY_ORDER;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.RELIEF_FROM_SANCTIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STAY_THE_CLAIM;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    StayOrderDeadlineTaskHandler.class})
public class StayOrderDeadlineTaskHandlerTest {

    @MockBean
    private ExternalTask externalTask;

    @MockBean
    private ExternalTaskService externalTaskService;

    @MockBean
    private OrderMadeSearchService searchService;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private StayOrderDeadlineTaskHandler gaOrderMadeTaskHandler;

    private CaseDetails caseDetailsWithTodayDeadlineNotProcessed;
    private CaseDetails caseDetailsWithTodayDeadlineProcessed;
    private CaseDetails caseDetailsWithTodayDeadlineReliefFromSanctionOrder;
    private CaseDetails caseDetailsWithDeadlineCrossedNotProcessed;
    private CaseDetails caseDetailsWithDeadlineCrossedProcessed;
    private CaseDetails caseDetailsWithNoDeadline;
    private CaseDetails caseDetailsWithFutureDeadline;

    private final LocalDate deadlineCrossed = LocalDate.now().minusDays(2);
    private final LocalDate deadlineInFuture = LocalDate.now().plusDays(2);

    private final LocalDate deadLineToday = LocalDate.now();

    @BeforeEach
    void init() {
        caseDetailsWithTodayDeadlineNotProcessed = CaseDetails.builder().id(1L).data(

            Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                    .makeAnOrder(APPROVE_OR_EDIT)
                    .judgeRecitalText("Sample Text")
                    .judgeApproveEditOptionDate(deadLineToday)
                    .reasonForDecisionText("Sample Test")
                    .esOrderProcessedByStayScheduler(YesOrNo.NO)
                .build(),
            "generalAppType", GAApplicationType.builder().types(List.of(STAY_THE_CLAIM)).build()))
            .state(ORDER_MADE.toString()).build();
        caseDetailsWithTodayDeadlineProcessed = CaseDetails.builder().id(1L).data(

                Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                           .makeAnOrder(APPROVE_OR_EDIT)
                           .judgeRecitalText("Sample Text")
                           .judgeApproveEditOptionDate(deadLineToday)
                           .reasonForDecisionText("Sample Test")
                           .esOrderProcessedByStayScheduler(YesOrNo.YES)
                           .build(),
                       "generalAppType", GAApplicationType.builder().types(List.of(STAY_THE_CLAIM)).build()))
            .state(ORDER_MADE.toString()).build();
        caseDetailsWithTodayDeadlineReliefFromSanctionOrder = CaseDetails.builder().id(2L).data(
            Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                .makeAnOrder(APPROVE_OR_EDIT)
                .judgeRecitalText("Sample Text")
                .judgeApproveEditOptionDate(deadLineToday)
                .reasonForDecisionText("Sample Test")
                .esOrderProcessedByStayScheduler(YesOrNo.NO)
                .build(),
                   "generalAppType", GAApplicationType.builder().types(List.of(RELIEF_FROM_SANCTIONS)).build()))
            .state(ORDER_MADE.toString()).build();
        caseDetailsWithDeadlineCrossedNotProcessed = CaseDetails.builder().id(3L).data(
            Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                .makeAnOrder(APPROVE_OR_EDIT)
                .judgeRecitalText("Sample Text")
                .judgeApproveEditOptionDate(deadlineCrossed)
                .reasonForDecisionText("Sample Test")
                .esOrderProcessedByStayScheduler(YesOrNo.NO)
                .build(),
                   "generalAppType", GAApplicationType.builder().types(List.of(STAY_THE_CLAIM)).build()))
            .state(ORDER_MADE.toString()).build();
        caseDetailsWithDeadlineCrossedProcessed = CaseDetails.builder().id(4L).data(
                Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                           .makeAnOrder(APPROVE_OR_EDIT)
                           .judgeRecitalText("Sample Text")
                           .judgeApproveEditOptionDate(deadlineCrossed)
                           .reasonForDecisionText("Sample Test")
                           .esOrderProcessedByStayScheduler(YesOrNo.YES)
                           .build(),
                       "generalAppType", GAApplicationType.builder().types(List.of(STAY_THE_CLAIM)).build()))
            .state(ORDER_MADE.toString()).build();
        caseDetailsWithNoDeadline = CaseDetails.builder().id(5L).data(
                Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                           .makeAnOrder(APPROVE_OR_EDIT)
                           .judgeRecitalText("Sample Text")
                           .reasonForDecisionText("Sample Test")
                           .esOrderProcessedByStayScheduler(YesOrNo.NO)
                           .build(),
                       "generalAppType", GAApplicationType.builder().types(List.of(STAY_THE_CLAIM)).build()))
            .state(ORDER_MADE.toString()).build();
        caseDetailsWithFutureDeadline = CaseDetails.builder().id(6L).data(
                Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                           .makeAnOrder(APPROVE_OR_EDIT)
                           .judgeRecitalText("Sample Text")
                           .judgeApproveEditOptionDate(deadlineInFuture)
                           .reasonForDecisionText("Sample Test")
                           .esOrderProcessedByStayScheduler(YesOrNo.NO)
                           .build(),
                       "generalAppType", GAApplicationType.builder().types(List.of(STAY_THE_CLAIM)).build()))
            .state(ORDER_MADE.toString()).build();
    }

    @Test
    void shouldNotSendMessageAndTriggerEvent_whenZeroCasesFound() {
        when(searchService.getGeneralApplications()).thenReturn(List.of());

        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldNotSendMessageAndTriggerEvent_whenCasesPastDeadlineFoundAndDifferentAppType() {
        when(searchService.getGeneralApplications()).thenReturn(List.of(
            caseDetailsWithTodayDeadlineReliefFromSanctionOrder,
            caseDetailsWithDeadlineCrossedProcessed
        ));

        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldNotSendMessageAndTriggerEvent_whenCasesHaveFutureDeadLine() {
        when(searchService.getGeneralApplications()).thenReturn(List.of(
            caseDetailsWithTodayDeadlineReliefFromSanctionOrder,
            caseDetailsWithFutureDeadline
        ));

        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_onlyWhen_NotProcessedAndDeadlineReached() {
        when(searchService.getGeneralApplications()).thenReturn(
            List.of(caseDetailsWithTodayDeadlineNotProcessed,
                caseDetailsWithTodayDeadlineReliefFromSanctionOrder,
                caseDetailsWithDeadlineCrossedNotProcessed,
                caseDetailsWithTodayDeadlineProcessed,
                caseDetailsWithFutureDeadline,
                caseDetailsWithNoDeadline
        ));

        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verify(coreCaseDataService).triggerEvent(3L, END_SCHEDULER_DEADLINE_STAY_ORDER);
        verify(coreCaseDataService).triggerEvent(1L, END_SCHEDULER_DEADLINE_STAY_ORDER);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCasesFoundWithNullDeadlineDate() {
        when(searchService.getGeneralApplications()).thenReturn(List.of(caseDetailsWithNoDeadline));

        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

}
