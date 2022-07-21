package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.service.PaymentsService;
import uk.gov.hmcts.reform.payments.client.InvalidPaymentRequestException;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.START_MAKE_ADDITIONAL_PAYMENT_REF;
import static uk.gov.hmcts.reform.civil.enums.PaymentStatus.FAILED;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.notificationCriterion;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.APPLICATION_CHANGE_TO_WITH_NOTICE;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdditionalPaymentsReferenceCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(START_MAKE_ADDITIONAL_PAYMENT_REF);
    private static final String ERROR_MESSAGE = "Technical error occurred";
    private static final String TASK_ID = "GeneralApplicationMakeAdditionalPayment";
    public static final String DUPLICATE_PAYMENT_MESSAGE
        = "You attempted to retry the payment to soon. Try again later.";

    private final PaymentsService paymentsService;
    private final ObjectMapper objectMapper;

    @Override
    public String camundaActivityId(CallbackParams callbackParams) {
        return TASK_ID;
    }

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::getAdditionalPaymentReference
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse getAdditionalPaymentReference(CallbackParams callbackParams) {

        var caseData = callbackParams.getCaseData();
        var authToken = callbackParams.getParams().get(BEARER_TOKEN).toString();
        List<String> errors = new ArrayList<>();
        if (notificationCriterion(caseData).equals(APPLICATION_CHANGE_TO_WITH_NOTICE)) {
            try {
                log.info("processing payment reference for case " + caseData.getCcdCaseReference());
                paymentsService.validateRequest(caseData);

                var paymentServiceRequest = paymentsService.createServiceRequestAdditionalPayment(caseData,
                                                                                                  authToken)
                    .getServiceRequestReference();
                GAPbaDetails paymentDetails = ofNullable(caseData.getGeneralAppPBADetails())
                    .map(GAPbaDetails::toBuilder)
                    .orElse(GAPbaDetails.builder())
                    .additionalPaymentServiceReqReference(paymentServiceRequest)
                    .build();
                caseData = caseData.toBuilder().generalAppPBADetails(paymentDetails).build();
            } catch (FeignException e) {
                log.info(String.format("Http Status %s ", e.status()), e);
                if (e.status() == 403) {
                    caseData = updateWithBusinessError(caseData, e);
                } else {
                    errors.add(ERROR_MESSAGE);
                }
            } catch (InvalidPaymentRequestException e) {
                log.error(String.format("Error handling payment for general application: %s, response body: [%s]",
                                        caseData.getCcdCaseReference(), e.getMessage()
                ));
                caseData = updateWithDuplicatePaymentError(caseData, e);
            }
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
           .data(caseData.toMap(objectMapper))
           .errors(errors)
           .build();
    }

    private CaseData updateWithBusinessError(CaseData caseData, FeignException e) {
        try {
            GAPbaDetails pbaDetails = caseData.getGeneralAppPBADetails();
            var paymentDto = objectMapper.readValue(e.contentUTF8(), PaymentDto.class);
            var statusHistory = paymentDto.getStatusHistories()[0];
            PaymentDetails paymentDetails = ofNullable(pbaDetails.getPaymentDetails())
                .map(PaymentDetails::toBuilder).orElse(PaymentDetails.builder())
                .customerReference(pbaDetails.getServiceReqReference())
                .status(FAILED)
                .errorCode(statusHistory.getErrorCode())
                .errorMessage(statusHistory.getErrorMessage())
                .build();

            return caseData.toBuilder().generalAppPBADetails(pbaDetails.toBuilder()
                                                                 .paymentDetails(paymentDetails).build())
                .build();
        } catch (JsonProcessingException jsonException) {
            log.error(jsonException.getMessage());
            log.error(String.format("Unknown payment error for case: %s, response body: %s",
                                    caseData.getCcdCaseReference(), e.contentUTF8()
            ));
            throw e;
        }
    }

    private CaseData updateWithDuplicatePaymentError(CaseData caseData, InvalidPaymentRequestException e) {
        GAPbaDetails pbaDetails = caseData.getGeneralAppPBADetails();
        var paymentDetails = ofNullable(pbaDetails.getPaymentDetails())
            .map(PaymentDetails::toBuilder)
            .orElse(PaymentDetails.builder())
            .customerReference(pbaDetails.getServiceReqReference())
            .status(FAILED)
            .errorCode(null)
            .errorMessage(DUPLICATE_PAYMENT_MESSAGE)
            .build();
        return caseData.toBuilder()
            .generalAppPBADetails(pbaDetails.toBuilder().paymentDetails(paymentDetails).build())
            .build();
    }

}
