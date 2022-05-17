package uk.gov.hmcts.reform.civil.utils;

import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;

import java.util.Optional;

import static uk.gov.hmcts.reform.civil.utils.ApplicationNotificationUtil.respondentIsPresent;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.CONCURRENT_WRITTEN_REP;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.NON_CRITERION;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.SEQUENTIAL_WRITTEN_REP;

public class JudicialDecisionNotificationUtil {

    private JudicialDecisionNotificationUtil(){
        // Utilities class, no instance
    }

    public static NotificationCriterion getNotificationCriteria(CaseData caseData) {

        if (isNotificationCriteriaSatisfiedForWrittenReps(caseData)
            && isApplicationForConcurrentWrittenRep(caseData)
            && respondentIsPresent(caseData)) {
            return CONCURRENT_WRITTEN_REP;
        } else if (isNotificationCriteriaSatisfiedForWrittenReps(caseData)
            && isApplicationForSequentialWrittenRep(caseData)
            && respondentIsPresent(caseData)) {
            return SEQUENTIAL_WRITTEN_REP;
        }
        return NON_CRITERION;
    }

    private static GAJudgeWrittenRepresentationsOptions writtenOptions(CaseData caseData) {
        return Optional
            .ofNullable(caseData
                            .getJudicialDecisionMakeAnOrderForWrittenRepresentations())
            .map(GAJudicialWrittenRepresentations::getWrittenOption)
            .orElse(null);
    }

    private static boolean isNotificationCriteriaSatisfiedForWrittenReps(CaseData caseData) {
        boolean isApplicantPresent = StringUtils.isNotEmpty(caseData.getGeneralAppApplnSolicitor().getEmail());
        return (isApplicationForConcurrentWrittenRep(caseData) || isApplicationForSequentialWrittenRep(caseData))
            && isApplicantPresent;
    }

    private static boolean isApplicationForConcurrentWrittenRep(CaseData caseData) {
        return writtenOptions(caseData) != null
            && (writtenOptions(caseData)
            .equals(
                GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS
            ));
    }

    private static boolean isApplicationForSequentialWrittenRep(CaseData caseData) {
        return writtenOptions(caseData) != null
            && (writtenOptions(caseData)
            .equals(
                GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS
            ));
    }

}
