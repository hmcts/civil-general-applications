package uk.gov.hmcts.reform.civil.utils;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.CONCURRENT_WRITTEN_REP;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_APPROVED_THE_ORDER;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_DIRECTION_ORDER;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_DISMISSED_APPLICATION;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.LIST_FOR_HEARING;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.NON_CRITERION;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.REQUEST_FOR_INFORMATION;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.SEQUENTIAL_WRITTEN_REP;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_FREE_FORM_ORDER;

public class JudicialDecisionNotificationUtil {

    private JudicialDecisionNotificationUtil() {
        // Utilities class, no instance
    }

    private static final String JUDGES_DECISION = "MAKE_DECISION";
    private static final String UPLOAD_TRANSLATED_JUDGES_DECISION = "UPLOAD_TRANSLATED_DOCUMENT_JUDGE_DECISION";

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
        if (isFreeFormOrder(caseData)) {
            return JUDGE_FREE_FORM_ORDER;
        }
        if (isJudicialDismissal(caseData)) {
            return JUDGE_DISMISSED_APPLICATION;
        }
        if (isJudicialApproval(caseData)) {
            return JUDGE_APPROVED_THE_ORDER;
        }
        if (isDirectionOrder(caseData)) {
            return JUDGE_DIRECTION_ORDER;
        }
        if (isRequestForInformation(caseData)) {
            return REQUEST_FOR_INFORMATION;
        }
        if (Objects.nonNull(caseData.getApproveConsentOrder())) {
            return JUDGE_APPROVED_THE_ORDER;
        }
        return NON_CRITERION;
    }

    public static String requiredGAType(CaseData caseData) {
        List<GeneralApplicationTypes> types = caseData.getGeneralAppType().getTypes();
        return types.stream().map(GeneralApplicationTypes::getDisplayedValue)
            .collect(Collectors.joining(", "));
    }

    private static boolean isApplicationForConcurrentWrittenRep(CaseData caseData) {
        boolean isApplicantPresent = isApplicantPresent(caseData.getGeneralAppApplnSolicitor());
        boolean isRespondentPresent = areRespondentSolicitorsPresent(caseData);
        boolean isAppConcurWrittenRep = isAppWrittenRepresentationOfGivenType(caseData,
                                                                              GAJudgeWrittenRepresentationsOptions
                                                                                 .CONCURRENT_REPRESENTATIONS);
        return
            isJudicialDecisionEvent(caseData)
            && isAppConcurWrittenRep
            && isApplicantPresent
            && isRespondentPresent;
    }

    private static boolean isApplicationForSequentialWrittenRep(CaseData caseData) {
        boolean isApplicantPresent = isApplicantPresent(caseData.getGeneralAppApplnSolicitor());
        boolean isRespondentPresent = areRespondentSolicitorsPresent(caseData);
        boolean isAppSeqWrittenRep = isAppWrittenRepresentationOfGivenType(caseData,
                                                                          GAJudgeWrittenRepresentationsOptions
                                                                              .SEQUENTIAL_REPRESENTATIONS);
        return
            isJudicialDecisionEvent(caseData)
            && isAppSeqWrittenRep
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

    public static boolean isApplicationCloaked(CaseData caseData) {
        var decision = Optional.ofNullable(caseData.getJudicialDecision())
            .map(GAJudicialDecision::getDecision).orElse(null);
        return isJudicialDecisionEvent(caseData)
            && Objects.nonNull(decision)
            && (Objects.isNull(caseData.getApplicationIsCloaked()) || caseData.getApplicationIsCloaked().equals(YES));
    }

    public static boolean isGeneralAppConsentOrder(CaseData caseData) {
        return Objects.nonNull(caseData.getGeneralAppConsentOrder());
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

    private static boolean isFreeFormOrder(CaseData caseData) {
        var decision = Optional.ofNullable(caseData.getJudicialDecision())
            .map(GAJudicialDecision::getDecision).orElse(null);
        return
            isJudicialDecisionEvent(caseData)
                && (decision != null)
                && caseData.getJudicialDecision().getDecision()
                .equals(GAJudgeDecisionOption.FREE_FORM_ORDER);
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

    private static boolean isRequestForInformation(CaseData caseData) {
        return
            isJudicialDecisionEvent(caseData)
                && caseData.getJudicialDecision()
                .getDecision().equals(REQUEST_MORE_INFO);
    }

    private static boolean isJudicialDecisionEvent(CaseData caseData) {
        var judicialDecision = Optional.ofNullable(caseData.getBusinessProcess())
            .map(BusinessProcess::getCamundaEvent).orElse(null);
        return
            Objects.nonNull(judicialDecision)
            && (caseData.getBusinessProcess().getCamundaEvent().equals(JUDGES_DECISION)
                || caseData.getBusinessProcess().getCamundaEvent().equals(UPLOAD_TRANSLATED_JUDGES_DECISION));
    }

    private static boolean isApplicantPresent(GASolicitorDetailsGAspec gaSolicitorDetailsGAspec) {
        if (gaSolicitorDetailsGAspec != null && gaSolicitorDetailsGAspec.getEmail() != null) {
            return StringUtils.isNotEmpty(gaSolicitorDetailsGAspec.getEmail());
        }
        return false;
    }

    private static boolean isAppWrittenRepresentationOfGivenType(CaseData caseData,
                                                                 GAJudgeWrittenRepresentationsOptions
                                                                             gaJudgeWrittenRepresentationsOptions) {

        return caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations() != null
            && caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
            .getWrittenOption().equals(gaJudgeWrittenRepresentationsOptions);
    }

    public static boolean isNotificationCriteriaSatisfied(CaseData caseData) {

        if (!CollectionUtils.isEmpty(caseData.getGeneralAppRespondentSolicitors())) {

            var recipient = caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getEmail();
            return isWithNotice(caseData)
                && isNonUrgent(caseData)
                && !(StringUtils.isEmpty(recipient));
        }
        return false;
    }

    public static boolean isUrgentApplnNotificationCriteriaSatisfied(CaseData caseData) {

        if (!CollectionUtils.isEmpty(caseData.getGeneralAppRespondentSolicitors())) {

            var recipient = caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getEmail();
            return isWithNotice(caseData)
                && !isNonUrgent(caseData)
                && !(StringUtils.isEmpty(recipient));
        }
        return false;
    }

    public static boolean isWithNotice(CaseData caseData) {
        // Check if the judge uncloaks the application, in addition
        return (caseData.getApplicationIsUncloakedOnce() != null
            && caseData.getApplicationIsUncloakedOnce().equals(YES))
            || (caseData.getApplicationIsCloaked() != null
            && caseData.getApplicationIsCloaked().equals(NO))
            || (caseData.getGeneralAppInformOtherParty() != null
                && YES.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice()));
    }

    public static boolean isNonUrgent(CaseData caseData) {
        return caseData.getGeneralAppUrgencyRequirement() != null
            && caseData.getGeneralAppUrgencyRequirement()
                .getGeneralAppUrgency() == NO;
    }

    public static boolean isUrgent(CaseData caseData) {
        return caseData.getGeneralAppUrgencyRequirement() != null
            && caseData.getGeneralAppUrgencyRequirement()
            .getGeneralAppUrgency() == YES;
    }
}
