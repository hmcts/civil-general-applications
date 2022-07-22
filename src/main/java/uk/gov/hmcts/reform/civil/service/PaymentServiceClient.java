package uk.gov.hmcts.reform.civil.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.civil.model.PaymentServiceRequest;
import uk.gov.hmcts.reform.civil.model.PaymentServiceResponse;
import uk.gov.hmcts.reform.civil.model.ServiceRequestPaymentDto;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;

@Service
@ConditionalOnProperty(prefix = "payments", name = "api.url")
public class PaymentServiceClient {

    private PaymentStoreApi paymentsApi;
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    public PaymentServiceClient(PaymentStoreApi paymentsApi, AuthTokenGenerator authTokenGenerator) {
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

    public PaymentDto createPbaPayment(String serviceReqReference, String authorisation,
                                       ServiceRequestPaymentDto paymentRequest) {
        return paymentsApi.createPbaPayment(
            serviceReqReference,
            authorisation,
            authTokenGenerator.generate(),
            paymentRequest
        );
    }
}
