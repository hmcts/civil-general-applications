package uk.gov.hmcts.reform.civil.handler.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.search.CaseStateSearchService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.LocalDate.now;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STAY_THE_CLAIM;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.UNLESS_ORDER;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${judge.revisit.stayOrder.event.emitter.enabled:true}")
public class CheckStayOrderDeadlineEndTaskHandler implements BaseExternalTaskHandler {

    private final CaseStateSearchService caseSearchService;

    private final CoreCaseDataService coreCaseDataService;

    private final CaseDetailsConverter caseDetailsConverter;
    private final ObjectMapper mapper;

    @Override
    public void handleTask(ExternalTask externalTask) {

        List<CaseDetails> orderMadeCases = caseSearchService.getOrderMadeGeneralApplications(ORDER_MADE);

        List<CaseData> stayClaimCases = getOrderMadeCasesThatAreEndingToday_StayClaim(orderMadeCases);
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), stayClaimCases.size());

        stayClaimCases.forEach(this::fireEventForStateChange_StayClaim);

        List<CaseData> unlessOrderCases = getOrderMadeCasesThatAreEndingToday_UnlessOrder(orderMadeCases);
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), unlessOrderCases.size());

        unlessOrderCases.forEach(this::fireEventForStateChange_UnlessOrderType);
    }

    private List<CaseData> getOrderMadeCasesThatAreEndingToday_StayClaim(List<CaseDetails> orderMadeCases) {

        return orderMadeCases.stream()
            .map(caseDetailsConverter::toCaseData)
            .filter(caseData -> caseData.getJudicialDecisionMakeOrder().getJudgeApproveEditOptionDate() != null
                && caseData.getGeneralAppType().getTypes().contains(STAY_THE_CLAIM)
                && caseData.getJudicialDecisionMakeOrder().getIsOrderProcessedByStayScheduler() != null
                && caseData.getJudicialDecisionMakeOrder().getIsOrderProcessedByStayScheduler()
                .equals(YesOrNo.NO)
                && (!now().isBefore(caseData.getJudicialDecisionMakeOrder().getJudgeApproveEditOptionDate()))
            )
            .collect(Collectors.toList());
    }

    private List<CaseData> getOrderMadeCasesThatAreEndingToday_UnlessOrder(List<CaseDetails> orderMadeCases) {

        return orderMadeCases.stream()
            .map(caseDetailsConverter::toCaseData)
            .filter(caseDate -> caseDate.getJudicialDecisionMakeOrder()
                .getJudgeApproveEditOptionDateForUnlessOrder() != null
                && caseDate.getGeneralAppType().getTypes().contains(UNLESS_ORDER)
                && caseDate.getJudicialDecisionMakeOrder().getIsOrderProcessedByUnlessScheduler() != null
                && caseDate.getJudicialDecisionMakeOrder().getIsOrderProcessedByUnlessScheduler().equals(YesOrNo.NO)
                && (!now().isBefore(caseDate.getJudicialDecisionMakeOrder()
                                        .getJudgeApproveEditOptionDateForUnlessOrder()))
            )
            .collect(Collectors.toList());
    }

    private void fireEventForStateChange_StayClaim(CaseData caseData) {
        Long caseId = caseData.getCcdCaseReference();

        GAJudicialMakeAnOrder judicialDecisionMakeOrder = caseData.getJudicialDecisionMakeOrder();

        /*
         * Trigger state change event if application type without UNLESS_ORDER type
         * */

        if (!caseData.getGeneralAppType().getTypes().contains(UNLESS_ORDER)) {
            triggerGaEventForStayClaimType(caseId, caseData);
        } else {
            /*
             * Check if application is been already processed by the scheduler when application type
             * also includes UNLESS_ORDER.
             *
             * if so, don't trigger the event state changes to prevent duplication of creating WA task
             *
             * */
            if (judicialDecisionMakeOrder.getIsOrderProcessedByUnlessScheduler() != null
                && judicialDecisionMakeOrder.getIsOrderProcessedByUnlessScheduler().equals(YesOrNo.NO)) {

                triggerGaEventForStayClaimType(caseId, caseData);
            }
        }
    }

    private void triggerGaEventForStayClaimType(Long caseId, CaseData caseData) {
        log.info("Firing event END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE to check applications with ORDER_MADE"
                     + "and with Application type Stay claim and its end date is today"
                     + "for caseId: {}", caseId);

        coreCaseDataService.triggerGaEvent(caseId, END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE,
                                           getUpdatedCaseDataMapper(updateCaseData_StayClaim(caseData))
        );
        log.info("Checking state for caseId: {}", caseId);
    }

    private CaseData updateCaseData_StayClaim(CaseData caseData) {
        GAJudicialMakeAnOrder judicialDecisionMakeOrder = caseData.getJudicialDecisionMakeOrder();
        caseData = caseData.toBuilder()
            .judicialDecisionMakeOrder(
                judicialDecisionMakeOrder.toBuilder().isOrderProcessedByStayScheduler(YesOrNo.YES).build())
            .build();
        return caseData;
    }

    private void fireEventForStateChange_UnlessOrderType(CaseData caseData) {
        Long caseId = caseData.getCcdCaseReference();
        GAJudicialMakeAnOrder judicialDecisionMakeOrder = caseData.getJudicialDecisionMakeOrder();

        /*
         * Trigger state change event if application type without STAY_THE_CLAIM type
         * */

        if (!caseData.getGeneralAppType().getTypes().contains(STAY_THE_CLAIM)) {
            triggerGaEventForUnlessOrderType(caseId, caseData);
        } else {
            /*
             * Check if application is been already processed by the scheduler when application type
             * also includes STAY_THE_CLAIM.
             *
             * if so, don't trigger the event state changes to prevent duplication of creating WA task
             *
             * */
            if (judicialDecisionMakeOrder.getIsOrderProcessedByStayScheduler() != null
                && judicialDecisionMakeOrder.getIsOrderProcessedByStayScheduler().equals(YesOrNo.NO)) {

                triggerGaEventForUnlessOrderType(caseId, caseData);
            }
        }
    }

    private void triggerGaEventForUnlessOrderType(Long caseId, CaseData caseData) {
        log.info("Firing event END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE to check applications with ORDER_MADE"
                     + "and with Application type Unless Order and its end date is today"
                     + "for caseId: {}", caseId);

        coreCaseDataService.triggerGaEvent(caseId, END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE,
                                           getUpdatedCaseDataMapper(updateCaseDataForUnlessOrderType(caseData))
        );
        log.info("Checking state for caseId: {}", caseId);
    }

    private CaseData updateCaseDataForUnlessOrderType(CaseData caseData) {
        GAJudicialMakeAnOrder judicialDecisionMakeOrder = caseData.getJudicialDecisionMakeOrder();
        caseData = caseData.toBuilder()
            .judicialDecisionMakeOrder(
                judicialDecisionMakeOrder.toBuilder().isOrderProcessedByUnlessScheduler(YesOrNo.YES).build())
            .build();
        return caseData;
    }

    private Map<String, Object> getUpdatedCaseDataMapper(CaseData caseData) {
        return caseData.toMap(mapper);
    }
}
