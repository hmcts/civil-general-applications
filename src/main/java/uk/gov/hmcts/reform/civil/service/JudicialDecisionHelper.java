package uk.gov.hmcts.reform.civil.service;

import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

public class JudicialDecisionHelper {

    public YesOrNo isApplicationCloaked(CaseData caseData) {
        return (caseData.getGeneralAppRespondentAgreement() != null
            && NO.equals(caseData.getGeneralAppRespondentAgreement().getHasAgreed())
            && caseData.getGeneralAppInformOtherParty() != null
            && NO.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice()))
            ? YES : NO;
    }

    public boolean isApplicantAndRespondentLocationPrefSame(CaseData caseData) {
        if (caseData.getGeneralAppHearingDetails() == null
            || caseData.getGeneralAppHearingDetails().getHearingPreferredLocation() == null
            || caseData.getRespondentsResponses() == null) {
            return false;
        }
        String applicantLocation = caseData.getGeneralAppHearingDetails().getHearingPreferredLocation()
            .getValue().getLabel();
        long count = caseData.getRespondentsResponses().stream()
            .filter(e -> !applicantLocation.equals(
                e.getValue().getGaHearingDetails().getHearingPreferredLocation().getValue().getLabel())).count();
        return count == 0;
    }
}
