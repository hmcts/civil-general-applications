package uk.gov.hmcts.reform.civil.utils;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NO_REMISSION_HWF_GA;

import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.citizenui.HelpWithFees;

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

    public static CaseData updateOutstandingFee(CaseData caseData, String caseEventId) {
        var updatedData = caseData.toBuilder();
        BigDecimal gaRemissionAmount = NO_REMISSION_HWF_GA == CaseEvent.valueOf(caseEventId)
                ? BigDecimal.ZERO
                : getGaRemissionAmount(caseData);
        BigDecimal hearingRemissionAmount = NO_REMISSION_HWF_GA == CaseEvent.valueOf(caseEventId)
                ? BigDecimal.ZERO
                : getAdditionalRemissionAmount(caseData);
        BigDecimal feeAmount = getCalculatedFeeInPence(caseData);
        BigDecimal outstandingFeeAmount;

        if (caseData.isHWFTypeApplication() && BigDecimal.ZERO.compareTo(feeAmount) != 0) {
            outstandingFeeAmount = feeAmount.subtract(gaRemissionAmount);
            updatedData.gaHwfDetails(
                    caseData.getGaHwfDetails().toBuilder()
                            .remissionAmount(gaRemissionAmount)
                            .outstandingFeeInPounds(MonetaryConversions.penniesToPounds(outstandingFeeAmount))
                            .build()
            );
        } else if (caseData.isHWFTypeAdditional() && BigDecimal.ZERO.compareTo(feeAmount) != 0) {
            outstandingFeeAmount = feeAmount.subtract(hearingRemissionAmount);
            updatedData.additionalHwfDetails(
                    caseData.getAdditionalHwfDetails().toBuilder()
                            .remissionAmount(hearingRemissionAmount)
                            .outstandingFeeInPounds(MonetaryConversions.penniesToPounds(outstandingFeeAmount))
                            .build()
            );
        }
        return updatedData.build();
    }

    public static CaseData updateHwfReferenceNumber(CaseData caseData) {
        CaseData.CaseDataBuilder updatedData = caseData.toBuilder();

        if (Objects.nonNull(caseData.getFeePaymentOutcomeDetails())
                && caseData.getFeePaymentOutcomeDetails().getHwfNumberAvailable() == YesOrNo.YES) {
            HelpWithFees helpWithFees = HelpWithFees.builder()
                    .helpWithFee(YesOrNo.YES)
                    .helpWithFeesReferenceNumber(caseData.getFeePaymentOutcomeDetails().getHwfNumberForFeePaymentOutcome())
                    .build();
            updatedData.generalAppHelpWithFees(helpWithFees);
            clearHwfReferenceProperties(updatedData);
        }
        return updatedData.build();
    }

    private static void clearHwfReferenceProperties(CaseData.CaseDataBuilder caseDataBuilder) {
        CaseData caseData = caseDataBuilder.build();
        caseDataBuilder.feePaymentOutcomeDetails(caseData.getFeePaymentOutcomeDetails().toBuilder()
                .hwfNumberAvailable(null)
                .hwfNumberForFeePaymentOutcome(null).build());
    }
}
