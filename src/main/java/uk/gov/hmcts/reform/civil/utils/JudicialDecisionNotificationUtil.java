package uk.gov.hmcts.reform.civil.utils;

import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.APPLICATION_CHANGE_TO_WITH_NOTICE;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.CONCURRENT_WRITTEN_REP;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_APPROVED_THE_ORDER;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_APPROVED_THE_ORDER_CLOAK;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_DIRECTION_ORDER;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.JUDGE_DIRECTION_ORDER_CLOAK;
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
            && !isApplicationCloaked(caseData)) {
            return JUDGE_DISMISSED_APPLICATION;
        }
        if (isApplicationCloaked(caseData)
            && isJudicialDismissal(caseData)) {
            return JUDGE_DISMISSED_APPLICATION_CLOAK;
        }
        if (isJudicialApproval(caseData)
            && !isApplicationCloaked(caseData)) {
            return JUDGE_APPROVED_THE_ORDER;
        }
        if (isApplicationCloaked(caseData)
            && isJudicialApproval(caseData)) {
            return JUDGE_APPROVED_THE_ORDER_CLOAK;
        }
        if (isDirectionOrder(caseData)
            && !isApplicationCloaked(caseData)) {
            return JUDGE_DIRECTION_ORDER;
        }
        if (isDirectionOrder(caseData)
            && isApplicationCloaked(caseData)) {
            return JUDGE_DIRECTION_ORDER_CLOAK;
        }
        if (isRequestForInformationWithouNotice(caseData)) {
            return REQUEST_FOR_INFORMATION;
        }
        if (isRequestForInformationWithoutNoticeToNotice(caseData)) {
            return APPLICATION_CHANGE_TO_WITH_NOTICE;
        }
        if (isRequestForInformationWithNotice(caseData)) {
            return REQUEST_FOR_INFORMATION;
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

    public static boolean isApplicationUncloakedInJudicialDecision(CaseData caseData) {
        return !isApplicationCloaked(caseData) && caseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(NO)
            && caseData.getGeneralAppInformOtherParty().getIsWithNotice().equals(NO);
    }

    public static boolean isApplicationCloaked(CaseData caseData) {
        var decision = Optional.ofNullable(caseData.getJudicialDecision())
            .map(GAJudicialDecision::getDecision).orElse(null);
        return isJudicialDecisionEvent(caseData)
            && Objects.nonNull(caseData.getApplicationIsCloaked())
            && Objects.nonNull(decision)
            && caseData.getJudicialDecision()
            .getDecision().equals(GAJudgeDecisionOption.MAKE_AN_ORDER)
            && caseData.getApplicationIsCloaked().equals(YES);
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

    private static boolean isRequestForInformationWithouNotice(CaseData caseData) {
        var decision = Optional.ofNullable(caseData.getJudicialDecisionRequestMoreInfo())
            .map(GAJudicialRequestMoreInfo::getRequestMoreInfoOption).orElse(null);
        return
            isJudicialDecisionEvent(caseData)
                && Objects.nonNull(decision)
                && caseData.getJudicialDecision()
                .getDecision().equals(REQUEST_MORE_INFO)
                && caseData.getJudicialDecisionRequestMoreInfo()
                .getRequestMoreInfoOption().equals(
                    GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION)
                && caseData.getJudicialDecisionRequestMoreInfo()
                .getJudgeRequestMoreInfoText() != null
                && caseData.getJudicialDecisionRequestMoreInfo()
                .getJudgeRequestMoreInfoByDate() != null;
    }

    private static boolean isRequestForInformationWithoutNoticeToNotice(CaseData caseData) {
        var decision = Optional.ofNullable(caseData.getJudicialDecisionRequestMoreInfo())
            .map(GAJudicialRequestMoreInfo::getRequestMoreInfoOption).orElse(null);
        return
            isJudicialDecisionEvent(caseData)
                && Objects.nonNull(decision)
                && caseData.getJudicialDecision()
                .getDecision().equals(REQUEST_MORE_INFO)
                && caseData.getJudicialDecisionRequestMoreInfo()
                .getRequestMoreInfoOption().equals(
                    GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY);
//                && caseData.getJudicialDecisionRequestMoreInfo()
//                .getJudgeRequestMoreInfoText() != null
//                && caseData.getJudicialDecisionRequestMoreInfo()
//                .getJudgeRequestMoreInfoByDate() != null;
    }

    private static boolean isRequestForInformationWithNotice(CaseData caseData) {
        var decision = Optional.ofNullable(caseData.getJudicialDecisionRequestMoreInfo())
            .map(GAJudicialRequestMoreInfo::getRequestMoreInfoOption).orElse(null);
        return
            isJudicialDecisionEvent(caseData)
                && Objects.isNull(decision)
                && caseData.getJudicialDecision()
                .getDecision().equals(REQUEST_MORE_INFO)
                && caseData.getJudicialDecisionRequestMoreInfo()
                .getRequestMoreInfoOption() == null
                && caseData.getJudicialDecisionRequestMoreInfo()
                .getJudgeRequestMoreInfoText() != null
                && caseData.getJudicialDecisionRequestMoreInfo()
                .getJudgeRequestMoreInfoByDate() != null;
    }

    private static boolean isJudicialDecisionEvent(CaseData caseData) {
        var judicialDecision = Optional.ofNullable(caseData.getBusinessProcess())
            .map(BusinessProcess::getCamundaEvent).orElse(null);
        return
            Objects.nonNull(judicialDecision)
            && caseData.getBusinessProcess().getCamundaEvent()
            .equals(JUDGES_DECISION);
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

        if (caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations() != null
            && caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
            .getWrittenOption().equals(gaJudgeWrittenRepresentationsOptions)) {
            return true;
        }
        return false;
    }

}
