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
    private final String serviceReqReference;
    private final Fee fee;
    private final PaymentDetails paymentDetails;
    private final LocalDateTime paymentSuccessfulDate;
    private final String additionalPaymentServiceRef;
    private final Fee additionalUncloakFee;

    @JsonCreator
    GAPbaDetails(@JsonProperty("applicantsPbaAccounts") DynamicList applicantsPbaAccounts,
                 @JsonProperty("pbaReference") String serviceReqReference,
                 @JsonProperty("fee") Fee fee,
                 @JsonProperty("paymentDetails") PaymentDetails paymentDetails,
                 @JsonProperty("paymentSuccessfulDate") LocalDateTime paymentSuccessfulDate,
                 @JsonProperty("additionalPaymentServiceRef") String additionalPaymentServiceRef,
                 @JsonProperty("additionalUncloakFee") Fee additionalUncloakFee) {
        this.applicantsPbaAccounts = applicantsPbaAccounts;
        this.serviceReqReference = serviceReqReference;
        this.fee = fee;
        this.paymentDetails = paymentDetails;
        this.paymentSuccessfulDate = paymentSuccessfulDate;
        this.additionalPaymentServiceRef = additionalPaymentServiceRef;
        this.additionalUncloakFee = additionalUncloakFee;
    }
}
