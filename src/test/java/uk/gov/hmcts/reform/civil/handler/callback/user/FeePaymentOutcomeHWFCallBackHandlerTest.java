package uk.gov.hmcts.reform.civil.handler.callback.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.FEE_PAYMENT_OUTCOME_GA;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_COSC_APPLICATION_AFTER_PAYMENT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_GA_ADD_HWF;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.citizenui.HelpWithFees;
import uk.gov.hmcts.reform.civil.model.genapplication.FeePaymentOutcomeDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesDetails;
import uk.gov.hmcts.reform.civil.service.HwfNotificationService;
import uk.gov.hmcts.reform.civil.service.PaymentRequestUpdateCallbackService;

import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    FeePaymentOutcomeHWFCallBackHandler.class,
    JacksonAutoConfiguration.class
})
public class FeePaymentOutcomeHWFCallBackHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private FeePaymentOutcomeHWFCallBackHandler handler;
    @Autowired
    private ObjectMapper mapper = new ObjectMapper();
    @MockBean
    private PaymentRequestUpdateCallbackService service;
    @MockBean
    private HwfNotificationService hwfNotificationService;
    @MockBean
    private FeatureToggleService featureToggleService;

    @BeforeEach
    void setup() {
        when(featureToggleService.isCoSCEnabled()).thenReturn(false);
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(FEE_PAYMENT_OUTCOME_GA);
    }

    @Nested
    class AboutToStartCallbackHandling {

        @Test
        void updateFeeType_shouldSetAdditionalFeeTypeWithEmptyRef_whenCaseStateIsApplicationAddPayment() {
            // Arrange
            CaseData caseData = CaseData.builder()
                .ccdState(CaseState.APPLICATION_ADD_PAYMENT)
                .generalAppHelpWithFees(HelpWithFees.builder().build())
                .hwfFeeType(FeeType.ADDITIONAL)
                .generalAppPBADetails(GAPbaDetails.builder().fee(
                    Fee.builder()
                        .calculatedAmountInPence(BigDecimal.valueOf(180))
                        .code("FEE123").build()).build())
                .build();

            // Act
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_START);
            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);
            // Assert
            assertThat(updatedData.getHwfFeeType()).isEqualTo(FeeType.ADDITIONAL);
            assertThat(updatedData.getFeePaymentOutcomeDetails().getHwfNumberAvailable()).isEqualTo(YesOrNo.NO);
        }

        @Test
        void updateFeeType_shouldSetAdditionalFeeTypeWithRef_whenCaseStateIsApplicationAddPayment() {
            // Arrange
            CaseData caseData = CaseData.builder()
                .ccdState(CaseState.APPLICATION_ADD_PAYMENT)
                .hwfFeeType(FeeType.ADDITIONAL)
                .generalAppHelpWithFees(HelpWithFees.builder().helpWithFeesReferenceNumber("123").build())
                .build();

            // Act
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_START);
            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);
            // Assert
            assertThat(updatedData.getHwfFeeType()).isEqualTo(FeeType.ADDITIONAL);
            assertThat(updatedData.getFeePaymentOutcomeDetails().getHwfNumberAvailable()).isEqualTo(YesOrNo.YES);
            assertThat(updatedData.getFeePaymentOutcomeDetails().getHwfNumberForFeePaymentOutcome()).isEqualTo("123");
        }

        @Test
        void updateFeeType_shouldSetApplicationFeeTypeWithEmptyRef_whenCaseStateIsNotApplicationAddPayment() {
            // Arrange
            CaseData caseData = CaseData.builder()
                .ccdState(CaseState.AWAITING_RESPONDENT_RESPONSE)
                .hwfFeeType(FeeType.APPLICATION)
                .generalAppHelpWithFees(HelpWithFees.builder().build())
                .generalAppPBADetails(GAPbaDetails.builder().fee(
                    Fee.builder()
                        .calculatedAmountInPence(BigDecimal.valueOf(180))
                        .code("FEE123").build()).build())
                .build();

            // Act
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_START);
            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            // Assert
            assertThat(updatedData.getHwfFeeType()).isEqualTo(FeeType.APPLICATION);
            assertThat(updatedData.getFeePaymentOutcomeDetails().getHwfNumberAvailable()).isEqualTo(YesOrNo.NO);
        }

        @Test
        void updateFeeType_shouldSetApplicationFeeTypeWithRef_whenCaseStateIsNotApplicationAddPayment() {
            // Arrange
            CaseData caseData = CaseData.builder()
                .ccdState(CaseState.AWAITING_RESPONDENT_RESPONSE)
                .hwfFeeType(FeeType.APPLICATION)
                .generalAppHelpWithFees(HelpWithFees.builder().helpWithFeesReferenceNumber("123").build())
                .build();

            // Act
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_START);
            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            // Assert
            assertThat(updatedData.getHwfFeeType()).isEqualTo(FeeType.APPLICATION);
            assertThat(updatedData.getFeePaymentOutcomeDetails().getHwfNumberAvailable()).isEqualTo(YesOrNo.YES);
            assertThat(updatedData.getFeePaymentOutcomeDetails().getHwfNumberForFeePaymentOutcome()).isEqualTo("123");
        }
    }

    @Nested
    class MidCallback {

        @Test
        void shouldValidationFeePaymentOutcomeGa_withInvalidOutstandingFee() {
            //Given
            CaseData caseData = CaseData.builder()
                    .feePaymentOutcomeDetails(FeePaymentOutcomeDetails.builder().hwfNumberAvailable(YesOrNo.YES)
                            .hwfNumberForFeePaymentOutcome("HWF-1C4-E34")
                            .hwfFullRemissionGrantedForGa(YesOrNo.YES).build())
                    .hwfFeeType(FeeType.APPLICATION)
                    .gaHwfDetails(HelpWithFeesDetails.builder()
                            .outstandingFee(BigDecimal.valueOf(100.00))
                            .build())
                    .build();
            //When
            CallbackParams params = callbackParamsOf(caseData, MID, "remission-type");
            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            //Then
            Assertions.assertThat(response.getErrors()).containsExactly("Incorrect remission type selected");
        }

        @Test
        void shouldValidationFeePaymentOutcomeAdditional_withInvalidOutstandingFee() {
            //Given
            CaseData caseData = CaseData.builder()
                    .feePaymentOutcomeDetails(FeePaymentOutcomeDetails.builder().hwfNumberAvailable(YesOrNo.YES)
                            .hwfNumberForFeePaymentOutcome("HWF-1C4-E34")
                            .hwfFullRemissionGrantedForAdditionalFee(YesOrNo.YES).build())
                    .hwfFeeType(FeeType.ADDITIONAL)
                    .additionalHwfDetails(HelpWithFeesDetails.builder()
                            .outstandingFee(BigDecimal.valueOf(100.00))
                            .build())
                    .build();
            //When
            CallbackParams params = callbackParamsOf(caseData, MID, "remission-type");
            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            //Then
            Assertions.assertThat(response.getErrors()).containsExactly("Incorrect remission type selected");
        }
    }

    @Nested
    class AboutToSubmitCallback {
        @Test
        void shouldTrigger_after_payment_GaFee() {
            CaseData caseData = CaseData.builder()
                    .generalAppPBADetails(GAPbaDetails.builder().fee(
                                    Fee.builder()
                                            .calculatedAmountInPence(BigDecimal.valueOf(10000)).code("OOOCM002").build())
                            .build())
                    .generalAppHelpWithFees(HelpWithFees.builder().helpWithFeesReferenceNumber("ref").build())
                    .gaHwfDetails(HelpWithFeesDetails.builder().build())
                    .hwfFeeType(FeeType.APPLICATION)
                    .build();
            when(service.processHwf(any(CaseData.class)))
                    .thenAnswer((Answer<CaseData>) invocation -> invocation.getArgument(0));

            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            verify(service, times(1)).processHwf(any());
            verify(hwfNotificationService, times(1)).sendNotification(any(), eq(FEE_PAYMENT_OUTCOME_GA));
            assertThat(updatedData.getBusinessProcess().getCamundaEvent()).isEqualTo(INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT.toString());
        }

        @Test
        void shouldTrigger_after_payment_GaFee_shouldTriggerCosc() {
            CaseData caseData = CaseData.builder()
                    .generalAppType(GAApplicationType.builder()
                                        .types(List.of(GeneralApplicationTypes.CONFIRM_CCJ_DEBT_PAID))
                                        .build())
                    .generalAppPBADetails(GAPbaDetails.builder().fee(
                                    Fee.builder()
                                            .calculatedAmountInPence(BigDecimal.valueOf(10000)).code("OOOCM002").build())
                            .build())
                    .generalAppHelpWithFees(HelpWithFees.builder().helpWithFeesReferenceNumber("ref").build())
                    .gaHwfDetails(HelpWithFeesDetails.builder().build())
                    .hwfFeeType(FeeType.APPLICATION)
                    .build();
            when(featureToggleService.isCoSCEnabled()).thenReturn(true);
            when(service.processHwf(any(CaseData.class)))
                    .thenAnswer((Answer<CaseData>) invocation -> invocation.getArgument(0));

            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            verify(service, times(1)).processHwf(any());
            verify(hwfNotificationService, times(1)).sendNotification(any(), eq(FEE_PAYMENT_OUTCOME_GA));
            assertThat(updatedData.getBusinessProcess().getCamundaEvent()).isEqualTo(INITIATE_COSC_APPLICATION_AFTER_PAYMENT.toString());
        }

        @Test
        void shouldTrigger_modify_state_additioanlFee() {
            CaseData caseData = CaseData.builder()
                    .generalAppPBADetails(GAPbaDetails.builder().fee(
                                    Fee.builder()
                                            .calculatedAmountInPence(BigDecimal.valueOf(10000)).code("OOOCM002").build())
                            .build())
                    .generalAppHelpWithFees(HelpWithFees.builder().helpWithFeesReferenceNumber("ref").build())
                    .gaHwfDetails(HelpWithFeesDetails.builder().build())
                    .additionalHwfDetails(HelpWithFeesDetails.builder().build())
                    .hwfFeeType(FeeType.ADDITIONAL)
                    .build();
            when(service.processHwf(any(CaseData.class)))
                    .thenAnswer((Answer<CaseData>) invocation -> invocation.getArgument(0));

            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            verify(service, times(1)).processHwf(any());
            verify(hwfNotificationService, times(1)).sendNotification(any(), eq(FEE_PAYMENT_OUTCOME_GA));
            assertThat(updatedData.getBusinessProcess().getCamundaEvent()).isEqualTo(UPDATE_GA_ADD_HWF.toString());
        }
    }
}
