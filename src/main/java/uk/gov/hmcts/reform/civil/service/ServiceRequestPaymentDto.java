package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceRequestPaymentDto {

    @JsonProperty("account_number")
    private String accountNumber;
    @JsonProperty("amount")
    private BigDecimal amount;
    @Builder.Default
    @JsonProperty("currency")
    private String currency = "GBP";
    @JsonProperty("customer_reference")
    private String customerReference;
    @JsonProperty("idempotency_key")
    private String idempotencyKey;
    @JsonProperty("organisationName")
    private String organisationName;
}
