package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CardPaymentStatusResponse;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;
import uk.gov.hmcts.reform.payments.request.CardPaymentServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.CardPaymentServiceRequestResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeesPaymentService {

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;
    private final PaymentStatusService paymentStatusService;
    private final UpdatePaymentStatusService updatePaymentStatusService;
    @Value("${cui-front-end.url}") String cuiFrontEndUrl;

    public CardPaymentStatusResponse createGovPaymentRequest(String caseReference, String authorization) {

        log.info("Creating gov Payment request url for caseId {}", caseReference);
        CaseDetails caseDetails = coreCaseDataService.getCase(Long.valueOf(caseReference));
        CaseData caseData = caseDetailsConverter.toCaseData(caseDetails);
        String parentCaseRef = caseData.getParentCaseReference();
        GAPbaDetails generalAppPbaDetails = caseData.getGeneralAppPBADetails();

        requireNonNull(generalAppPbaDetails, "Fee Payment details cannot be null");
        requireNonNull(generalAppPbaDetails.getServiceReqReference(), "Fee Payment service request cannot be null");

        String returnUrlSubPath = "/general-application/payment-confirmation/" + parentCaseRef + "/gaid/";

        CardPaymentServiceRequestDTO requestDto = CardPaymentServiceRequestDTO.builder()
            .amount(generalAppPbaDetails.getFee().getCalculatedAmountInPence()
                        .divide(BigDecimal.valueOf(100), RoundingMode.CEILING)
                        .setScale(2, RoundingMode.CEILING))
            .currency("GBP")
            .language("En")
            .returnUrl(cuiFrontEndUrl + returnUrlSubPath + caseReference)
            .build();
        CardPaymentServiceRequestResponse govPayCardPaymentRequest = paymentStatusService
            .createGovPayCardPaymentRequest(
                generalAppPbaDetails.getServiceReqReference(),
                authorization,
                requestDto
            );
        return CardPaymentStatusResponse.from(govPayCardPaymentRequest);
    }

    public CardPaymentStatusResponse getGovPaymentRequestStatus(String caseReference, String paymentReference, String authorization) {
        log.info("Checking payment status for {}", paymentReference);
        PaymentDto cardPaymentDetails = paymentStatusService.getCardPaymentDetails(paymentReference, authorization);
        String paymentStatus = cardPaymentDetails.getStatus();
        CardPaymentStatusResponse.CardPaymentStatusResponseBuilder response = CardPaymentStatusResponse.builder()
            .status(paymentStatus)
            .paymentReference(cardPaymentDetails.getReference())
            .externalReference(cardPaymentDetails.getPaymentGroupReference())
            .paymentAmount(cardPaymentDetails.getAmount());

        if (paymentStatus.equals("Failed")) {
            Arrays.asList(cardPaymentDetails.getStatusHistories()).stream()
                .filter(h -> h.getStatus().equals(paymentStatus))
                .findFirst()
                .ifPresent(h -> response.errorCode(h.getErrorCode()).errorDescription(h.getErrorMessage()));
        }

        try {
            updatePaymentStatusService.updatePaymentStatus(caseReference, response.build());

        } catch (Exception e) {

            log.error("Update payment status failed for claim [{}]", caseReference);
        }

        return response.build();
    }
}
