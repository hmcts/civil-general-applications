package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;

import java.time.LocalDateTime;

@Setter
@Data
@Builder(toBuilder = true)
public class GAPbaDetails {

    private final DynamicList applicantsPbaAccounts;
    private final String pbaReference;
    private final Fee fee;
    private final PaymentDetails paymentDetails;
    private final LocalDateTime paymentSuccessfulDate;
    private final String serviceReqReference;
    private final String additionalPaymentServiceRef;
    private final PaymentDetails additionalPaymentDetails;

    @JsonCreator
    GAPbaDetails(@JsonProperty("applicantsPbaAccounts") DynamicList applicantsPbaAccounts,
                 @JsonProperty("pbaReference") String pbaReference,
                 @JsonProperty("fee") Fee fee,
                 @JsonProperty("paymentDetails") PaymentDetails paymentDetails,
                 @JsonProperty("paymentSuccessfulDate") LocalDateTime paymentSuccessfulDate,
                 @JsonProperty("serviceRequestReference") String serviceReqReference) {
                 @JsonProperty("paymentSuccessfulDate") LocalDateTime paymentSuccessfulDate,
                 @JsonProperty("additionalPaymentServiceRef") String additionalPaymentServiceRef,
                 @JsonProperty("additionalPaymentDetails") PaymentDetails additionalPaymentDetails) {
        this.applicantsPbaAccounts = applicantsPbaAccounts;
        this.pbaReference = pbaReference;
        this.fee = fee;
        this.paymentDetails = paymentDetails;
        this.paymentSuccessfulDate = paymentSuccessfulDate;
        this.serviceReqReference = serviceReqReference;
        this.additionalPaymentServiceRef = additionalPaymentServiceRef;
        this.additionalPaymentDetails = additionalPaymentDetails;
    }
}
