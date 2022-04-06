package uk.gov.hmcts.reform.civil.utils;

import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;

import java.util.List;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

public class ApplicationNotificationUtil {

    private ApplicationNotificationUtil() {
        // Utilities class, no instances
    }

    public static boolean isNotificationCriteriaSatisfied(CaseData caseData) {

        if (caseData.getGeneralAppRespondentSolicitors() != null && caseData.getGeneralAppRespondentSolicitors().stream()
            .iterator().next().getValue().getEmail() != null) {

            var recipient = caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getEmail();
            return isWithNotice(caseData)
                && isNonConsent(caseData)
                && isNonUrgent(caseData)
                && !(recipient == null || recipient.isEmpty());
        }
        return false;
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

}
