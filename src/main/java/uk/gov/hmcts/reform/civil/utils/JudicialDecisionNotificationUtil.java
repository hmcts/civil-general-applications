package uk.gov.hmcts.reform.civil.utils;

import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.APPLICANT_WRITTEN_REP_SEQUENTIAL;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.APPLICANT_WRITTEN_REP_CONCURRENT;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.CONCURRENT_WRITTEN_REP;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGES_DIRECTION_GIVEN;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_APPROVED_THE_ORDER;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_DISMISSED_APPLICATION;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.LIST_FOR_HEARING;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.NON_CRITERION;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.SEQUENTIAL_WRITTEN_REP;

public class JudicialDecisionNotificationUtil {

    private JudicialDecisionNotificationUtil(){
        // Utilities class, no instance
    }

    private static final String FOR_SUMMARY_JUDGEMENT = "for summary judgment";
    private static final String TO_STRIKE_OUT = "to strike out";
    private static final String TO_STAY_THE_CLAIM = "to stay the claim";
    private static final String TO_EXTEND_TIME = "to extend time";
    private static final String JUDGES_DECISION = "JUDGE_MAKES_DECISION";

    public static NotificationCriterion notificationCriterion(CaseData caseData) {
        return
            isApplicationForConcurrentWrittenRep(caseData) ? CONCURRENT_WRITTEN_REP :
            isApplicationForSequentialWrittenRep(caseData) ? SEQUENTIAL_WRITTEN_REP :
            isWrittenRepForApplicantConcurrent(caseData) ? APPLICANT_WRITTEN_REP_CONCURRENT :
            isWrittenRepForApplicantSequential(caseData) ? APPLICANT_WRITTEN_REP_SEQUENTIAL :
            isListForHearing(caseData) ? LIST_FOR_HEARING :
            isRequestMoreInfo(caseData) ? REQUEST_MORE_INFO :
            isGiveDirectionsOnOrder(caseData) ? JUDGES_DIRECTION_GIVEN :
            isJudicialDismissal(caseData) ? JUDGE_DISMISSED_APPLICATION :
            isJudgesApproval(caseData) ? JUDGE_APPROVED_THE_ORDER :

            NON_CRITERION;
    }

    private static GAJudgeWrittenRepresentationsOptions writtenOptions(CaseData caseData) {
        return Optional
            .ofNullable(caseData
                            .getJudicialDecisionMakeAnOrderForWrittenRepresentations())
            .map(GAJudicialWrittenRepresentations::getWrittenOption)
            .orElse(null);
    }

    public static String requiredGAType(List<GeneralApplicationTypes> applicationTypes) {

        for (GeneralApplicationTypes type : applicationTypes) {
            return
                type.equals(GeneralApplicationTypes.STRIKE_OUT) ? TO_STRIKE_OUT :
                type.equals(GeneralApplicationTypes.SUMMARY_JUDGEMENT) ? FOR_SUMMARY_JUDGEMENT :
                type.equals(GeneralApplicationTypes.STAY_THE_CLAIM) ? TO_STAY_THE_CLAIM :
                type.equals(GeneralApplicationTypes.EXTEND_TIME) ? TO_EXTEND_TIME : null;
        }
        return null;
    }

    private static boolean isApplicationForConcurrentWrittenRep(CaseData caseData) {
        boolean isApplicantPresent = StringUtils.isNotEmpty(caseData.getGeneralAppApplnSolicitor().getEmail());
        return
            (Objects.requireNonNull(writtenOptions(caseData))
            .equals(GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS))
            && isApplicantPresent
            && isRespondentPresent(caseData);
    }

    private static boolean isApplicationForSequentialWrittenRep(CaseData caseData) {
        boolean isApplicantPresent = StringUtils.isNotEmpty(caseData.getGeneralAppApplnSolicitor().getEmail());
        return
            (Objects.requireNonNull(writtenOptions(caseData))
            .equals(GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS))
            && isApplicantPresent
            && isRespondentPresent(caseData);
    }

    private static boolean isRespondentPresent(CaseData caseData) {
        var respondents  = Optional
            .ofNullable(
                caseData
                    .getGeneralAppRespondentSolicitors())
            .stream().flatMap(
                List::stream
            ).filter(e -> !e.getValue().getEmail().isEmpty()).findFirst().orElse(null);
        return respondents != null;
    }

    private static boolean isRequestMoreInfo(CaseData caseData) {
        return isJudicialDecisionEvent(caseData)
            && Objects.requireNonNull(caseData.getJudicialDecision().getDecision())
            .equals(GAJudgeDecisionOption.REQUEST_MORE_INFO);
    }

    private static boolean isListForHearing(CaseData caseData) {
        return
            isJudicialDecisionEvent(caseData)
            && Objects.requireNonNull(caseData.getJudicialDecision().getDecision())
            .equals(GAJudgeDecisionOption.LIST_FOR_A_HEARING);
    }

    private static boolean isGiveDirectionsOnOrder(CaseData caseData) {
        return isJudicialDecisionEvent(caseData)
            && Objects.requireNonNull(caseData.getJudicialDecisionMakeOrder().getMakeAnOrder())
            .equals(GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING);
    }

    private static boolean isJudicialDismissal(CaseData caseData) {
        return
            isJudicialDecisionEvent(caseData)
            && Objects.requireNonNull(caseData.getJudicialDecisionMakeOrder().getMakeAnOrder())
            .equals(GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION);
    }

    private static boolean isJudgesApproval(CaseData caseData) {
        return
            isJudicialDecisionEvent(caseData)
            && Objects.requireNonNull(caseData.getJudicialDecisionMakeOrder().getMakeAnOrder())
            .equals(GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT);
    }

    private static boolean isWrittenRepForApplicantSequential(CaseData caseData) {
        return
            !isRespondentPresent(caseData)
            && (Objects.requireNonNull(writtenOptions(caseData))
            .equals(GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS));
    }

    private static boolean isWrittenRepForApplicantConcurrent(CaseData caseData) {
        return
            !isRespondentPresent(caseData)
            && (Objects.requireNonNull(writtenOptions(caseData))
            .equals(GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS));
    }

    private static boolean isJudicialDecisionEvent(CaseData caseData) {
        return
            Objects.nonNull(caseData.getBusinessProcess().getCamundaEvent())
            && caseData.getBusinessProcess().getCamundaEvent()
            .equals(JUDGES_DECISION);
    }
}
