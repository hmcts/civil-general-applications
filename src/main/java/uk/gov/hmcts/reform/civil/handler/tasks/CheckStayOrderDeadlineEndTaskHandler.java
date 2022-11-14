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
import uk.gov.hmcts.reform.civil.service.search.OrderMadeSearchService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.LocalDate.now;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STAY_THE_CLAIM;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${judge.revisit.event.emitter.enabled:true}")
public class CheckStayOrderDeadlineEndTaskHandler implements BaseExternalTaskHandler {

    private final OrderMadeSearchService caseSearchService;

    private final CoreCaseDataService coreCaseDataService;

    private final CaseDetailsConverter caseDetailsConverter;
    private final ObjectMapper mapper;

    @Override
    public void handleTask(ExternalTask externalTask) {
        List<CaseData> cases = getOrderMadeCasesThatAreEndingToday();
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), cases.size());

        cases.forEach(this::fireEventForStateChange);
    }

    private List<CaseData> getOrderMadeCasesThatAreEndingToday() {
        List<CaseDetails> orderMadeCases = caseSearchService.getGeneralApplications();
        return orderMadeCases.stream()
            .map(caseDetailsConverter::toCaseData)
            .filter(caseData -> caseData.getJudicialDecisionMakeOrder().getJudgeApproveEditOptionDate() != null
                && caseData.getGeneralAppType().getTypes().contains(STAY_THE_CLAIM)
                && caseData.getJudicialDecisionMakeOrder().getIsOrderProcessedByStayScheduler().equals(YesOrNo.NO)
                && (!now().isBefore(caseData.getJudicialDecisionMakeOrder().getJudgeApproveEditOptionDate()))
                )
            .collect(Collectors.toList());
    }

    private void fireEventForStateChange(CaseData caseData) {
        Long caseId = caseData.getCcdCaseReference();
        log.info("Firing event END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE to check applications with ORDER_MADE"
                     + "and with Application type Stay claim and its end date is today"
                     + "for caseId: {}", caseId);

        coreCaseDataService.triggerGaEvent(caseId, END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE,
                                           getUpdatedCaseDataMapper(updateCaseData(caseData)));
        log.info("Checking state for caseId: {}", caseId);
    }

    private CaseData updateCaseData(CaseData caseData) {
        GAJudicialMakeAnOrder judicialDecisionMakeOrder = caseData.getJudicialDecisionMakeOrder();
        caseData = caseData.toBuilder()
            .judicialDecisionMakeOrder(
                judicialDecisionMakeOrder.toBuilder().isOrderProcessedByStayScheduler(YesOrNo.YES).build())
            .build();
        return caseData;
    }

    private Map<String, Object> getUpdatedCaseDataMapper(CaseData caseData) {
        return caseData.toMap(mapper);
    }
}
