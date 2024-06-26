package uk.gov.hmcts.reform.civil.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.citizenui.HelpWithFees;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesDetails;
import uk.gov.hmcts.reform.civil.utils.HwFFeeTypeService;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

public class HwFFeeTypeServiceTest {

    @Test
    void updateFeeType_shouldSetAdditionalFeeType_whenCaseStateIsApplicationAddPayment() {
        // Arrange
        CaseData caseData = CaseData.builder()
                .ccdState(CaseState.APPLICATION_ADD_PAYMENT)
                .generalAppHelpWithFees(HelpWithFees.builder().build())
                .build();

        // Act
        CaseData.CaseDataBuilder updatedCaseDataBuilder = HwFFeeTypeService.updateFeeType(caseData);

        // Assert
        assertThat(updatedCaseDataBuilder.build().getHwfFeeType()).isEqualTo(FeeType.ADDITIONAL);
    }

    @Test
    void updateFeeType_shouldSetApplicationFeeType_whenCaseStateIsNotApplicationAddPayment() {
        // Arrange
        CaseData caseData = CaseData.builder()
                .ccdState(CaseState.AWAITING_RESPONDENT_RESPONSE)
                .generalAppHelpWithFees(HelpWithFees.builder().build())
                .build();

        // Act
        CaseData.CaseDataBuilder updatedCaseDataBuilder = HwFFeeTypeService.updateFeeType(caseData);

        // Assert
        assertThat(updatedCaseDataBuilder.build().getHwfFeeType()).isEqualTo(FeeType.APPLICATION);
    }

    @Test
    void updateFeeType_shouldNotChangeFeeType_whenGeneralAppHelpWithFeesIsNull() {
        // Arrange
        CaseData caseData = CaseData.builder()
                .ccdState(CaseState.APPLICATION_ADD_PAYMENT)
                .build();

        // Act
        CaseData.CaseDataBuilder updatedCaseDataBuilder = HwFFeeTypeService.updateFeeType(caseData);

        // Assert
        assertThat(updatedCaseDataBuilder.build().getHwfFeeType()).isNull();
    }

    @Test
    void getCalculatedFeeInPence_shouldReturnFee_whenFeesIsNotNull() {
        // Arrange
        CaseData caseData = CaseData.builder()
                .generalAppPBADetails(GAPbaDetails.builder().fee(
                                Fee.builder()
                                        .calculatedAmountInPence(BigDecimal.valueOf(30000))
                                        .code("OOOCM002").build())
                        .build())
                .additionalHwfDetails(HelpWithFeesDetails.builder().build())
                .hwfFeeType(FeeType.ADDITIONAL)
                .build();

        // Act
        BigDecimal feeInPence = HwFFeeTypeService.getCalculatedFeeInPence(caseData);

        // Assert
        assertThat(feeInPence).isEqualTo(BigDecimal.valueOf(30000));
    }

    @Test
    void getCalculatedFeeInPence_shouldReturnZero_whenFeesIsNull() {
        // Arrange
        CaseData caseData = CaseData.builder()
                .build();

        // Act
        BigDecimal feeInPence = HwFFeeTypeService.getCalculatedFeeInPence(caseData);

        // Assert
        assertThat(feeInPence).isEqualTo(BigDecimal.ZERO);
    }
}
