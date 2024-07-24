package uk.gov.hmcts.reform.civil.handler.callback.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.FEE_PAYMENT_OUTCOME_GA;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.citizenui.HelpWithFees;
import uk.gov.hmcts.reform.civil.model.genapplication.FeePaymentOutcomeDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesDetails;
import uk.gov.hmcts.reform.civil.service.PaymentRequestUpdateCallbackService;

import java.math.BigDecimal;

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

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(FEE_PAYMENT_OUTCOME_GA);
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
                            .outstandingFeeInPounds(BigDecimal.valueOf(100.00))
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
                            .hwfFullRemissionGrantedForAdditional(YesOrNo.YES).build())
                    .hwfFeeType(FeeType.ADDITIONAL)
                    .additionalHwfDetails(HelpWithFeesDetails.builder()
                            .outstandingFeeInPounds(BigDecimal.valueOf(100.00))
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
                    .gaHwfDetails(HelpWithFeesDetails.builder().build())
                    .generalAppHelpWithFees(HelpWithFees.builder().helpWithFeesReferenceNumber("ref").build())
                    .hwfFeeType(FeeType.APPLICATION)
                    .build();
            when(service.processHwf(any(CaseData.class)))
                    .thenAnswer((Answer<CaseData>) invocation -> invocation.getArgument(0));

            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            verify(service, times(1)).processHwf(any());
            assertThat(updatedData.getBusinessProcess().getCamundaEvent()).isEqualTo(INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT.toString());
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
            assertThat(updatedData.getBusinessProcess().getCamundaEvent()).isEqualTo(MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID.toString());
        }
    }
}
