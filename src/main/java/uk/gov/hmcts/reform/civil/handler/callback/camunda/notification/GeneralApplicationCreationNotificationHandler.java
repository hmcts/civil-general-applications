package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.GeneralApplicationCreationNotificationService;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_GENERAL_APPLICATION_RESPONDENT;

@Service
@RequiredArgsConstructor
public class GeneralApplicationCreationNotificationHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(
        NOTIFY_GENERAL_APPLICATION_RESPONDENT
    );

    private final ObjectMapper objectMapper;

    private final GeneralApplicationCreationNotificationService gaCreationNotificationService;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::notifyGeneralApplicationCreationRespondent
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse notifyGeneralApplicationCreationRespondent(CallbackParams callbackParams) {

        CaseData caseData = callbackParams.getCaseData();

        caseData = gaCreationNotificationService.sendNotification(caseData);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .build();
    }
}
