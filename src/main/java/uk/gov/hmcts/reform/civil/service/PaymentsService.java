package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.config.PaymentsConfiguration;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.CasePaymentRequestDto;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.PaymentServiceRequest;
import uk.gov.hmcts.reform.civil.model.PaymentServiceResponse;
import uk.gov.hmcts.reform.civil.model.ServiceRequestPaymentDto;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.payments.client.InvalidPaymentRequestException;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;
import uk.gov.hmcts.reform.prd.model.Organisation;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.isBlank;

@Service
@RequiredArgsConstructor
public class PaymentsService {

    private final PaymentServiceClient paymentServiceClient;
    private final PaymentsConfiguration paymentsConfiguration;
    private final OrganisationService organisationService;
    public static final String PAYMENT_ACTION = "payment";

    public void validateRequest(CaseData caseData) {
        String error = null;
        GAPbaDetails generalAppPBADetails = caseData.getGeneralAppPBADetails();
        if (generalAppPBADetails == null) {
            error = "PBA details not received.";
        } else if (generalAppPBADetails.getFee() == null
            || generalAppPBADetails.getFee().getCalculatedAmountInPence() == null
            || isBlank(generalAppPBADetails.getFee().getVersion())
            || isBlank(generalAppPBADetails.getFee().getCode())) {
            error = "Fees are not set correctly.";
        }
        if (caseData.getGeneralAppApplnSolicitor() == null
                || isBlank(caseData.getGeneralAppApplnSolicitor().getOrganisationIdentifier())) {
            error = "Applicant's organization details not received.";
        }
        if (!isBlank(error)) {
            throw new InvalidPaymentRequestException(error);
        }
    }

    public PaymentDto createCreditAccountPayment(CaseData caseData, String authToken) {
        String serviceReqReference = caseData.getGeneralAppPBADetails().getServiceReqReference();
        return paymentServiceClient.createPbaPayment(serviceReqReference, authToken, buildRequest(caseData));
    }

    public PaymentServiceResponse createPaymentServiceReq(CaseData caseData, String authToken) {
        return paymentServiceClient.createServiceRequest(authToken, buildServiceRequest(caseData));
    }

    private PaymentServiceRequest buildServiceRequest(CaseData caseData) {
        GAPbaDetails generalAppPBADetails = caseData.getGeneralAppPBADetails();
        FeeDto feeResponse = generalAppPBADetails.getFee().toFeeDto();
        return PaymentServiceRequest.builder()
            .callBackUrl(paymentsConfiguration.getPayApiCallBackUrl())
            .casePaymentRequest(CasePaymentRequestDto.builder()
                                    .action(PAYMENT_ACTION)
                                    .responsibleParty(caseData.getApplicantPartyName()).build())
            .caseReference(caseData.getLegacyCaseReference())
            .ccdCaseNumber(caseData.getCcdCaseReference().toString())
            .fees(new FeeDto[] { (FeeDto.builder()
                .calculatedAmount(feeResponse.getCalculatedAmount())
                .code(feeResponse.getCode())
                .version(feeResponse.getVersion())
                .volume(1).build())})
            .organisationId(paymentsConfiguration.getSiteId()).build();
    }

    private ServiceRequestPaymentDto buildRequest(CaseData caseData) {
        GAPbaDetails generalAppPBADetails = caseData.getGeneralAppPBADetails();
        FeeDto claimFee = generalAppPBADetails.getFee().toFeeDto();
        var organisationId = caseData.getGeneralAppApplnSolicitor().getOrganisationIdentifier();
        var organisationName = organisationService.findOrganisationById(organisationId)
            .map(Organisation::getName)
            .orElseThrow(RuntimeException::new);

        String customerReference = ofNullable(generalAppPBADetails.getPaymentDetails())
            .map(PaymentDetails::getCustomerReference)
            .orElse(generalAppPBADetails.getServiceReqReference());

        return ServiceRequestPaymentDto.builder()
            .accountNumber(generalAppPBADetails.getApplicantsPbaAccounts()
                    .getValue().getLabel())
            .amount(claimFee.getCalculatedAmount())
            .customerReference(customerReference)
            .organisationName(organisationName)
            .build();
    }

    public PaymentServiceResponse createServiceRequestAdditionalPayment(CaseData caseData, String authToken)  {
        return paymentServiceClient.createServiceRequest(authToken, buildAdditionalPaymentRequest(caseData));
    }

    private PaymentServiceRequest buildAdditionalPaymentRequest(CaseData caseData) {
        GAPbaDetails generalAppPBADetails = caseData.getGeneralAppPBADetails();
        Fee fee = generalAppPBADetails.getFee();
        return PaymentServiceRequest.builder()
            .callBackUrl(paymentsConfiguration.getPayApiCallBackUrl())
            .casePaymentRequest(CasePaymentRequestDto.builder()
                                    .action(PAYMENT_ACTION)
                                    .responsibleParty(caseData.getApplicantPartyName()).build())
            .caseReference(caseData.getLegacyCaseReference())
            .ccdCaseNumber(caseData.getCcdCaseReference().toString())
            .fees(new FeeDto[] { (FeeDto.builder()
                .calculatedAmount(fee.getCalculatedAmountInPence())
                .code(fee.getCode())
                .version(fee.getVersion())
                .volume(1).build())})
            .build();
    }
}
