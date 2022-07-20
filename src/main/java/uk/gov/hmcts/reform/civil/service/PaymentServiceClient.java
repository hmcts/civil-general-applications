package uk.gov.hmcts.reform.civil.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.payments.client.PaymentsApi;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;
import uk.gov.hmcts.reform.payments.request.CreditAccountPaymentRequest;

@Service
@ConditionalOnProperty(prefix = "payments", name = "api.url")
public class PaymentServiceClient {
    private PaymentStoreApi paymentsApi;
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    public PaymentServiceClient (PaymentStoreApi paymentsApi, AuthTokenGenerator authTokenGenerator) {
        this.paymentsApi = paymentsApi;
        this.authTokenGenerator = authTokenGenerator;
    }

    public PaymentServiceResponse createServiceRequest(String authorisation, PaymentServiceRequest paymentRequest) {
        return paymentsApi.createServiceRequest(
            authorisation,
            authTokenGenerator.generate(),
            paymentRequest
        );
    }
}
