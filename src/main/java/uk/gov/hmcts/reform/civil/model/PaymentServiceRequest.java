package uk.gov.hmcts.reform.civil.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.civil.model.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;

@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentServiceRequest {

    @JsonProperty("call_back_url")
    private String callBackUrl;
    @JsonProperty("case_payment_request")
    private CasePaymentRequestDto casePaymentRequest;
    @JsonProperty("case_reference")
    private String caseReference;
    @JsonProperty("ccd_case_number")
    private String ccdCaseNumber;
    @JsonProperty("fees")
    private FeeDto[] fees;
    @JsonProperty("organisation_id")
    private String organisationId;

}
