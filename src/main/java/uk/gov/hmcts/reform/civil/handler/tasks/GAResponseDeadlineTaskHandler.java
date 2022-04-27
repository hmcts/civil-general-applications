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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${response.deadline.check.event.emitter.enabled:true}")
public class GAResponseDeadlineTaskHandler implements BaseExternalTaskHandler {

    private final AwaitingResponseStatusSearchService caseSearchService;

    private final CoreCaseDataService coreCaseDataService;

    private final CaseDetailsConverter caseDetailsConverter;

    @Override
    public void handleTask(ExternalTask externalTask) {
        List<CaseDetails> cases = getAwaitingResponseCasesThatArePastDueDate();
        log.info("Job '{}' found {} case(s)", externalTask.getTopicName(), cases.size());

        cases.forEach(this::fireEventForStateChange);
    }

    private void fireEventForStateChange(CaseDetails caseDetails) {
        Long caseId = caseDetails.getId();
        log.info("Firing event CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION to change the state from "
                     + "AWAITING_RESPONDENT_RESPONSE to APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION "
                     + "for caseId: {}", caseId);
        coreCaseDataService.triggerEvent(caseId, CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION);
    }

    private List<CaseDetails> getAwaitingResponseCasesThatArePastDueDate() {
        List<CaseDetails> awaitingResponseCases = caseSearchService.getGeneralApplications();
        return awaitingResponseCases.stream()
            .filter(a -> !isEmpty(caseDetailsConverter.toCaseData(a).getGeneralAppDeadlineNotificationDate())
                && now().isAfter(LocalDateTime.parse(
                caseDetailsConverter.toCaseData(a).getGeneralAppDeadlineNotificationDate())))
            .collect(Collectors.toList());
    }

    @Override
    public int getMaxAttempts() {
        return 1;
    }
}
