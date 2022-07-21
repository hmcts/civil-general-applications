package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;

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
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.service.PaymentsService;
import uk.gov.hmcts.reform.civil.service.Time;
import uk.gov.hmcts.reform.payments.client.InvalidPaymentRequestException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAKE_PAYMENT_SERVICE_REQ_GASPEC;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceRequestHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(MAKE_PAYMENT_SERVICE_REQ_GASPEC);
    private static final String ERROR_MESSAGE = "Technical error occurred";
    private static final String TASK_ID = "GeneralApplicationMakePayment";
    public static final String DUPLICATE_PAYMENT_MESSAGE
        = "You attempted to retry the payment to soon. Try again later.";

    private final PaymentsService paymentsService;
    private final ObjectMapper objectMapper;
    private final Time time;

    @Override
    public String camundaActivityId(CallbackParams callbackParams) {
        return TASK_ID;
    }

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::makePaymentServiceReq
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse makePaymentServiceReq(CallbackParams callbackParams) {
        var caseData = callbackParams.getCaseData();
        var authToken = callbackParams.getParams().get(BEARER_TOKEN).toString();
        List<String> errors = new ArrayList<>();
        try {
            log.info("processing payment for case " + caseData.getCcdCaseReference());
            paymentsService.validateRequest(caseData);
            var serviceRequestReference = paymentsService.createPaymentServiceReq(caseData, authToken)
                                                                        .getServiceRequestReference();
            GAPbaDetails pbaDetails = caseData.getGeneralAppPBADetails();
            caseData = caseData.toBuilder()
                .generalAppPBADetails(GAPbaDetails.builder()
                                          .serviceReqReference(serviceRequestReference).build())
                .build();
        } catch (FeignException e) {
            log.info(String.format("Http Status %s ", e.status()), e);
            errors.add(ERROR_MESSAGE);
        } catch (InvalidPaymentRequestException e) {
            log.error(String.format("Error handling payment for general application: %s, response body: [%s]",
                                    caseData.getCcdCaseReference(), e.getMessage()
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .errors(errors)
            .build();
    }

}
