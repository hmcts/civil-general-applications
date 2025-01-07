package uk.gov.hmcts.reform.civil.handler.callback.user;

import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.service.HwfNotificationService;
import uk.gov.hmcts.reform.civil.service.PaymentRequestUpdateCallbackService;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

abstract class HWFCallbackHandlerBase extends CallbackHandler {

    protected final ObjectMapper objectMapper;
    protected final List<CaseEvent> events;
    protected final PaymentRequestUpdateCallbackService paymentRequestUpdateCallbackService;
    protected final HwfNotificationService hwfNotificationService;
    protected final FeatureToggleService featureToggleService;

    public HWFCallbackHandlerBase(ObjectMapper objectMapper,
                                  List<CaseEvent> events,
                                  PaymentRequestUpdateCallbackService paymentRequestUpdateCallbackService,
                                  HwfNotificationService hwfNotificationService, FeatureToggleService featureToggleService) {
        this.objectMapper = objectMapper;
        this.events = events;
        this.paymentRequestUpdateCallbackService = paymentRequestUpdateCallbackService;
        this.hwfNotificationService = hwfNotificationService;
        this.featureToggleService = featureToggleService;
    }

    public HWFCallbackHandlerBase(ObjectMapper objectMapper,
                                  List<CaseEvent> events) {
        this.objectMapper = objectMapper;
        this.events = events;
        this.paymentRequestUpdateCallbackService = null;
        this.hwfNotificationService = null;
        this.featureToggleService = null;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return events;
    }

}
