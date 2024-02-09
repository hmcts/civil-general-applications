package uk.gov.hmcts.reform.civil.handler.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.search.CaseStateSearchService;

import java.util.List;

import static java.time.LocalDateTime.now;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${response.deadline.check.event.emitter.enabled:true}")
public class GAResponseDeadlineTaskHandler implements BaseExternalTaskHandler {

    private final CaseStateSearchService caseSearchService;

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
        try {
            coreCaseDataService.triggerEvent(caseId, CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION);
        } catch (Exception e) {
            log.error("Error in GAResponseDeadlineTaskHandler::fireEventForStateChange: " + e);
        }
    }

    protected List<CaseDetails> getAwaitingResponseCasesThatArePastDueDate() {
        List<CaseDetails> awaitingResponseCases = caseSearchService
            .getGeneralApplications(AWAITING_RESPONDENT_RESPONSE);

        return awaitingResponseCases.stream()
            .filter(a -> {
                try {
                    return caseDetailsConverter.toCaseData(a).getGeneralAppNotificationDeadlineDate() != null
                        && now().isAfter(
                        caseDetailsConverter.toCaseData(a).getGeneralAppNotificationDeadlineDate());
                } catch (Exception e) {
                    log.error("GAResponseDeadlineTaskHandler failed: " + e);
                }
                return false;
            })
            .toList();
    }

    @Override
    public int getMaxAttempts() {
        return 1;
    }
}
