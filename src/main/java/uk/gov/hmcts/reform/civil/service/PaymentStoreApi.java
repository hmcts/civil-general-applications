package uk.gov.hmcts.reform.civil.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;

@FeignClient(name = "payments-api", url = "${payments.api.url}")
public interface PaymentStoreApi {

    @PostMapping(value = "/service-request", consumes = "application/json")
    PaymentServiceResponse createServiceRequest(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
        @RequestBody PaymentServiceRequest paymentRequest
    );

    @PostMapping(value = "/service-request/{service-request-reference}/pba-payments", consumes = "application/json")
    PaymentDto createPbaPayment(
        @PathVariable("service-request-reference") String serviceReqReference,
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
        @RequestBody ServiceRequestPaymentDto paymentRequest
    );
}
