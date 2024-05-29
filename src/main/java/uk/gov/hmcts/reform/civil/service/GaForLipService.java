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
        return featureToggleService.isGaForLipsEnabled() && (Objects.nonNull(caseData.getIsGaApplicantLip())
            && caseData.getIsGaApplicantLip().equals(YES))
            || (Objects.nonNull(caseData.getIsGaRespondentOneLip())
            && caseData.getIsGaRespondentOneLip().equals(YES))
            || (caseData.getIsMultiParty().equals(YES)
            && Objects.nonNull(caseData.getIsGaRespondentTwoLip())
            && caseData.getIsGaRespondentTwoLip().equals(YES));

    }
}
