package uk.gov.hmcts.reform.civil.utils;

import uk.gov.hmcts.reform.civil.model.CaseData;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

public class GaForLipService {

    private GaForLipService() {

    }

    public static boolean isGaForLip(CaseData caseData) {
        return caseData.getIsGaApplicantLip().equals(YES)
            || caseData.getIsGaRespondentOneLip().equals(YES)
            || (caseData.getIsMultiParty().equals(YES) && caseData.getIsGaRespondentTwoLip().equals(YES));

    }
}
