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

    public YesOrNo isApplicantAndRespondentHearingPrefSame(CaseData caseData) {
        return (caseData.getHearingDetailsResp() != null
            && caseData.getRespondentsResponses() != null
            && caseData.getRespondentsResponses().size() == 1
            && caseData.getGeneralAppHearingDetails().getHearingPreferencesPreferredType().getDisplayedValue()
            .equals(caseData.getRespondentsResponses().stream().iterator().next().getValue().getGaHearingDetails()
                        .getHearingPreferencesPreferredType().getDisplayedValue()))
            ? YES : NO;
    }

    public YesOrNo isApplicantAndRespondentSupportReqSame(CaseData caseData) {
        return (caseData.getHearingDetailsResp() != null
            && caseData.getRespondentsResponses() != null
            && caseData.getRespondentsResponses().size() == 1
            && caseData.getGeneralAppHearingDetails().getHearingPreferencesPreferredType().getDisplayedValue()
            .equals(caseData.getRespondentsResponses().stream().iterator().next().getValue().getGaHearingDetails()
                        .getHearingPreferencesPreferredType().getDisplayedValue()))
            ? YES : NO;
    }

    public boolean isApplicantAndRespondentLocationPrefSame(CaseData caseData) {
        return caseData.getGeneralAppHearingDetails() != null
            && caseData.getRespondentsResponses() != null
            && caseData.getRespondentsResponses().size() == 1
            && caseData.getGeneralAppHearingDetails().getHearingPreferredLocation().getValue().getLabel()
            .equals(caseData.getRespondentsResponses().stream().iterator().next().getValue().getGaHearingDetails()
                        .getHearingPreferredLocation().getValue().getLabel());
    }

    public YesOrNo isApplicantAndRespondentHearingDurationSame(CaseData caseData) {
        return (caseData.getHearingDetailsResp() != null
            && caseData.getRespondentsResponses() != null
            && caseData.getRespondentsResponses().size() == 1
            && caseData.getGeneralAppHearingDetails().getHearingDuration().getDisplayedValue()
            .equals(caseData.getRespondentsResponses().stream().iterator().next().getValue().getGaHearingDetails()
                        .getHearingDuration().getDisplayedValue()))
            ? YES : NO;
    }
}
