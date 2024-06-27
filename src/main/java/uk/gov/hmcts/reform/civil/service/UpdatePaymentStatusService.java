package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.PaymentStatus;
import uk.gov.hmcts.reform.civil.exceptions.CaseDataUpdateException;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CardPaymentStatusResponse;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;

import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdatePaymentStatusService {

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;
    private final ObjectMapper objectMapper;

    @Retryable(value = CaseDataUpdateException.class, maxAttempts = 3, backoff = @Backoff(delay = 500))
    public void updatePaymentStatus(String caseReference, CardPaymentStatusResponse cardPaymentStatusResponse) {

        try {
            CaseDetails caseDetails = coreCaseDataService.getCase(Long.valueOf(caseReference));
            CaseData caseData = caseDetailsConverter.toCaseData(caseDetails);
            caseData = updateCaseDataWithStateAndPaymentDetails(cardPaymentStatusResponse, caseData);

            createEvent(caseData, caseReference);
        } catch (Exception ex) {
            throw new CaseDataUpdateException();
        }
    }

    private void createEvent(CaseData caseData, String caseReference) {

        StartEventResponse startEventResponse = coreCaseDataService.startUpdate(
            caseReference,
            INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT
        );

        CaseDataContent caseDataContent = buildCaseDataContent(
            startEventResponse,
            caseData
        );

        coreCaseDataService.submitUpdate(caseReference, caseDataContent);
    }

    private CaseDataContent buildCaseDataContent(StartEventResponse startEventResponse, CaseData caseData) {

        Map<String, Object> updatedData = caseData.toMap(objectMapper);
        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder().id(startEventResponse.getEventId())
                       .summary(null)
                       .description(null)
                       .build())
            .data(updatedData)
            .build();
    }

    private CaseData updateCaseDataWithStateAndPaymentDetails(CardPaymentStatusResponse cardPaymentStatusResponse,
                                                              CaseData caseData) {

        GAPbaDetails pbaDetails = caseData.getGeneralAppPBADetails();
        GAPbaDetails.GAPbaDetailsBuilder pbaDetailsBuilder;
        pbaDetailsBuilder = pbaDetails == null ? GAPbaDetails.builder() : pbaDetails.toBuilder();

        PaymentDetails paymentDetails = PaymentDetails.builder()
            .status(PaymentStatus.valueOf(cardPaymentStatusResponse.getStatus().toUpperCase()))
            .reference(cardPaymentStatusResponse.getPaymentReference())
            .errorCode(cardPaymentStatusResponse.getErrorCode())
            .errorMessage(cardPaymentStatusResponse.getErrorDescription())
            .build();

        pbaDetails = pbaDetailsBuilder.paymentDetails(paymentDetails).build();
        return caseData.toBuilder()
            .generalAppPBADetails(pbaDetails)
            .build();
    }

}
