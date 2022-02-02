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
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.service.NotificationService;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_GENERAL_APPLICATION_RESPONDENT;

@Service
@RequiredArgsConstructor
public class GeneralApplicationCreationNotificationHandler extends CallbackHandler implements NotificationData {

    private static final List<CaseEvent> EVENTS = List.of(
        NOTIFY_GENERAL_APPLICATION_RESPONDENT
    );

    private static final String TASK_ID = "NotifyGeneralApplicationRespondent";

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
        var recipient = caseData.getGeneralApplicationRespondentEmailAddress();

        boolean conditions = isWithNotice(caseData)
            && isNonConsent(caseData)
            && isNonUrgent(caseData);

        if (conditions) {
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
            String.format(REFERENCE_TEMPLATE, caseData.getLegacyCaseReference())
        );
    }

    private boolean isNonConsent(CaseData caseData) {
        List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
        boolean present = generalApplications.stream()
            .anyMatch(app -> app.getValue()
                .getGeneralAppRespondentAgreement()
                .getHasAgreed() == YesOrNo.NO);
        return present;
    }

    private boolean isWithNotice(CaseData caseData) {
        List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
        boolean present = generalApplications.stream()
            .anyMatch(app -> app.getValue()
                .getGeneralAppInformOtherParty()
                .getIsWithNotice() == YesOrNo.YES);
        return present;
    }

    private boolean isNonUrgent(CaseData caseData) {
        List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
        boolean present = generalApplications.stream()
            .anyMatch(app -> app.getValue()
                .getGeneralAppUrgencyRequirement()
                .getGeneralAppUrgency() == YesOrNo.NO);
        return present;
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        return Map.of(
            GENERAL_APPLICATION_REFERENCE, caseData.getLegacyCaseReference()
        );
    }
}
