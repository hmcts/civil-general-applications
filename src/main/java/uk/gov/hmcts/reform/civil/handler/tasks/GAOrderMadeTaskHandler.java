package uk.gov.hmcts.reform.civil.handler.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.search.AwaitingResponseStatusSearchService;

import java.util.List;
import java.util.stream.Collectors;

import static java.time.LocalDate.now;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CHECK_ORDER_MADE_END_DATE;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STAY_THE_CLAIM;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${response.deadline.check.event.emitter.enabled:true}")
public class GAOrderMadeTaskHandler implements BaseExternalTaskHandler {

    private final AwaitingResponseStatusSearchService caseSearchService;

    private final CoreCaseDataService coreCaseDataService;

    private final CaseDetailsConverter caseDetailsConverter;

    @Override
    public void handleTask(ExternalTask externalTask) {
        List<CaseDetails> cases = getOrderMadeCasesThatAreEndingToday();
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), cases.size());

        cases.forEach(this::fireEventForStateChange);
    }
    private List<CaseDetails> getOrderMadeCasesThatAreEndingToday() {
        List<CaseDetails> orderMadeCases = caseSearchService.getGeneralApplications();
        return orderMadeCases.stream()
            .filter(a -> caseDetailsConverter.toCaseData(a).getJudicialDecisionMakeOrder()
                .getJudgeApproveEditOptionDate() != null
                && caseDetailsConverter.toCaseData(a).getGeneralAppType()
                .getTypes().contains(STAY_THE_CLAIM)
                && now().isEqual(
                caseDetailsConverter.toCaseData(a).getJudicialDecisionMakeOrder()
                    .getJudgeApproveEditOptionDate()))
            .collect(Collectors.toList());
    }
    private void fireEventForStateChange(CaseDetails caseDetails) {
        Long caseId = caseDetails.getId();
        log.info("Firing event CHECK_ORDER_MADE_END_DATE to check applications with ORDER_MADE"
                     + "and with Application type Stay claim andd its end date is today"
                     + "for caseId: {}", caseId);

        coreCaseDataService.triggerEvent(caseId, CHECK_ORDER_MADE_END_DATE);
    }
}
