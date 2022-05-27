package uk.gov.hmcts.reform.civil.handler.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.search.DirectionOrderSearchService;
import uk.gov.hmcts.reform.civil.service.search.RequestForInformationrSearchService;
import uk.gov.hmcts.reform.civil.service.search.WrittenRepresentationSearchService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("${response.deadline.check.event.emitter.enabled:true}")
public class GAJudgeRevisitTaskHandler implements BaseExternalTaskHandler {

    private final WrittenRepresentationSearchService writtenRepresentationSearchService;
    private final DirectionOrderSearchService directionOrderSearchService;
    private final RequestForInformationrSearchService requestForInformationrSearchService;

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

    private void fireEventForStateChange(CaseDetails caseDetails) {
        Long caseId = caseDetails.getId();
        log.info("Firing event CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION to change the state "
                     + "to APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION "
                     + "for caseId: {}", caseId);
        coreCaseDataService.triggerEvent(caseId, CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION);
    }

    private List<CaseDetails> getWrittenRepCaseReadyToJudgeRevisit() {
        List<CaseDetails> judgeReadyToRevisitWrittenRepCases = writtenRepresentationSearchService
            .getGeneralApplications();
        return judgeReadyToRevisitWrittenRepCases.stream()
            .filter(a -> (caseDetailsConverter.toCaseData(a).getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                .getWrittenOption().equals(CONCURRENT_REPRESENTATIONS))
                && LocalDate.now().isEqual(caseDetailsConverter.toCaseData(a)
                                               .getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                               .getWrittenConcurrentRepresentationsBy())
            || caseDetailsConverter.toCaseData(a).getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                .getWrittenOption().equals(SEQUENTIAL_REPRESENTATIONS)
                && LocalDate.now().isEqual(caseDetailsConverter.toCaseData(a)
                                               .getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                               .getSequentialApplicantMustRespondWithin()))
            .collect(Collectors.toList());
    }

    private List<CaseDetails> getDirectionOrderCaseReadyToJudgeRevisit() {
        List<CaseDetails> judgeReadyToRevisitDirectionOrderCases = directionOrderSearchService
            .getGeneralApplications();
        return judgeReadyToRevisitDirectionOrderCases.stream()
            .filter(a -> (caseDetailsConverter.toCaseData(a).getJudicialDecisionMakeOrder().getMakeAnOrder()
                .equals(GIVE_DIRECTIONS_WITHOUT_HEARING))
                && LocalDate.now().isEqual(caseDetailsConverter.toCaseData(a)
                                               .getJudicialDecisionMakeOrder()
                                               .getDirectionsResponseByDate()))
                .collect(Collectors.toList());
    }

    private List<CaseDetails> getRequestForInformationCaseReadyToJudgeRevisit() {
        List<CaseDetails> judgeReadyToRevisitRequestForInfoCases = requestForInformationrSearchService
            .getGeneralApplications();
        return judgeReadyToRevisitRequestForInfoCases.stream()
            .filter(a -> (caseDetailsConverter.toCaseData(a)
                .getJudicialDecisionRequestMoreInfo()
                .getRequestMoreInfoOption()
                .equals(REQUEST_MORE_INFORMATION))
                && LocalDate.now().isEqual(caseDetailsConverter.toCaseData(a)
                                               .getJudicialDecisionRequestMoreInfo()
                                               .getJudgeRequestMoreInfoByDate()))
            .collect(Collectors.toList());
    }

    @Override
    public int getMaxAttempts() {
        return 1;
    }
}
