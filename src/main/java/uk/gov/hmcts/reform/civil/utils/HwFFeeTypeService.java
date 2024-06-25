package uk.gov.hmcts.reform.civil.utils;

import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    public static BigDecimal getGaRemissionAmount(CaseData caseData) {
        if (Objects.nonNull(caseData.getGaHwfDetails())
                && Objects.nonNull(caseData.getGaHwfDetails().getRemissionAmount())) {
            return caseData.getGaHwfDetails().getRemissionAmount();
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal getAdditionalRemissionAmount(CaseData caseData) {
        if (Objects.nonNull(caseData.getAdditionalHwfDetails())
                && Objects.nonNull(caseData.getAdditionalHwfDetails().getRemissionAmount())) {
            return caseData.getAdditionalHwfDetails().getRemissionAmount();
        }
        return BigDecimal.ZERO;
    }

}
