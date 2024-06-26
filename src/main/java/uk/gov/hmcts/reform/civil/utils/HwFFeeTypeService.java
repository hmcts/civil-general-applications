package uk.gov.hmcts.reform.civil.utils;

import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.math.BigDecimal;
import java.util.Objects;

public class HwFFeeTypeService {

    private HwFFeeTypeService() {
    }

    public static CaseData.CaseDataBuilder updateFeeType(CaseData caseData) {
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        if (Objects.nonNull(caseData.getGeneralAppHelpWithFees())
            && Objects.isNull(caseData.getHwfFeeType())) {
            if (caseData.getCcdState().equals(CaseState.APPLICATION_ADD_PAYMENT)) {
                caseDataBuilder.hwfFeeType(FeeType.ADDITIONAL);
            } else {
                caseDataBuilder.hwfFeeType(FeeType.APPLICATION);
            }
        }
        return caseDataBuilder;
    }


    public static BigDecimal getCalculatedFeeInPence(CaseData caseData) {
        if (Objects.nonNull(caseData.getGeneralAppPBADetails())
                && Objects.nonNull(caseData.getGeneralAppPBADetails().getFee())) {
            return caseData.getGeneralAppPBADetails().getFee().getCalculatedAmountInPence();
        }
        return BigDecimal.ZERO;
    }

}
