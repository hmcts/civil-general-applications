package uk.gov.hmcts.reform.civil.utils;

import uk.gov.hmcts.reform.civil.model.CaseData;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

import java.util.Objects;

public class GaForLipService {

    private GaForLipService() {

    }

    public static boolean isGaForLip(CaseData caseData) {
        return (Objects.nonNull(caseData.getIsGaApplicantLip())
                && caseData.getIsGaApplicantLip().equals(YES))
            || (Objects.nonNull(caseData.getIsGaRespondentOneLip())
                && caseData.getIsGaRespondentOneLip().equals(YES))
            || (caseData.getIsMultiParty().equals(YES)
                && Objects.nonNull(caseData.getIsGaRespondentTwoLip())
                && caseData.getIsGaRespondentTwoLip().equals(YES));

    }
}
