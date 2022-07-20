package uk.gov.hmcts.reform.civil.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.civil.service.PaymentsService;
import uk.gov.hmcts.reform.payments.client.health.InternalHealth;
import uk.gov.hmcts.reform.payments.request.CreditAccountPaymentRequest;


@FeignClient(name = "payments-api", url = "${payments.api.url}")
public interface PaymentStoreApi {

    @PostMapping(value = "/service-request", consumes = "application/json")
    PaymentServiceResponse createServiceRequest(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
        @RequestBody PaymentServiceRequest paymentRequest
    );
}
