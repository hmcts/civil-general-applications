package uk.gov.hmcts.reform.civil.utils;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

public class ApplicationNotificationUtil {

    private static final String FOR_SUMMARY_JUDGEMENT = "for summary judgment";
    private static final String TO_STRIKE_OUT = "to strike out";
    private static final String TO_STAY_THE_CLAIM = "to stay the claim";
    private static final String TO_EXTEND_TIME = "to extend time";

    private ApplicationNotificationUtil() {
        // Utilities class, no instances
    }

    public static boolean isNotificationCriteriaSatisfied(CaseData caseData) {

        if (!CollectionUtils.isEmpty(caseData.getGeneralAppRespondentSolicitors())) {

            var recipient = caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getEmail();
            return isWithNotice(caseData)
                && isNonConsent(caseData)
                && isNonUrgent(caseData)
                && !(StringUtils.isEmpty(recipient));
        }
        return false;
    }

    public static boolean isNotificationCriteriaSatisfiedForWrittenReps(CaseData caseData) {
        boolean isApplicantPresent = StringUtils.isNotEmpty(caseData.getGeneralAppApplnSolicitor().getEmail());
        return (isApplicationForConcurrentWrittenRep(caseData) || isApplicationForSequentialWrittenRep(caseData))
            && isApplicantPresent;
    }

    public static boolean isApplicationForConcurrentWrittenRep(CaseData caseData) {
        return writtenOptions(caseData) != null
            && (writtenOptions(caseData)
            .equals(
                GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS
            ));
    }

    public static boolean isApplicationForSequentialWrittenRep(CaseData caseData) {
        return writtenOptions(caseData) != null
            && (writtenOptions(caseData)
            .equals(
                GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS
            ));
    }

    public static String getRequiredGAType(List<GeneralApplicationTypes> gaType) {

        for (GeneralApplicationTypes type : gaType) {
            if (type.equals(GeneralApplicationTypes.STRIKE_OUT)) {
                return TO_STRIKE_OUT;
            } else if (type.equals(GeneralApplicationTypes.SUMMARY_JUDGEMENT)) {
                return FOR_SUMMARY_JUDGEMENT;
            } else if (type.equals(GeneralApplicationTypes.STAY_THE_CLAIM)) {
                return TO_STAY_THE_CLAIM;
            } else if (type.equals((GeneralApplicationTypes.EXTEND_TIME))) {
                return TO_EXTEND_TIME;
            }
        }
        return null;
    }

    public static boolean respondentIsPresent(CaseData caseData) {
        var respondents  = Optional
            .ofNullable(
                caseData
                    .getGeneralAppRespondentSolicitors())
            .stream().flatMap(
            List::stream
        ).filter(e -> !e.getValue().getEmail().isEmpty()).findFirst().orElse(null);
        return respondents != null;
    }

    private static boolean isNonConsent(CaseData caseData) {
        return caseData
                .getGeneralAppRespondentAgreement()
                .getHasAgreed() == NO;
    }

    private static boolean isWithNotice(CaseData caseData) {
        return caseData.getGeneralAppRespondentAgreement() != null
                && NO.equals(caseData.getGeneralAppRespondentAgreement().getHasAgreed())
                && caseData.getGeneralAppInformOtherParty() != null
                && YES.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice());
    }

    private static boolean isNonUrgent(CaseData caseData) {
        return caseData
                .getGeneralAppUrgencyRequirement()
                .getGeneralAppUrgency() == NO;
    }

    private static GAJudgeWrittenRepresentationsOptions writtenOptions(CaseData caseData) {
        return Optional
            .ofNullable(caseData
                            .getJudicialDecisionMakeAnOrderForWrittenRepresentations())
            .map(GAJudicialWrittenRepresentations::getWrittenOption)
            .orElse(null);
    }
}
