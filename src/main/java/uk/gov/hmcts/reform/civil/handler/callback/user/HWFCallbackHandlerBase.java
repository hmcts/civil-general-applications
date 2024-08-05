package uk.gov.hmcts.reform.civil.handler.callback.user;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.PaymentRequestUpdateCallbackService;
import uk.gov.hmcts.reform.civil.utils.HwFFeeTypeService;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

abstract class HWFCallbackHandlerBase extends CallbackHandler {

    protected final ObjectMapper objectMapper;
    protected final List<CaseEvent> events;
    protected final PaymentRequestUpdateCallbackService paymentRequestUpdateCallbackService;

    public HWFCallbackHandlerBase(ObjectMapper objectMapper,
                                  List<CaseEvent> events,
                                  PaymentRequestUpdateCallbackService paymentRequestUpdateCallbackService) {
        this.objectMapper = objectMapper;
        this.events = events;
        this.paymentRequestUpdateCallbackService = paymentRequestUpdateCallbackService;
    }

    public HWFCallbackHandlerBase(ObjectMapper objectMapper,
                                  List<CaseEvent> events) {
        this.objectMapper = objectMapper;
        this.events = events;
        this.paymentRequestUpdateCallbackService = null;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return events;
    }

    protected CallbackResponse setData(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = HwFFeeTypeService.updateFeeType(caseData);
        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
    }
}
