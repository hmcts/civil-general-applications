package uk.gov.hmcts.reform.civil.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.civil.model.CreateServiceRequest;
import uk.gov.hmcts.reform.civil.model.PBAServiceRequestResponse;
import uk.gov.hmcts.reform.civil.model.PaymentServiceResponse;
import uk.gov.hmcts.reform.civil.model.ServiceRequestPaymentDto;

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

    public PaymentServiceResponse createServiceRequest(String authorisation, CreateServiceRequest paymentRequest) {
        return paymentsApi.createServiceRequest(
            authorisation,
            authTokenGenerator.generate(),
            paymentRequest
        );
    }

    public PBAServiceRequestResponse createPbaPayment(String serviceReqReference, String authorisation,
                                                      ServiceRequestPaymentDto paymentRequest) {
        return paymentsApi.createPbaPayment(
            serviceReqReference,
            authorisation,
            authTokenGenerator.generate(),
            paymentRequest
        );
    }
}
