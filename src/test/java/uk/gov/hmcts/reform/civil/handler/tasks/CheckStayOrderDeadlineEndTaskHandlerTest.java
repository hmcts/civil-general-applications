package uk.gov.hmcts.reform.civil.handler.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.search.OrderMadeSearchService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.RELIEF_FROM_SANCTIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STAY_THE_CLAIM;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    CheckStayOrderDeadlineEndTaskHandler.class})
class CheckStayOrderDeadlineEndTaskHandlerTest {

    @MockBean
    private ExternalTask externalTask;

    @MockBean
    private ExternalTaskService externalTaskService;

    @MockBean
    private OrderMadeSearchService searchService;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private CheckStayOrderDeadlineEndTaskHandler gaOrderMadeTaskHandler;

    @Autowired
    private ObjectMapper mapper;

    private CaseDetails caseDetailsWithTodayDeadlineNotProcessed;
    private CaseDetails caseDetailsWithTodayDeadlineProcessed;
    private CaseDetails caseDetailsWithTodayDeadlineReliefFromSanctionOrder;
    private CaseDetails caseDetailsWithDeadlineCrossedNotProcessed;
    private CaseDetails caseDetailsWithDeadlineCrossedProcessed;

    private CaseDetails caseDetailsWithNoDeadline;
    private CaseDetails caseDetailsWithFutureDeadline;
    private CaseData caseDataWithDeadlineCrossedNotProcessed;
    private CaseData caseDataWithTodayDeadlineNotProcessed;
    private CaseData caseDataWithTodayDeadlineProcessed;
    private CaseData caseDataWithTodayDeadlineReliefFromSanctionOrder;
    private CaseData caseDataWithDeadlineCrossedProcessed;
    private CaseData caseDataWithNoDeadline;
    private CaseData caseDataWithFutureDeadline;

    private final LocalDate deadlineCrossed = LocalDate.now().minusDays(2);
    private final LocalDate deadlineInFuture = LocalDate.now().plusDays(2);
    private final LocalDate deadLineToday = LocalDate.now();

    @BeforeEach
    void init() {
        caseDetailsWithTodayDeadlineNotProcessed = getCaseDetails(1L, STAY_THE_CLAIM, deadLineToday,
                                                                  YesOrNo.NO);
        caseDataWithTodayDeadlineNotProcessed = getCaseData(1L, STAY_THE_CLAIM, deadLineToday,
                                                            YesOrNo.NO);

        caseDetailsWithTodayDeadlineProcessed = getCaseDetails(1L, STAY_THE_CLAIM, deadLineToday,
                                                               YesOrNo.YES);
        caseDataWithTodayDeadlineProcessed = getCaseData(1L, STAY_THE_CLAIM, deadLineToday,
                                                         YesOrNo.YES);

        caseDetailsWithTodayDeadlineReliefFromSanctionOrder = getCaseDetails(2L, RELIEF_FROM_SANCTIONS,
                                                                             deadLineToday, YesOrNo.NO);
        caseDataWithTodayDeadlineReliefFromSanctionOrder = getCaseData(2L, RELIEF_FROM_SANCTIONS,
                                                            deadLineToday, YesOrNo.NO);

        caseDetailsWithDeadlineCrossedNotProcessed = getCaseDetails(3L, STAY_THE_CLAIM,
                                                                    deadlineCrossed, YesOrNo.NO);
        caseDataWithDeadlineCrossedNotProcessed = getCaseData(3L, STAY_THE_CLAIM, deadlineCrossed,
                                                              YesOrNo.NO);

        caseDetailsWithDeadlineCrossedProcessed = getCaseDetails(4L, STAY_THE_CLAIM, deadlineCrossed,
                                                                 YesOrNo.YES);
        caseDataWithDeadlineCrossedProcessed = getCaseData(4L, STAY_THE_CLAIM, deadlineCrossed,
                                                           YesOrNo.YES);

        caseDetailsWithNoDeadline = getCaseDetails(5L, STAY_THE_CLAIM,
                                                   null, YesOrNo.NO);
        caseDataWithNoDeadline = getCaseData(5L, STAY_THE_CLAIM,
                                                            null, YesOrNo.NO);

        caseDetailsWithFutureDeadline = getCaseDetails(6L, STAY_THE_CLAIM,
                                                       deadlineInFuture, YesOrNo.NO);
        caseDataWithFutureDeadline = getCaseData(6L, STAY_THE_CLAIM,
                                                 deadlineInFuture, YesOrNo.NO);
    }

    @Test
    void shouldNotSendMessageAndTriggerGaEvent_whenZeroCasesFound() {
        when(searchService.getGeneralApplications()).thenReturn(List.of());

        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldNotSendMessageAndTriggerGaEvent_whenCasesPastDeadlineFoundAndDifferentAppType() {
        when(searchService.getGeneralApplications()).thenReturn(List.of(
            caseDetailsWithTodayDeadlineReliefFromSanctionOrder,
            caseDetailsWithDeadlineCrossedProcessed
        ));
        when(caseDetailsConverter.toCaseData(caseDetailsWithTodayDeadlineReliefFromSanctionOrder))
            .thenReturn(caseDataWithTodayDeadlineReliefFromSanctionOrder);
        when(caseDetailsConverter.toCaseData(caseDetailsWithDeadlineCrossedProcessed))
            .thenReturn(caseDataWithDeadlineCrossedProcessed);
        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldNotSendMessageAndTriggerGaEvent_whenCasesHaveFutureDeadLine() {
        when(searchService.getGeneralApplications()).thenReturn(List.of(
            caseDetailsWithTodayDeadlineReliefFromSanctionOrder,
            caseDetailsWithFutureDeadline
        ));

        when(caseDetailsConverter.toCaseData(caseDetailsWithTodayDeadlineReliefFromSanctionOrder))
            .thenReturn(caseDataWithTodayDeadlineReliefFromSanctionOrder);
        when(caseDetailsConverter.toCaseData(caseDetailsWithFutureDeadline))
            .thenReturn(caseDataWithFutureDeadline);

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

        when(caseDetailsConverter.toCaseData(caseDetailsWithTodayDeadlineNotProcessed))
            .thenReturn(caseDataWithTodayDeadlineNotProcessed);
        when(caseDetailsConverter.toCaseData(caseDetailsWithTodayDeadlineReliefFromSanctionOrder))
            .thenReturn(caseDataWithTodayDeadlineReliefFromSanctionOrder);

        when(caseDetailsConverter.toCaseData(caseDetailsWithDeadlineCrossedNotProcessed))
            .thenReturn(caseDataWithDeadlineCrossedNotProcessed);
        when(caseDetailsConverter.toCaseData(caseDetailsWithTodayDeadlineProcessed))
            .thenReturn(caseDataWithTodayDeadlineProcessed);

        when(caseDetailsConverter.toCaseData(caseDetailsWithFutureDeadline))
            .thenReturn(caseDataWithFutureDeadline);
        when(caseDetailsConverter.toCaseData(caseDetailsWithNoDeadline))
            .thenReturn(caseDataWithNoDeadline);

        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verify(coreCaseDataService).triggerGaEvent(1L, END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE,
                                                   getCaseData(1L, STAY_THE_CLAIM, deadLineToday,
                                                               YesOrNo.YES).toMap(mapper));
        verify(coreCaseDataService).triggerGaEvent(3L, END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE,
                                                   getCaseData(3L, STAY_THE_CLAIM, deadlineCrossed,
                                                               YesOrNo.YES).toMap(mapper));
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCasesFoundWithNullDeadlineDate() {
        when(searchService.getGeneralApplications()).thenReturn(List.of(caseDetailsWithNoDeadline));

        when(caseDetailsConverter.toCaseData(caseDetailsWithNoDeadline))
            .thenReturn(caseDataWithNoDeadline);

        gaOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getGeneralApplications();
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    private CaseData getCaseData(Long ccdId, GeneralApplicationTypes generalApplicationType,
                                 LocalDate deadline, YesOrNo esProcessed) {
        return CaseDataBuilder.builder()
            .ccdCaseReference(ccdId)
            .ccdState(ORDER_MADE)
            .generalAppType(GAApplicationType.builder().types(List.of(generalApplicationType)).build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .makeAnOrder(APPROVE_OR_EDIT)
                                           .judgeRecitalText("Sample Text")
                                           .judgeApproveEditOptionDate(deadline)
                                           .reasonForDecisionText("Sample Test")
                                           .esOrderProcessedByStayScheduler(esProcessed)
                                           .build()).build();
    }

    private CaseDetails getCaseDetails(Long ccdId, GeneralApplicationTypes generalApplicationType,
                                 LocalDate deadline, YesOrNo esProcessed) {
        return CaseDetails.builder().id(ccdId).data(
                Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                           .makeAnOrder(APPROVE_OR_EDIT)
                           .judgeRecitalText("Sample Text")
                           .judgeApproveEditOptionDate(deadline)
                           .reasonForDecisionText("Sample Test")
                           .esOrderProcessedByStayScheduler(esProcessed)
                           .build(),
                       "generalAppType", GAApplicationType.builder().types(List.of(generalApplicationType)).build()))
            .state(ORDER_MADE.toString()).build();
    }
}
