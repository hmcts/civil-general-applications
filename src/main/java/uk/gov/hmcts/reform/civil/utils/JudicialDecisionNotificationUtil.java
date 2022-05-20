package uk.gov.hmcts.reform.civil.utils;

import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.APPLICANT_WRITTEN_REP_CONCURRENT;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.APPLICANT_WRITTEN_REP_SEQUENTIAL;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.APPLICATION_MOVES_TO_WITH_NOTICE;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.APPLICATION_UNCLOAK;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.CONCURRENT_WRITTEN_REP;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_DISMISSED_APPLICATION;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.LIST_FOR_HEARING;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.NON_CRITERION;
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

        if (isApplicationForConcurrentWrittenRep(caseData)) {
            return CONCURRENT_WRITTEN_REP;
        } else if (isApplicationForSequentialWrittenRep(caseData)) {
            return SEQUENTIAL_WRITTEN_REP;
        } else if (isWrittenRepForApplicantConcurrent(caseData)) {
            return APPLICANT_WRITTEN_REP_CONCURRENT;
        } else if (isWrittenRepForApplicantSequential(caseData)) {
            return APPLICANT_WRITTEN_REP_SEQUENTIAL;
        } else if (isListForHearing(caseData)) {
            return LIST_FOR_HEARING;
        } else if (isApplicationUncloaked(caseData)) {
            return APPLICATION_UNCLOAK;
        } else if (isApplicationAmendedWithNotice(caseData)) {
            return APPLICATION_MOVES_TO_WITH_NOTICE;
        } else if (isJudicialDismissal(caseData)) {
            return JUDGE_DISMISSED_APPLICATION;
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

    public static String requiredGAType(List<GeneralApplicationTypes> applicationTypes) {

        for (GeneralApplicationTypes type : applicationTypes) {
            if (type.equals(GeneralApplicationTypes.STRIKE_OUT)) {
                return TO_STRIKE_OUT;
            }
            if (type.equals(GeneralApplicationTypes.SUMMARY_JUDGEMENT)) {
                return FOR_SUMMARY_JUDGEMENT;
            }
            if (type.equals(GeneralApplicationTypes.STAY_THE_CLAIM)) {
                return TO_STAY_THE_CLAIM;
            }
            if (type.equals(GeneralApplicationTypes.EXTEND_TIME)) {
                return TO_EXTEND_TIME;
            }
        }
        return null;
    }

    private static boolean isApplicationForConcurrentWrittenRep(CaseData caseData) {
        boolean isApplicantPresent = StringUtils.isNotEmpty(caseData.getGeneralAppApplnSolicitor().getEmail());
        return
            isJudicialDecisionEvent(caseData)
            && Objects.nonNull(writtenOptions(caseData))
            && ((writtenOptions(caseData))
            .equals(GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS))
            && isApplicantPresent
            && isRespondentPresent(caseData);
    }

    private static boolean isApplicationForSequentialWrittenRep(CaseData caseData) {
        boolean isApplicantPresent = StringUtils.isNotEmpty(caseData.getGeneralAppApplnSolicitor().getEmail());
        return
            isJudicialDecisionEvent(caseData)
            && Objects.nonNull(writtenOptions(caseData))
            && ((writtenOptions(caseData))
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

    private static boolean isApplicationUncloaked(CaseData caseData) {
        return isJudicialDecisionEvent(caseData)
            && Objects.nonNull(caseData.getApplicationIsCloaked())
            && caseData.getApplicationIsCloaked().equals(YesOrNo.NO);
    }

    private static boolean isListForHearing(CaseData caseData) {
        var decision = Optional.ofNullable(caseData.getJudicialDecision())
            .map(GAJudicialDecision::getDecision).orElse(null);
        return
            isJudicialDecisionEvent(caseData)
            && (decision != null)
            && caseData.getJudicialDecision().getDecision()
                .equals(GAJudgeDecisionOption.LIST_FOR_A_HEARING);
    }

    private static boolean isApplicationAmendedWithNotice(CaseData caseData) {
        return isJudicialDecisionEvent(caseData)
            && Objects.nonNull(caseData.getGeneralAppRespondentAgreement())
            && Objects.nonNull(caseData.getGeneralAppInformOtherParty())
            && NO.equals(caseData.getGeneralAppRespondentAgreement().getHasAgreed())
            && YES.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice());
    }

    private static boolean isJudicialDismissal(CaseData caseData) {
        var judicialDecision = Optional.ofNullable(caseData.getJudicialDecisionMakeOrder())
            .map(GAJudicialMakeAnOrder::getMakeAnOrder).orElse(null);
        return
            isJudicialDecisionEvent(caseData)
            && Objects.nonNull(judicialDecision)
            && caseData.getJudicialDecisionMakeOrder().getMakeAnOrder()
            .equals(GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION);
    }

    private static boolean isWrittenRepForApplicantConcurrent(CaseData caseData) {
        return
            isJudicialDecisionEvent(caseData)
            && !isRespondentPresent(caseData)
            && Objects.nonNull(writtenOptions(caseData))
            && (Objects.requireNonNull(writtenOptions(caseData))
            .equals(GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS));
    }

    private static boolean isWrittenRepForApplicantSequential(CaseData caseData) {
        return
            isJudicialDecisionEvent(caseData)
            && !isRespondentPresent(caseData)
            && Objects.nonNull(writtenOptions(caseData))
            && (Objects.requireNonNull(writtenOptions(caseData))
            .equals(GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS));
    }

    private static boolean isJudicialDecisionEvent(CaseData caseData) {
        var judicialDecision = Optional.ofNullable(caseData.getBusinessProcess())
            .map(BusinessProcess::getCamundaEvent).orElse(null);
        return
            Objects.nonNull(judicialDecision)
            && caseData.getBusinessProcess().getCamundaEvent()
            .equals(JUDGES_DECISION);
    }
}
