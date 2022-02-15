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
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.NotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_GENERAL_APPLICATION_RESPONDENT;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;

@Service
@RequiredArgsConstructor
public class GeneralApplicationCreationNotificationHandler extends CallbackHandler implements NotificationData {

    private static final List<CaseEvent> EVENTS = List.of(
        NOTIFY_GENERAL_APPLICATION_RESPONDENT
    );

    private static final String REFERENCE_TEMPLATE = "general-application-respondent-notification-%s";

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationsProperties notificationProperties;

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
        var recipient = caseData.getRespondentSolicitor1EmailAddress();
        boolean isNotificationCriteriaSatisfied = isWithNotice(caseData)
            && isNonConsent(caseData)
            && isNonUrgent(caseData)
            && !(recipient == null || recipient.isEmpty());

        if (isNotificationCriteriaSatisfied) {
            sendNotificationToGeneralAppRespondent(caseData, recipient);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .build();
    }

    private void sendNotificationToGeneralAppRespondent(CaseData caseData, String recipient) {
        notificationService.sendMail(
            recipient,
            notificationProperties.getGeneralApplicationRespondentEmailTemplate(),
            addProperties(caseData),
            String.format(REFERENCE_TEMPLATE, caseData.getCcdCaseReference().toString())
        );
    }

    private boolean isNonConsent(CaseData caseData) {
        return caseData
            .getGeneralAppRespondentAgreement()
            .getHasAgreed() == YesOrNo.NO;
    }

    private boolean isWithNotice(CaseData caseData) {
        return caseData
            .getGeneralAppInformOtherParty()
            .getIsWithNotice() == YesOrNo.YES;
    }

    private boolean isNonUrgent(CaseData caseData) {
        return caseData
            .getGeneralAppUrgencyRequirement()
            .getGeneralAppUrgency() == YesOrNo.NO;
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        LocalDateTime deadline = LocalDate.now().atStartOfDay().plusDays(5);
        return Map.of(
            GENERAL_APPLICATION_REFERENCE, caseData.getCcdCaseReference().toString(),
            GA_NOTIFICATION_DEADLINE,
            DateFormatHelper.formatLocalDateTime(deadline, DATE)
        );
    }
}
