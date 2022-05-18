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
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.START_NOTIFICATION_PROCESS_MAKE_DECISION;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.getRequiredGAType;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.notificationCriterion;

@Service
@RequiredArgsConstructor
public class JudicialDecisionNotificationHandler extends CallbackHandler implements NotificationData {

    private final ObjectMapper objectMapper;
    private final NotificationsProperties notificationProperties;
    private final NotificationService notificationService;

    private static final List<CaseEvent> EVENTS = List.of(
        START_NOTIFICATION_PROCESS_MAKE_DECISION
    );

    private static final String REFERENCE_TEMPLATE = "general-apps-judicial-notification-make-decision-%s";

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::judicialDecisionNotification
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse judicialDecisionNotification(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        switch (notificationCriterion(caseData)) {
            case CONCURRENT_WRITTEN_REP:
                caseData.getGeneralAppRespondentSolicitors().forEach((
                    respondentSolicitor) -> sendNotificationToGeneralAppRespondent(
                    caseData, respondentSolicitor.getValue().getEmail(),
                    notificationProperties.getWrittenRepConcurrentRepresentationTemplate()));
                break;
            case SEQUENTIAL_WRITTEN_REP:
                caseData.getGeneralAppRespondentSolicitors().forEach((
                    respondentSolicitor) -> sendNotificationToGeneralAppRespondent(
                    caseData, respondentSolicitor.getValue().getEmail(),
                    notificationProperties.getWrittenRepSequentialRepresentationTemplate()));
                break;
            default: case NON_CRITERION:

        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    private void sendNotificationToGeneralAppRespondent(CaseData caseData, String recipient, String template) {
        notificationService.sendMail(
            recipient,
            template,
            addProperties(caseData),
            String.format(REFERENCE_TEMPLATE, caseData.getGeneralAppParentCaseLink().getCaseReference())
        );
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        return Map.of(
            CASE_REFERENCE, caseData.getGeneralAppParentCaseLink().getCaseReference(),
            GA_APPLICATION_TYPE, Objects.requireNonNull(getRequiredGAType(caseData.getGeneralAppType().getTypes()))
        );
    }

}
