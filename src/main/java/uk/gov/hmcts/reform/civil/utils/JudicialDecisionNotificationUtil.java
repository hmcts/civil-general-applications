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
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.CONCURRENT_WRITTEN_REP;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.DIRECTION_ORDER;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_APPROVED_THE_ORDER;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_APPROVED_THE_ORDER_CLOAK;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_DISMISSED_APPLICATION;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_DISMISSED_APPLICATION_CLOAK;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.LIST_FOR_HEARING;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.NON_CRITERION;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.REQUEST_FOR_INFORMATION;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.SEQUENTIAL_WRITTEN_REP;

public class JudicialDecisionNotificationUtil {

    private JudicialDecisionNotificationUtil(){
        // Utilities class, no instance
    }

    private static final String JUDGES_DECISION = "JUDGE_MAKES_DECISION";

    public static NotificationCriterion notificationCriterion(CaseData caseData) {

        if (isApplicationForConcurrentWrittenRep(caseData)) {
            return CONCURRENT_WRITTEN_REP;
        }
        if (isApplicationForSequentialWrittenRep(caseData)) {
            return SEQUENTIAL_WRITTEN_REP;
        }
        if (isListForHearing(caseData)) {
            return LIST_FOR_HEARING;
        }
        if (isJudicialDismissal(caseData)
            && !isApplicationUnCloaked(caseData)) {
            return JUDGE_DISMISSED_APPLICATION;
        }
        if (isJudicialApproval(caseData)
            && !isApplicationUnCloaked(caseData)) {
            return JUDGE_APPROVED_THE_ORDER;
        }
        if (isApplicationUnCloaked(caseData)
            && isJudicialDismissal(caseData)) {
            return JUDGE_DISMISSED_APPLICATION_CLOAK;
        }
        if (isApplicationUnCloaked(caseData)
            && isJudicialApproval(caseData)) {
            return JUDGE_APPROVED_THE_ORDER_CLOAK;
        }
        if (isDirectionOrder(caseData)
            && !isApplicationUnCloaked(caseData)) {
            return DIRECTION_ORDER;
        }
        if (isRequestForInfomration(caseData)) {
            return REQUEST_FOR_INFORMATION;
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

    public static String requiredGAType(CaseData caseData) {
        List<GeneralApplicationTypes> types = caseData.getGeneralAppType().getTypes();
        return types.stream().map(GeneralApplicationTypes::getDisplayedValue)
            .collect(Collectors.joining(", "));
    }

    private static boolean isApplicationForConcurrentWrittenRep(CaseData caseData) {
        boolean isApplicantPresent = StringUtils.isNotEmpty(caseData.getGeneralAppApplnSolicitor().getEmail());
        boolean isRespondentPresent = areRespondentSolicitorsPresent(caseData);
        return
            isJudicialDecisionEvent(caseData)
            && Objects.nonNull(writtenOptions(caseData))
            && ((writtenOptions(caseData))
            .equals(GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS))
            && isApplicantPresent
            && isRespondentPresent;
    }

    private static boolean isApplicationForSequentialWrittenRep(CaseData caseData) {
        boolean isApplicantPresent = StringUtils.isNotEmpty(caseData.getGeneralAppApplnSolicitor().getEmail());
        boolean isRespondentPresent = areRespondentSolicitorsPresent(caseData);
        return
            isJudicialDecisionEvent(caseData)
            && Objects.nonNull(writtenOptions(caseData))
            && ((writtenOptions(caseData))
            .equals(GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS))
            && isApplicantPresent
            && isRespondentPresent;
    }

    public static boolean areRespondentSolicitorsPresent(CaseData caseData) {
        var respondents  = Optional
            .ofNullable(
                caseData
                    .getGeneralAppRespondentSolicitors())
            .stream().flatMap(
                List::stream
            ).filter(e -> !e.getValue().getEmail().isEmpty()).findFirst().orElse(null);
        return respondents != null;
    }

    public static boolean isApplicationUnCloaked(CaseData caseData) {
        var decision = Optional.ofNullable(caseData.getJudicialDecision())
            .map(GAJudicialDecision::getDecision).orElse(null);
        return isJudicialDecisionEvent(caseData)
            && Objects.nonNull(caseData.getApplicationIsCloaked())
            && Objects.nonNull(decision)
            && caseData.getJudicialDecision()
            .getDecision().equals(GAJudgeDecisionOption.MAKE_AN_ORDER)
            && caseData.getApplicationIsCloaked().equals(YesOrNo.YES);
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

    private static boolean isJudicialDismissal(CaseData caseData) {
        var judicialDecision = Optional.ofNullable(caseData.getJudicialDecisionMakeOrder())
            .map(GAJudicialMakeAnOrder::getMakeAnOrder).orElse(null);
        return
            isJudicialDecisionEvent(caseData)
            && Objects.nonNull(judicialDecision)
            && caseData.getJudicialDecisionMakeOrder().getMakeAnOrder()
            .equals(GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION);
    }

    private static boolean isJudicialApproval(CaseData caseData) {
        var judicialDecision = Optional.ofNullable(caseData.getJudicialDecisionMakeOrder())
            .map(GAJudicialMakeAnOrder::getMakeAnOrder).orElse(null);
        return
            isJudicialDecisionEvent(caseData)
                && Objects.nonNull(judicialDecision)
                && caseData.getJudicialDecisionMakeOrder().getMakeAnOrder()
                .equals(GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT);
    }

    private static boolean isDirectionOrder(CaseData caseData) {
        var judicialDecision = Optional.ofNullable(caseData.getJudicialDecisionMakeOrder())
            .map(GAJudicialMakeAnOrder::getMakeAnOrder).orElse(null);
        return
            isJudicialDecisionEvent(caseData)
            && Objects.nonNull(judicialDecision)
            && caseData.getJudicialDecisionMakeOrder().getMakeAnOrder()
                .equals(GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING);
    }

    private static boolean isRequestForInfomration(CaseData caseData) {
        var decision = Optional.ofNullable(caseData.getJudicialDecision())
            .map(GAJudicialDecision::getDecision).orElse(null);
        return
            isJudicialDecisionEvent(caseData)
                && (decision != null)
                && caseData.getJudicialDecision().getDecision()
                .equals(GAJudgeDecisionOption.REQUEST_MORE_INFO);
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
