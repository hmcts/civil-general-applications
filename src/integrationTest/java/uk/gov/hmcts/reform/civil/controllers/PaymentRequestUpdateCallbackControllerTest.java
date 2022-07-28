package uk.gov.hmcts.reform.civil.controllers;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.civil.model.ServiceRequestUpdateDto;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentRequestUpdateCallbackControllerTest extends BaseIntegrationTest {

    private static final String PAYMENT_CALLBACK_URL = "/payment-request-update";

    @Test
    public void whenInvalidTypeOfRequestMade_ReturnMethodNotAllowed() throws Exception {

        doPost(BEARER_TOKEN, buildServiceDto(), PAYMENT_CALLBACK_URL, "")
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void whenServiceRequestUpdateRequest() throws Exception {

        doPut(BEARER_TOKEN, buildServiceDto(), PAYMENT_CALLBACK_URL, "")
            .andExpect(status().isOk());
    }

    private ServiceRequestUpdateDto buildServiceDto() {
        return ServiceRequestUpdateDto.builder()
            .ccdCaseNumber("1234")
            .serviceRequestStatus("Paid")
            .payment(PaymentDto.builder()
                         .amount(new BigDecimal(167))
                         .paymentReference("reference")
                         .caseReference("reference")
                         .accountNumber("123445555")
                         .build())
            .build();
    }
}
