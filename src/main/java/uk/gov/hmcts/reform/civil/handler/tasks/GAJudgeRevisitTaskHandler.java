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

import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_ADDITIONAL_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_DIRECTIONS_ORDER_DOCS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${judge.revisit.check.event.emitter.enabled:true}")
public class GAJudgeRevisitTaskHandler implements BaseExternalTaskHandler {

    private final CaseStateSearchService caseStateSearchService;

    private final CoreCaseDataService coreCaseDataService;

    private final CaseDetailsConverter caseDetailsConverter;

    @Override
    public void handleTask(ExternalTask externalTask) {
        List<CaseDetails> writtenRepresentationCases = getWrittenRepCaseReadyToJudgeRevisit();
        log.info("Job '{}' found {} written representation case(s)",
                 externalTask.getTopicName(), writtenRepresentationCases.size());
        writtenRepresentationCases.forEach(this::fireEventForStateChange);

        List<CaseDetails> directionOrderCases = getDirectionOrderCaseReadyToJudgeRevisit();
        log.info("Job '{}' found {} direction order case(s)",
                 externalTask.getTopicName(), directionOrderCases.size());
        directionOrderCases.forEach(this::fireEventForStateChange);

        List<CaseDetails> requestForInformationCases = getRequestForInformationCaseReadyToJudgeRevisit();
        log.info("Job '{}' found {} request for information case(s)",
                 externalTask.getTopicName(), requestForInformationCases.size());
        requestForInformationCases.forEach(this::fireEventForStateChange);
    }

    protected void fireEventForStateChange(CaseDetails caseDetails) {
        Long caseId = caseDetails.getId();
        log.info("Firing event CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION to change the state "
                     + "to APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION "
                     + "for caseId: {}", caseId);
        try {
            coreCaseDataService.triggerEvent(caseId, CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED);
        } catch (Exception exception) {
            log.error("Error in GAJudgeRevisitTaskHandler::fireEventForStateChange: " + exception);
        }
    }

    private List<CaseDetails> getWrittenRepCaseReadyToJudgeRevisit() {
        List<CaseDetails> judgeReadyToRevisitWrittenRepCases = caseStateSearchService
            .getGeneralApplications(AWAITING_WRITTEN_REPRESENTATIONS);

        return judgeReadyToRevisitWrittenRepCases.stream()
            .filter(a -> (caseDetailsConverter.toCaseData(a).getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                .getWrittenOption().equals(CONCURRENT_REPRESENTATIONS))
                && (LocalDate.now().compareTo(caseDetailsConverter.toCaseData(a)
                                               .getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                               .getWrittenConcurrentRepresentationsBy()) >= 0)
            || caseDetailsConverter.toCaseData(a).getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                .getWrittenOption().equals(SEQUENTIAL_REPRESENTATIONS)
                && (LocalDate.now().compareTo(caseDetailsConverter.toCaseData(a)
                                                .getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                                .getSequentialApplicantMustRespondWithin()) >= 0))
            .toList();
    }

    private List<CaseDetails> getDirectionOrderCaseReadyToJudgeRevisit() {
        List<CaseDetails> judgeReadyToRevisitDirectionOrderCases = caseStateSearchService
            .getGeneralApplications(AWAITING_DIRECTIONS_ORDER_DOCS);

        return judgeReadyToRevisitDirectionOrderCases.stream()
            .filter(a -> (caseDetailsConverter.toCaseData(a).getJudicialDecisionMakeOrder().getMakeAnOrder()
                .equals(GIVE_DIRECTIONS_WITHOUT_HEARING))
                && (LocalDate.now().compareTo(caseDetailsConverter.toCaseData(a)
                                                .getJudicialDecisionMakeOrder()
                                                .getDirectionsResponseByDate()) >= 0))
                .toList();
    }

    protected List<CaseDetails> getRequestForInformationCaseReadyToJudgeRevisit() {
        List<CaseDetails> judgeReadyToRevisitRequestForInfoCases = caseStateSearchService
            .getGeneralApplications(AWAITING_ADDITIONAL_INFORMATION);

        return judgeReadyToRevisitRequestForInfoCases.stream()
            .filter(a -> {
                try {
                    return caseDetailsConverter.toCaseData(a)
                        .getJudicialDecisionRequestMoreInfo()
                        .getJudgeRequestMoreInfoByDate() != null
                        && (LocalDate.now().compareTo(caseDetailsConverter.toCaseData(a)
                                                          .getJudicialDecisionRequestMoreInfo()
                                                          .getJudgeRequestMoreInfoByDate()) >= 0);
                } catch (Exception e) {
                    log.error("GAJudgeRevisitTaskHandler failed: " + e);
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
