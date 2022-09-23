package uk.gov.hmcts.reform.civil.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceRequestUpdateDto {

    private String serviceRequestReference;
    private String ccdCaseNumber;
    private String serviceRequestAmount;
    private String serviceRequestStatus;
    private PaymentDto payment;

    @JsonCreator
    public ServiceRequestUpdateDto(@JsonProperty("service_request_reference") String serviceRequestReference,
                                   @JsonProperty("ccd_case_number") String ccdCaseNumber,
                                   @JsonProperty("service_request_amount") String serviceRequestAmount,
                                   @JsonProperty("service_request_status") String serviceRequestStatus,
                                   @JsonProperty("payment") PaymentDto payment) {
        this.serviceRequestReference = serviceRequestReference;
        this.ccdCaseNumber = ccdCaseNumber;
        this.serviceRequestAmount = serviceRequestAmount;
        this.serviceRequestStatus = serviceRequestStatus;
        this.payment = payment;
    }
}
