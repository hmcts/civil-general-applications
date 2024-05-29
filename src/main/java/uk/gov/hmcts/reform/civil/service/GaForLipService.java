package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.Objects;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@RequiredArgsConstructor
@Service
public class GaForLipService {

    private final FeatureToggleService featureToggleService;

    public boolean isGaForLip(CaseData caseData) {
        return isLipApp(caseData)
                || isLipResp(caseData)
            || (caseData.getIsMultiParty().equals(YES)
            && Objects.nonNull(caseData.getIsGaRespondentTwoLip())
            && caseData.getIsGaRespondentTwoLip().equals(YES));

    }

    public static boolean isLipApp(CaseData caseData) {
        return Objects.nonNull(caseData.getIsGaApplicantLip())
                && caseData.getIsGaApplicantLip().equals(YES);
    }

    public static boolean isLipResp(CaseData caseData) {
        return Objects.nonNull(caseData.getIsGaRespondentOneLip())
                && caseData.getIsGaRespondentOneLip().equals(YES);
    }
}
