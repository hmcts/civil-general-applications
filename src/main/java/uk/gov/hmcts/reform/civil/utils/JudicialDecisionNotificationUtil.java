package uk.gov.hmcts.reform.civil.utils;

import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.CONCURRENT_WRITTEN_REP;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGES_DIRECTION_GIVEN;
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
            isListForHearing(caseData) ? LIST_FOR_HEARING :
            isRequestMoreInfo(caseData) ? REQUEST_MORE_INFO :
            isGiveDirectionsOnOrder(caseData) ? JUDGES_DIRECTION_GIVEN :
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
        return writtenOptions(caseData) != null
            && (writtenOptions(caseData).equals(GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS))
            && isApplicantPresent
            && isRespondentPresent(caseData);
    }

    private static boolean isApplicationForSequentialWrittenRep(CaseData caseData) {
        boolean isApplicantPresent = StringUtils.isNotEmpty(caseData.getGeneralAppApplnSolicitor().getEmail());
        return writtenOptions(caseData) != null
            && (writtenOptions(caseData).equals(GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS))
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
            && caseData.getJudicialDecision().getDecision() != null
            && caseData.getJudicialDecision().getDecision()
            .equals(GAJudgeDecisionOption.REQUEST_MORE_INFO);
    }

    private static boolean isListForHearing(CaseData caseData) {
        return
            isJudicialDecisionEvent(caseData)
            && caseData.getJudicialDecision().getDecision() != null
            && caseData.getJudicialDecision().getDecision()
            .equals(GAJudgeDecisionOption.LIST_FOR_A_HEARING);
    }

    private static boolean isGiveDirectionsOnOrder(CaseData caseData) {
        return isJudicialDecisionEvent(caseData)
            && !caseData.getJudicialGOHearingDirections().isEmpty();
    }

    private static boolean isJudicialDecisionEvent(CaseData caseData) {
        return
            caseData.getBusinessProcess().getCamundaEvent() != null
            && caseData.getBusinessProcess().getCamundaEvent()
            .equals(JUDGES_DECISION);
    }

}
