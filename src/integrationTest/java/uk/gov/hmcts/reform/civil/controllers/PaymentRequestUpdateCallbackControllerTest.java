package uk.gov.hmcts.reform.civil.controllers;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.civil.model.ServiceRequestUpdateDto;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentRequestUpdateCallbackControllerTest extends BaseIntegrationTest {

    private static final String PAYMENT_CALLBACK_URL = "/payment-request-update";
    private static final String CCD_CASE_NUMBER = "1234";
    private static final String PAID = "Paid";
    private static final String REFERENCE = "reference";
    private static final String ACCOUNT_NUMBER = "123445555";

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
            .ccdCaseNumber(CCD_CASE_NUMBER)
            .serviceRequestStatus(PAID)
            .payment(PaymentDto.builder()
                         .amount(new BigDecimal(167))
                         .paymentReference(REFERENCE)
                         .caseReference(REFERENCE)
                         .accountNumber(ACCOUNT_NUMBER)
                         .build())
            .build();
    }
}
