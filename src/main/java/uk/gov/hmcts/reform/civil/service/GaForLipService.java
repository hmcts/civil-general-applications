package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil;

import java.util.Objects;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@RequiredArgsConstructor
@Service
public class GaForLipService {

    private final FeatureToggleService featureToggleService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;

    public boolean isGaForLip(CaseData caseData) {
        return featureToggleService.isGaForLipsEnabled() && (Objects.nonNull(caseData.getIsGaApplicantLip())
                && caseData.getIsGaApplicantLip().equals(YES))
                || (Objects.nonNull(caseData.getIsGaRespondentOneLip())
                && caseData.getIsGaRespondentOneLip().equals(YES))
                || (caseData.getIsMultiParty().equals(YES)
                && Objects.nonNull(caseData.getIsGaRespondentTwoLip())
                && caseData.getIsGaRespondentTwoLip().equals(YES));
    }

    public boolean isLipApp(CaseData caseData) {
        return featureToggleService.isGaForLipsEnabled()
                && Objects.nonNull(caseData.getIsGaApplicantLip())
                && caseData.getIsGaApplicantLip().equals(YES);
    }

    public boolean isLipResp(CaseData caseData) {
        return featureToggleService.isGaForLipsEnabled()
                && Objects.nonNull(caseData.getIsGaRespondentOneLip())
                && caseData.getIsGaRespondentOneLip().equals(YES);
    }

    public boolean isWelshApp(CaseData caseData) {
        if (featureToggleService.isGaForLipsEnabled()) {
            CaseData civilCaseData = caseDetailsConverter
                .toCaseData(coreCaseDataService
                                .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));
            return civilCaseData.isApplicantBilingual(caseData.getParentClaimantIsApplicant());
        }
        return false;
    }

    public boolean isWelshResp(CaseData caseData) {
        if (featureToggleService.isGaForLipsEnabled()) {
            CaseData civilCaseData = caseDetailsConverter
                .toCaseData(coreCaseDataService
                                .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));
            return civilCaseData.isRespondentBilingual(caseData.getParentClaimantIsApplicant());
        }
        return false;
    }

    public boolean anyWelsh(CaseData caseData) {
        if (featureToggleService.isGaForLipsEnabled()) {
            CaseData civilCaseData = caseDetailsConverter
                .toCaseData(coreCaseDataService
                                .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));
            return civilCaseData.isApplicantBilingual(caseData.getParentClaimantIsApplicant())
                || civilCaseData.isRespondentBilingual(caseData.getParentClaimantIsApplicant());
        }
        return false;
    }

    public boolean anyWelshNotice(CaseData caseData) {
        if (featureToggleService.isGaForLipsEnabled()) {
            CaseData civilCaseData = caseDetailsConverter
                .toCaseData(coreCaseDataService
                                .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));
            if (!JudicialDecisionNotificationUtil.isWithNotice(caseData)) {
                return civilCaseData.isApplicantBilingual(caseData.getParentClaimantIsApplicant());
            }
            return civilCaseData.isApplicantBilingual(caseData.getParentClaimantIsApplicant())
                || civilCaseData.isRespondentBilingual(caseData.getParentClaimantIsApplicant());
        }
        return false;
    }
}
