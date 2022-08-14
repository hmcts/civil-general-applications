package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.Objects;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@Slf4j
@Service
@RequiredArgsConstructor
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
            || caseData.getRespondentsResponses() == null
            || caseData.getRespondentsResponses().stream().filter(
                e -> e.getValue().getGaHearingDetails().getHearingPreferredLocation() == null).count() > 0) {
            return false;
        }
        String applicantLocation = caseData.getGeneralAppHearingDetails().getHearingPreferredLocation()
            .getValue().getLabel();
        long count = caseData.getRespondentsResponses().stream()
            .filter(e -> !applicantLocation.equals(
                e.getValue().getGaHearingDetails().getHearingPreferredLocation().getValue().getLabel())).count();
        return count == 0;
    }

    public boolean isOrderMakeDecisionMadeVisibleToDefendant(CaseData caseData) {
        if (isApplicationCloaked(caseData).equals(YES)
            && Objects.nonNull(caseData.getApplicationIsCloaked())
            && caseData.getApplicationIsCloaked().equals(NO)
            && caseData.getJudicialDecision().getDecision().equals(GAJudgeDecisionOption.MAKE_AN_ORDER)) {
            return true;
        }
        return false;
    }

    public boolean isApplicationUncloakedWithAdditionalFee(CaseData caseData) {
        return false;

        /*TODO: Uncomment this code and revise the logic with CIV-3759
        var decision = Optional.ofNullable(caseData.getJudicialDecision())
            .map(GAJudicialDecision::getDecision).orElse(null);
        return !isApplicationCloaked(caseData)
            && caseData.getGeneralAppRespondentAgreement().getHasAgreed().equals(NO)
            && caseData.getGeneralAppInformOtherParty().getIsWithNotice().equals(NO)
            && Objects.nonNull(decision)
            && caseData.getJudicialDecision()
            .getDecision().equals(REQUEST_MORE_INFO);*/
    }
}
