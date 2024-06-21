package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.config.PaymentsConfiguration;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.payments.client.InvalidPaymentRequestException;
import uk.gov.hmcts.reform.payments.client.PaymentsClient;
import uk.gov.hmcts.reform.payments.client.models.CasePaymentRequestDto;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;
import uk.gov.hmcts.reform.payments.request.PBAServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.PBAServiceRequestResponse;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.civil.model.Organisation;

import java.util.UUID;

import static org.apache.commons.lang.StringUtils.isBlank;

@Service
@RequiredArgsConstructor
public class PaymentsService {

    private final PaymentsClient paymentsClient;
    private final PaymentsConfiguration paymentsConfiguration;
    private final OrganisationService organisationService;
    @Value("${payments.api.callback-url}")
    String callBackUrl;
    public static final String PAYMENT_ACTION = "payment";
    public static final String SPEC_CLAIM = "SPEC_CLAIM";

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

    public PBAServiceRequestResponse createCreditAccountPayment(CaseData caseData, String authToken) {
        String serviceReqReference = caseData.getGeneralAppPBADetails().getServiceReqReference();
        return paymentsClient.createPbaPayment(serviceReqReference, authToken, buildRequest(caseData));
    }

    public PaymentServiceResponse createServiceRequest(CaseData caseData, String authToken) {
        return paymentsClient.createServiceRequest(authToken, buildServiceRequest(caseData));
    }

    private CreateServiceRequestDTO buildServiceRequest(CaseData caseData) {
        GAPbaDetails generalAppPBADetails = caseData.getGeneralAppPBADetails();
        FeeDto feeResponse = generalAppPBADetails.getFee().toFeeDto();
        String siteId = caseData.getGeneralAppSuperClaimType().equals(SPEC_CLAIM)
            ? paymentsConfiguration.getSpecSiteId() : paymentsConfiguration.getSiteId();

        return CreateServiceRequestDTO.builder()
            .callBackUrl(callBackUrl)
            .casePaymentRequest(CasePaymentRequestDto.builder()
                                    .action(PAYMENT_ACTION)
                                    .responsibleParty(caseData.getApplicantPartyName()).build())
            .caseReference(caseData.getCcdCaseReference().toString())
            .ccdCaseNumber(caseData.getCcdCaseReference().toString())
            .fees(new FeeDto[] { (FeeDto.builder()
                .calculatedAmount(feeResponse.getCalculatedAmount())
                .code(feeResponse.getCode())
                .version(feeResponse.getVersion())
                .volume(1).build())})
            .hmctsOrgId(siteId).build();
    }

    private PBAServiceRequestDTO buildRequest(CaseData caseData) {
        GAPbaDetails generalAppPBADetails = caseData.getGeneralAppPBADetails();
        FeeDto claimFee = generalAppPBADetails.getFee().toFeeDto();
        var organisationId = caseData.getGeneralAppApplnSolicitor().getOrganisationIdentifier();
        var organisationName = organisationService.findOrganisationById(organisationId)
            .map(Organisation::getName)
            .orElseThrow(RuntimeException::new);

        return PBAServiceRequestDTO.builder()
            .amount(claimFee.getCalculatedAmount())
            .customerReference(generalAppPBADetails.getServiceReqReference())
            .organisationName(organisationName)
            .idempotencyKey(String.valueOf(UUID.randomUUID()))
            .build();
    }

}
