package uk.gov.hmcts.reform.civil.controllers;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.civil.model.ServiceRequestUpdateDto;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentRequestUpdateCallbackControllerTest extends BaseIntegrationTest {

    private static final String PAYMENT_CALLBACK_URL = "/payment-request-update";
    private static final String CCD_CASE_NUMBER = "1234";
    private static final String PAID = "Paid";
    private static final String REFERENCE = "reference";
    private static final String ACCOUNT_NUMBER = "123445555";


//    @MockBean
//    ServiceAuthCustomFilter serviceAuthFilter;
    protected static final String SERVICE_BEARER_TOKER = "Bearer eyJhbGciOiJIUzUxMiJ9" +
        ".eyJzdWIiOiJwYXltZW50X2FwcCIsImV4cCI6MTY1OTk3MTYzMX0" +
        ".3Qb7-rSkf2QG4XjxQuO5EUC92dQ8qjRilHKXsfLnbkBECIFQ_8g2berDodzmat8W0moN3bd55p9bthxHcQ46gQ";

    @Test
    public void whenInvalidTypeOfRequestMade_ReturnMethodNotAllowed() throws Exception {

        when(serviceAuthorisationApi.getServiceName(any())).thenReturn("payment_app");
        doPost(SERVICE_BEARER_TOKER,buildServiceDto(), PAYMENT_CALLBACK_URL, "")
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void whenServiceRequestUpdateRequest() throws Exception {

        when(serviceAuthorisationApi.getServiceName(any())).thenReturn("payment_app");

        doPut(SERVICE_BEARER_TOKER,buildServiceDto(), PAYMENT_CALLBACK_URL, "")
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

    @SneakyThrows
    protected <T> ResultActions doPut(String serviceAUth, T content, String urlTemplate, Object... uriVars) {
        return mockMvc.perform(
            MockMvcRequestBuilders.put(urlTemplate, uriVars)
                .header(ServiceAuthFilter.AUTHORISATION, serviceAUth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(content)));
    }

    @SneakyThrows
    protected <T> ResultActions doPost(String serviceAUth, T content, String urlTemplate, Object... uriVars) {
        return mockMvc.perform(
            MockMvcRequestBuilders.post(urlTemplate, uriVars)
                .header(ServiceAuthFilter.AUTHORISATION, serviceAUth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(content)));
    }
}
