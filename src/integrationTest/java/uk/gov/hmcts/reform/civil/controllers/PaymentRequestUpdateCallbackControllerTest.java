package uk.gov.hmcts.reform.civil.controllers;

import jakarta.servlet.ServletException;
import lombok.SneakyThrows;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.civil.model.ServiceRequestUpdateDto;
import uk.gov.hmcts.reform.civil.service.PaymentRequestUpdateCallbackService;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentRequestUpdateCallbackControllerTest extends BaseIntegrationTest {

    private static final String PAYMENT_CALLBACK_URL = "/service-request-update";
    private static final String CCD_CASE_NUMBER = "1234";
    private static final String PAID = "Paid";
    private static final String REFERENCE = "reference";
    private static final String ACCOUNT_NUMBER = "123445555";
    private static final String authToken = "Bearer TestAuthToken";
    private static final String s2sToken = "s2s AuthToken";

    @MockBean
    private PaymentRequestUpdateCallbackService requestUpdateCallbackService;

    @Test
    public void whenInvalidTypeOfRequestMade_ReturnMethodNotAllowed() throws Exception {
        doPost(buildServiceDto(), PAYMENT_CALLBACK_URL, "")
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void whenServiceRequestUpdateRequest() {
        doThrow(new RuntimeException("Payment failure")).when(requestUpdateCallbackService).processCallback(buildServiceDto());
        Exception e = assertThrows(ServletException.class,
            () -> doPut(buildServiceDto(), PAYMENT_CALLBACK_URL, "")
        );
        assertThat(e.getMessage()).contains("PaymentException");
    }

    @Test
    public void whenValidPaymentCallbackIsReceivedReturnSuccess() throws Exception {
        doPut(buildServiceDto(), PAYMENT_CALLBACK_URL, "")
            .andExpect(status().isOk());
    }

    @Test
    public void whenPaymentCallbackIsReceivedWithoutServiceAuthorisationReturn400() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.put(PAYMENT_CALLBACK_URL, "")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(buildServiceDto()))).andExpect(status().isBadRequest());
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

    @SneakyThrows
    protected <T> ResultActions doPut(T content, String urlTemplate, Object... uriVars) {
        return mockMvc.perform(
            MockMvcRequestBuilders.put(urlTemplate, uriVars)
                .header("ServiceAuthorization", s2sToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(content)));
    }

    @SneakyThrows
    protected <T> ResultActions doPost(T content, String urlTemplate, Object... uriVars) {
        return mockMvc.perform(
            MockMvcRequestBuilders.post(urlTemplate, uriVars)
                .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                .header("ServiceAuthorization", s2sToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(content)));
    }
}
