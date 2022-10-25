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
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CHECK_ORDER_MADE_END_DATE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.RELIEF_FROM_SANCTIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STAY_THE_CLAIM;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    GAOrderMadeTaskHandler.class})
public class GAOrderMadeTaskHandlerTest {

    @MockBean
    private ExternalTask externalTask;

    @MockBean
    private ExternalTaskService externalTaskService;

    @MockBean
    private OrderMadeSearchService searchService;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private GAOrderMadeTaskHandler gaOrderMadeTaskHandler;

    private CaseDetails caseDetails1;
    private CaseDetails caseDetails2;
    private CaseDetails caseDetails3;
    private CaseDetails caseDetails4;
    private CaseDetails caseDetails5;

    private final LocalDate deadlineCrossed = LocalDate.now().minusDays(2);
    private final LocalDate deadlineInFuture = LocalDate.now().plusDays(2);

    private final LocalDate deadLineToday = LocalDate.now();

    @BeforeEach
    void init() {
        caseDetails1 = CaseDetails.builder().id(1L).data(

            Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                    .makeAnOrder(APPROVE_OR_EDIT)
                    .judgeRecitalText("Sample Text")
                    .judgeApproveEditOptionDate(deadLineToday)
                    .reasonForDecisionText("Sample Test")
                .build(),
            "generalAppType", GAApplicationType.builder().types(List.of(STAY_THE_CLAIM)).build()))
            .state(ORDER_MADE.toString()).build();
        caseDetails2 = CaseDetails.builder().id(2L).data(
            Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                .makeAnOrder(APPROVE_OR_EDIT)
                .judgeRecitalText("Sample Text")
                .judgeApproveEditOptionDate(deadLineToday)
                .reasonForDecisionText("Sample Test")
                .build(),
                   "generalAppType", GAApplicationType.builder().types(List.of(RELIEF_FROM_SANCTIONS)).build()))
            .state(ORDER_MADE.toString()).build();
        caseDetails3 = CaseDetails.builder().id(3L).data(
            Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                .makeAnOrder(APPROVE_OR_EDIT)
                .judgeRecitalText("Sample Text")
                .judgeApproveEditOptionDate(deadlineCrossed)
                .reasonForDecisionText("Sample Test")
                .build(),
                   "generalAppType", GAApplicationType.builder().types(List.of(STAY_THE_CLAIM)).build()))
            .state(ORDER_MADE.toString()).build();
        caseDetails4 = CaseDetails.builder().id(4L).data(
                Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                           .makeAnOrder(APPROVE_OR_EDIT)
                           .judgeRecitalText("Sample Text")
                           .reasonForDecisionText("Sample Test")
                           .build(),
                       "generalAppType", GAApplicationType.builder().types(List.of(STAY_THE_CLAIM)).build()))
            .state(ORDER_MADE.toString()).build();
        caseDetails5 = CaseDetails.builder().id(5L).data(
                Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                           .makeAnOrder(APPROVE_OR_EDIT)
                           .judgeRecitalText("Sample Text")
                           .judgeApproveEditOptionDate(deadlineInFuture)
                           .reasonForDecisionText("Sample Test")
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
        when(searchService.getGeneralApplications()).thenReturn(List.of(caseDetails2, caseDetails3));

        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldNotSendMessageAndTriggerEvent_whenCasesHaveFutureDeadLine() {
        when(searchService.getGeneralApplications()).thenReturn(List.of(caseDetails2, caseDetails3));

        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenDifferentCases() {
        when(searchService.getGeneralApplications()).thenReturn(List.of(caseDetails1, caseDetails2,
                                                                        caseDetails3, caseDetails4));

        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verify(coreCaseDataService).triggerEvent(1L, CHECK_ORDER_MADE_END_DATE);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCasesEqualDeadlineFound() {
        when(searchService.getGeneralApplications()).thenReturn(List.of(caseDetails1));

        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verify(coreCaseDataService).triggerEvent(1L, CHECK_ORDER_MADE_END_DATE);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCasesFoundWithNullDeadlineDate() {
        when(searchService.getGeneralApplications()).thenReturn(List.of(caseDetails4));

        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

}
