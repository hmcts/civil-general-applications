package uk.gov.hmcts.reform.civil.handler.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.search.OrderMadeSearchService;

import java.util.List;
import java.util.stream.Collectors;

import static java.time.LocalDate.now;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STAY_THE_CLAIM;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${response.deadline.check.event.emitter.enabled:true}")
public class CheckStayOrderDeadlineEndTaskHandler implements BaseExternalTaskHandler {

    private final OrderMadeSearchService caseSearchService;

    private final CoreCaseDataService coreCaseDataService;

    private final CaseDetailsConverter caseDetailsConverter;

    @Override
    public void handleTask(ExternalTask externalTask) {
        List<CaseData> cases = getOrderMadeCasesThatAreEndingToday();
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), cases.size());

        cases.forEach(this::fireEventForStateChange);
    }

    private List<CaseData> getOrderMadeCasesThatAreEndingToday() {
        List<CaseDetails> orderMadeCases = caseSearchService.getGeneralApplications();
        return orderMadeCases.stream()
            .map(a -> caseDetailsConverter.toCaseData(a))
            .filter(caseData -> caseData.getJudicialDecisionMakeOrder().getJudgeApproveEditOptionDate() != null
                && caseData.getGeneralAppType().getTypes().contains(STAY_THE_CLAIM)
                && caseData.getJudicialDecisionMakeOrder().getEsOrderProcessedByStayScheduler().equals(YesOrNo.NO)
                && (!now().isBefore(caseData.getJudicialDecisionMakeOrder().getJudgeApproveEditOptionDate()))
                )
            .collect(Collectors.toList());
    }

    private void fireEventForStateChange(CaseData caseData) {
        Long caseId = caseData.getCcdCaseReference();
        log.info("Firing event CHECK_STAY_ORDER_END_DATE to check applications with ORDER_MADE"
                     + "and with Application type Stay claim and its end date is today"
                     + "for caseId: {}", caseId);

        coreCaseDataService.triggerEvent(caseId, END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE);
    }
}
