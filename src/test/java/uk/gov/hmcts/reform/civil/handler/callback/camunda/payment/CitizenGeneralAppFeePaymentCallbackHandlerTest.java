package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.PaymentStatus;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CITIZEN_GENERAL_APP_PAYMENT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT;

@ExtendWith(MockitoExtension.class)
class CitizenGeneralAppFeePaymentCallbackHandlerTest extends BaseCallbackHandlerTest {

    private CitizenGeneralAppFeePaymentCallbackHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        handler = new CitizenGeneralAppFeePaymentCallbackHandler(objectMapper);
    }

    @Test
    void shouldTriggerInitiateGeneralApplicationAfterPaymentCamundaEvent() {
        CaseData caseData = CaseDataBuilder.builder().build();
        caseData = caseData.toBuilder().generalAppPBADetails(GAPbaDetails.builder()
                                                                 .paymentDetails(buildPaymentDetails()).build()).build();
        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        String camundaEvent = responseCaseData.getBusinessProcess().getCamundaEvent();

        assertThat(response.getErrors()).isNull();
        assertThat(camundaEvent).isEqualTo(INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT.name());
    }

    @Test
    void shouldNotTriggerInitiateGeneralApplicationAfterPaymentCamundaEvent() {
        CaseData caseData = CaseDataBuilder.builder().build();
        caseData = caseData.toBuilder().generalAppPBADetails(GAPbaDetails.builder()
                                                                 .paymentDetails(PaymentDetails.builder()
                                                                                     .status(PaymentStatus.FAILED)
                                                                                     .reference("R1234-1234-1234-1234")
                                                                                     .build()).build()).build();
        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response.getErrors()).isNull();
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

        assertThat(responseCaseData.getBusinessProcess()).isNull();
    }

    private PaymentDetails buildPaymentDetails() {
        return PaymentDetails.builder()
            .status(PaymentStatus.SUCCESS)
            .reference("R1234-1234-1234-1234")
            .build();

    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(CITIZEN_GENERAL_APP_PAYMENT);
    }
}
