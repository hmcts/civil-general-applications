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

@ExtendWith(MockitoExtension.class)
class CitizenGeneralAppFeePaymentCallbackHandlerTest extends BaseCallbackHandlerTest {

    private CitizenGeneralAppFeePaymentCallbackHandler handler;

    @BeforeEach
    void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        handler = new CitizenGeneralAppFeePaymentCallbackHandler(objectMapper);
    }

    @Test
    void citizenClaimIssuePayment() {
        CaseData caseData = CaseDataBuilder.builder().build();
        caseData = caseData.toBuilder().generalAppPBADetails(GAPbaDetails.builder()
                                                                 .paymentDetails(buildPaymentDetails()).build()).build();
        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response.getErrors()).isNull();
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
