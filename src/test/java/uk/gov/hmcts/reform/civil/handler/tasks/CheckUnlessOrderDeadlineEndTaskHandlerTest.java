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
import uk.gov.hmcts.reform.civil.service.search.CaseStateSearchService;

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
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.UNLESS_ORDER;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    CheckStayOrderDeadlineEndTaskHandler.class})
public class CheckUnlessOrderDeadlineEndTaskHandlerTest {

    @MockBean
    private ExternalTask externalTask;

    @MockBean
    private ExternalTaskService externalTaskService;

    @MockBean
    private CaseStateSearchService searchService;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private CheckStayOrderDeadlineEndTaskHandler gaUnlessOrderMadeTaskHandler;

    @Autowired
    private ObjectMapper mapper;

    private CaseDetails caseDetailsWithTodayDeadlineNotProcessed;
    private CaseDetails caseDetailsWithTodayDeadlineProcessed;
    private CaseDetails caseDetailsWithTodayDeadlineReliefFromSanctionOrder;
    private CaseDetails caseDetailsWithDeadlineCrossedNotProcessed;
    private CaseDetails caseDetailsWithDeadlineCrossedProcessed;
    private CaseDetails caseDetailsWithTodayDeadLineWithOrderProcessedNull;

    private CaseDetails caseDetailsWithNoDeadline;
    private CaseDetails caseDetailsWithFutureDeadline;
    private CaseData caseDataWithDeadlineCrossedNotProcessed;
    private CaseData caseDataWithTodayDeadlineNotProcessed;
    private CaseData caseDataWithTodayDeadlineProcessed;
    private CaseData caseDataWithTodayDeadlineReliefFromSanctionOrder;
    private CaseData caseDataWithDeadlineCrossedProcessed;
    private CaseData caseDataWithTodayDeadLineWithOrderProcessedNull;
    private CaseData caseDataWithNoDeadline;
    private CaseData caseDataWithFutureDeadline;

    private CaseDetails caseDetailsWithProcessedForStayClaim;
    private CaseData caseDataWithProcessedForStayClaim;

    private CaseDetails caseDetailsWithNotProcessedForStayClaim;
    private CaseData caseDataWithNotProcessedForStayClaim;

    private final LocalDate deadlineCrossed = LocalDate.now().minusDays(2);
    private final LocalDate deadlineInFuture = LocalDate.now().plusDays(2);
    private final LocalDate deadLineToday = LocalDate.now();

    @BeforeEach
    void init() {
        caseDetailsWithTodayDeadlineNotProcessed = getCaseDetails(1L, List.of(UNLESS_ORDER), deadLineToday,
                                                                  YesOrNo.NO);
        caseDataWithTodayDeadlineNotProcessed = getCaseData(1L, List.of(UNLESS_ORDER), deadLineToday,
                                                            YesOrNo.NO);

        caseDetailsWithTodayDeadlineProcessed = getCaseDetails(1L, List.of(UNLESS_ORDER), deadLineToday,
                                                               YesOrNo.YES);
        caseDataWithTodayDeadlineProcessed = getCaseData(1L, List.of(UNLESS_ORDER), deadLineToday,
                                                         YesOrNo.YES);

        caseDetailsWithTodayDeadlineReliefFromSanctionOrder = getCaseDetails(2L, List.of(RELIEF_FROM_SANCTIONS),
                                                                             deadLineToday, YesOrNo.NO);
        caseDataWithTodayDeadlineReliefFromSanctionOrder = getCaseData(2L, List.of(RELIEF_FROM_SANCTIONS),
                                                                       deadLineToday, YesOrNo.NO);

        caseDetailsWithDeadlineCrossedNotProcessed = getCaseDetails(3L, List.of(UNLESS_ORDER),
                                                                    deadlineCrossed, YesOrNo.NO);
        caseDataWithDeadlineCrossedNotProcessed = getCaseData(3L, List.of(UNLESS_ORDER), deadlineCrossed,
                                                              YesOrNo.NO);

        caseDetailsWithDeadlineCrossedProcessed = getCaseDetails(4L, List.of(UNLESS_ORDER), deadlineCrossed,
                                                                 YesOrNo.YES);
        caseDataWithDeadlineCrossedProcessed = getCaseData(4L, List.of(UNLESS_ORDER), deadlineCrossed,
                                                           YesOrNo.YES);

        caseDetailsWithNoDeadline = getCaseDetails(5L, List.of(UNLESS_ORDER),
                                                   null, YesOrNo.NO);
        caseDataWithNoDeadline = getCaseData(5L, List.of(UNLESS_ORDER),
                                             null, YesOrNo.NO);

        caseDetailsWithFutureDeadline = getCaseDetails(6L, List.of(UNLESS_ORDER),
                                                       deadlineInFuture, YesOrNo.NO);
        caseDataWithFutureDeadline = getCaseData(6L, List.of(UNLESS_ORDER),
                                                 deadlineInFuture, YesOrNo.NO);
        caseDetailsWithTodayDeadLineWithOrderProcessedNull = getCaseDetails(7L, List.of(UNLESS_ORDER),
                                                                            deadLineToday, null);
        caseDataWithTodayDeadLineWithOrderProcessedNull = getCaseData(7L, List.of(UNLESS_ORDER),
                                                                      deadLineToday, null);

        caseDetailsWithTodayDeadlineNotProcessed = getCaseDetails(1L, List.of(UNLESS_ORDER), deadLineToday,
                                                                  YesOrNo.NO);
        caseDataWithTodayDeadlineNotProcessed = getCaseData(1L, List.of(UNLESS_ORDER), deadLineToday,
                                                            YesOrNo.NO);

        // General application type contains both Stay the Claim and Unless Order
        caseDetailsWithProcessedForStayClaim =
            getCaseDetailsForUnlessOrderAndStayClaim(11L, List.of(UNLESS_ORDER, STAY_THE_CLAIM), deadLineToday,
                                                     YesOrNo.NO, YesOrNo.YES);
        caseDataWithProcessedForStayClaim
            = getCaseDataForUnlessOrderAndStayClaim(11L, List.of(UNLESS_ORDER, STAY_THE_CLAIM), deadLineToday,
                                                    YesOrNo.NO, YesOrNo.YES);

        caseDetailsWithNotProcessedForStayClaim =
            getCaseDetailsForUnlessOrderAndStayClaim(12L, List.of(UNLESS_ORDER, STAY_THE_CLAIM), deadLineToday,
                                                     YesOrNo.NO, YesOrNo.NO);
        caseDataWithNotProcessedForStayClaim
            = getCaseDataForUnlessOrderAndStayClaim(12L, List.of(UNLESS_ORDER, STAY_THE_CLAIM), deadLineToday,
                                                    YesOrNo.NO, YesOrNo.NO);
    }

    @Test
    void shouldNotSendMessageAndTriggerGaEvent_whenZeroCasesFound() {
        when(searchService.getOrderMadeGeneralApplications(ORDER_MADE)).thenReturn(List.of());

        gaUnlessOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getOrderMadeGeneralApplications(ORDER_MADE);
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldNotSendMessageAndTriggerGaEvent_whenCasesPastDeadlineFoundAndDifferentAppType() {
        when(searchService.getOrderMadeGeneralApplications(ORDER_MADE)).thenReturn(List.of(
            caseDetailsWithTodayDeadlineReliefFromSanctionOrder,
            caseDetailsWithDeadlineCrossedProcessed
        ));
        when(caseDetailsConverter.toCaseData(caseDetailsWithTodayDeadlineReliefFromSanctionOrder))
            .thenReturn(caseDataWithTodayDeadlineReliefFromSanctionOrder);
        when(caseDetailsConverter.toCaseData(caseDetailsWithDeadlineCrossedProcessed))
            .thenReturn(caseDataWithDeadlineCrossedProcessed);
        gaUnlessOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getOrderMadeGeneralApplications(ORDER_MADE);
        verifyNoInteractions(coreCaseDataService);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldNotSendMessageAndTriggerGaEvent_whenCasesHaveFutureDeadLine() {
        when(searchService.getOrderMadeGeneralApplications(ORDER_MADE)).thenReturn(List.of(
            caseDetailsWithTodayDeadlineReliefFromSanctionOrder,
            caseDetailsWithFutureDeadline
        ));

        when(caseDetailsConverter.toCaseData(caseDetailsWithTodayDeadlineReliefFromSanctionOrder))
            .thenReturn(caseDataWithTodayDeadlineReliefFromSanctionOrder);
        when(caseDetailsConverter.toCaseData(caseDetailsWithFutureDeadline))
            .thenReturn(caseDataWithFutureDeadline);

        gaUnlessOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getOrderMadeGeneralApplications(ORDER_MADE);
        verifyNoInteractions(coreCaseDataService);
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldNotTriggerBusinessProcessEventWhenIsOrderProcessedIsNull() {
        when(searchService.getOrderMadeGeneralApplications(ORDER_MADE)).thenReturn(
            List.of(caseDetailsWithTodayDeadlineNotProcessed,
                    caseDetailsWithTodayDeadlineReliefFromSanctionOrder,
                    caseDetailsWithTodayDeadLineWithOrderProcessedNull));
        when(caseDetailsConverter.toCaseData(caseDetailsWithTodayDeadlineNotProcessed))
            .thenReturn(caseDataWithTodayDeadlineNotProcessed);
        when(caseDetailsConverter.toCaseData(caseDetailsWithTodayDeadlineReliefFromSanctionOrder))
            .thenReturn(caseDataWithTodayDeadlineReliefFromSanctionOrder);
        when(caseDetailsConverter.toCaseData(caseDetailsWithTodayDeadLineWithOrderProcessedNull))
            .thenReturn(caseDataWithTodayDeadLineWithOrderProcessedNull);

        gaUnlessOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getOrderMadeGeneralApplications(ORDER_MADE);
        verify(coreCaseDataService).triggerGaEvent(1L, END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE,
                                                   getCaseData(1L, List.of(UNLESS_ORDER), deadLineToday,
                                                               YesOrNo.YES).toMap(mapper));
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_onlyWhen_NotProcessedAndDeadlineReached() {
        when(searchService.getOrderMadeGeneralApplications(ORDER_MADE)).thenReturn(
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

        gaUnlessOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getOrderMadeGeneralApplications(ORDER_MADE);
        verify(coreCaseDataService).triggerGaEvent(1L, END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE,
                                                   getCaseData(1L, List.of(UNLESS_ORDER), deadLineToday,
                                                               YesOrNo.YES).toMap(mapper));
        verify(coreCaseDataService).triggerGaEvent(3L, END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE,
                                                   getCaseData(3L, List.of(UNLESS_ORDER), deadlineCrossed,
                                                               YesOrNo.YES).toMap(mapper));
        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);

    }

    @Test
    void shouldEmitBusinessProcessEvent_whenCasesFoundWithNullDeadlineDate() {
        when(searchService.getOrderMadeGeneralApplications(ORDER_MADE))
            .thenReturn(List.of(caseDetailsWithNoDeadline));

        when(caseDetailsConverter.toCaseData(caseDetailsWithNoDeadline))
            .thenReturn(caseDataWithNoDeadline);

        gaUnlessOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getOrderMadeGeneralApplications(ORDER_MADE);
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldNotEmitBusinessProcessEvent_whenItsAlreadyProcessedForStayTheClaim() {
        when(searchService.getOrderMadeGeneralApplications(ORDER_MADE))
            .thenReturn(List.of(caseDetailsWithProcessedForStayClaim));

        when(caseDetailsConverter.toCaseData(caseDetailsWithProcessedForStayClaim))
            .thenReturn(caseDataWithProcessedForStayClaim);

        gaUnlessOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getOrderMadeGeneralApplications(ORDER_MADE);
        verifyNoInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    @Test
    void shouldEmitBusinessProcessEvent_whenItsAlreadyProcessedForStayTheClaim() {
        when(searchService.getOrderMadeGeneralApplications(ORDER_MADE))
            .thenReturn(List.of(caseDetailsWithNotProcessedForStayClaim,
                                caseDetailsWithTodayDeadlineNotProcessed));

        when(caseDetailsConverter.toCaseData(caseDetailsWithNotProcessedForStayClaim))
            .thenReturn(caseDataWithNotProcessedForStayClaim);

        when(caseDetailsConverter.toCaseData(caseDetailsWithTodayDeadlineNotProcessed))
            .thenReturn(caseDataWithTodayDeadlineNotProcessed);

        gaUnlessOrderMadeTaskHandler.execute(externalTask, externalTaskService);

        verify(searchService).getOrderMadeGeneralApplications(ORDER_MADE);

        verify(coreCaseDataService)
            .triggerGaEvent(12L, END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE,
                            getCaseDataForUnlessOrderAndStayClaim(12L,
                                                                  List.of(UNLESS_ORDER, STAY_THE_CLAIM),
                                                                  deadLineToday,
                                                                  YesOrNo.YES, YesOrNo.NO).toMap(mapper));

        verify(coreCaseDataService)
            .triggerGaEvent(1L, END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE,
                            getCaseDataForUnlessOrderAndStayClaim(1L,
                                                                  List.of(UNLESS_ORDER),
                                                                  deadLineToday,
                                                                  YesOrNo.YES, null).toMap(mapper));

        verifyNoMoreInteractions(coreCaseDataService);
        verify(externalTaskService).complete(externalTask);
    }

    private CaseData getCaseData(Long ccdId, List<GeneralApplicationTypes> generalApplicationType,
                                 LocalDate deadline, YesOrNo isProcessed) {
        return CaseDataBuilder.builder()
            .ccdCaseReference(ccdId)
            .ccdState(ORDER_MADE)
            .generalAppType(GAApplicationType.builder().types(generalApplicationType).build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .makeAnOrder(APPROVE_OR_EDIT)
                                           .judgeRecitalText("Sample Text")
                                           .judgeApproveEditOptionDateForUnlessOrder(deadline)
                                           .reasonForDecisionText("Sample Test")
                                           .isOrderProcessedByUnlessScheduler(isProcessed)
                                           .build()).build();
    }

    private CaseDetails getCaseDetails(Long ccdId, List<GeneralApplicationTypes> generalApplicationType,
                                       LocalDate deadline, YesOrNo isProcessed) {
        return CaseDetails.builder().id(ccdId).data(
                Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                           .makeAnOrder(APPROVE_OR_EDIT)
                           .judgeRecitalText("Sample Text")
                           .judgeApproveEditOptionDateForUnlessOrder(deadline)
                           .reasonForDecisionText("Sample Test")
                           .isOrderProcessedByUnlessScheduler(isProcessed)
                           .build(),
                       "generalAppType", GAApplicationType.builder().types(generalApplicationType).build()))
            .state(ORDER_MADE.toString()).build();
    }

    private CaseData getCaseDataForUnlessOrderAndStayClaim(Long ccdId,
                                                           List<GeneralApplicationTypes> generalApplicationType,
                                                           LocalDate deadline, YesOrNo isUnlessProcessed,
                                                           YesOrNo isStayClaimProcessed) {
        return CaseDataBuilder.builder()
            .ccdCaseReference(ccdId)
            .ccdState(ORDER_MADE)
            .generalAppType(GAApplicationType.builder().types(generalApplicationType).build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .makeAnOrder(APPROVE_OR_EDIT)
                                           .judgeRecitalText("Sample Text")
                                           .judgeApproveEditOptionDateForUnlessOrder(deadline)
                                           .reasonForDecisionText("Sample Test")
                                           .isOrderProcessedByUnlessScheduler(isUnlessProcessed)
                                           .isOrderProcessedByStayScheduler(isStayClaimProcessed)
                                           .build()).build();
    }

    private CaseDetails getCaseDetailsForUnlessOrderAndStayClaim(Long ccdId,
                                                                 List<GeneralApplicationTypes> generalApplicationType,
                                                                 LocalDate deadline, YesOrNo isUnlessProcessed,
                                                                 YesOrNo isStayClaimProcessed) {
        return CaseDetails.builder().id(ccdId).data(
                Map.of("judicialDecisionMakeOrder", GAJudicialMakeAnOrder.builder()
                           .makeAnOrder(APPROVE_OR_EDIT)
                           .judgeRecitalText("Sample Text")
                           .judgeApproveEditOptionDateForUnlessOrder(deadline)
                           .reasonForDecisionText("Sample Test")
                           .isOrderProcessedByStayScheduler(isStayClaimProcessed)
                           .isOrderProcessedByUnlessScheduler(isUnlessProcessed)
                           .build(),
                       "generalAppType", GAApplicationType.builder().types(generalApplicationType).build()))
            .state(ORDER_MADE.toString()).build();
    }
}
