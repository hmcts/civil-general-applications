package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.PaymentStatus;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.service.GaForLipService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_COSC_APPLICATION_AFTER_PAYMENT;

@Service
@RequiredArgsConstructor
public class GeneralApplicationAfterPaymentCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = singletonList(INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT);
    private final ObjectMapper objectMapper;
    private final GaForLipService gaForLipService;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::generalAppAfterPayment,
            callbackKey(SUBMITTED), this::emptySubmittedCallbackResponse
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse generalAppAfterPayment(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        PaymentStatus paymentStatus = Optional.of(caseData).map(CaseData::getGeneralAppPBADetails).map(GAPbaDetails::getPaymentDetails)
            .map(PaymentDetails::getStatus).orElse(null);

        // No need to initiate business process if payment status is failed
        if (gaForLipService.isLipApp(caseData) && paymentStatus == PaymentStatus.FAILED) {
            return getCallbackResponse(caseDataBuilder);
        }

        if (caseData.getGeneralAppType().getTypes().contains(GeneralApplicationTypes.CONFIRM_CCJ_DEBT_PAID)) {
            caseDataBuilder.businessProcess(BusinessProcess
                                                .ready(INITIATE_COSC_APPLICATION_AFTER_PAYMENT));
        } else {
            caseDataBuilder.businessProcess(BusinessProcess
                                                .ready(INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT));
        }

        return getCallbackResponse(caseDataBuilder);
    }

    private CallbackResponse getCallbackResponse(CaseData.CaseDataBuilder caseDataBuilder) {
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

}
