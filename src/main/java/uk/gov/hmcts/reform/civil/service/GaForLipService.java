package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.Objects;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@RequiredArgsConstructor
@Service
public class GaForLipService {

    private static final Logger log = LoggerFactory.getLogger(GaForLipService.class);
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

    public boolean isLipApp(CaseData caseData) {
        return featureToggleService.isGaForLipsEnabled()
                && Objects.nonNull(caseData.getIsGaApplicantLip())
                && caseData.getIsGaApplicantLip().equals(YES);
    }

    public boolean isLipResp(CaseData caseData) {

        log.info(" ****************************************************************** ");
        log.info("-----feature Toggle caseData: {}", featureToggleService.isGaForLipsEnabled());
        log.info("---------isGaRespondentOneLip nonnull: {}", Objects.nonNull(caseData.getIsGaRespondentOneLip()));
        log.info("-------isGaRespondentOneLip: {}", caseData.getIsGaRespondentOneLip());
        log.info(" ****************************************************************** ");
        return featureToggleService.isGaForLipsEnabled()
                && Objects.nonNull(caseData.getIsGaRespondentOneLip())
                && caseData.getIsGaRespondentOneLip().equals(YES);
    }
}
