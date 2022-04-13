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
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.service.NotificationService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_GENERAL_APPLICATION_RESPONDENT;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.FORMATTER;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.MANDATORY_SUFFIX;
import static uk.gov.hmcts.reform.civil.utils.ApplicationNotificationUtil.isNotificationCriteriaSatisfied;

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
        boolean isNotificationCriteriaSatisfied = isNotificationCriteriaSatisfied(caseData);

        if (isNotificationCriteriaSatisfied) {

            List<Element<GASolicitorDetailsGAspec>> respondentSolicitor = caseData.getGeneralAppRespondentSolicitors();
            respondentSolicitor.stream().forEach((RS) ->
                sendNotificationToGeneralAppRespondent(caseData, RS.getValue().getEmail()));
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
            String.format(REFERENCE_TEMPLATE, caseData.getGeneralAppParentCaseLink().getCaseReference())
        );
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        return Map.of(
            APPLICANT_REFERENCE, YES.equals(caseData.getParentClaimantIsApplicant()) ? "claimant" : "respondent",
            CASE_REFERENCE, caseData.getGeneralAppParentCaseLink().getCaseReference(),
            GA_NOTIFICATION_DEADLINE, DateFormatHelper
            .formatLocalDate(
                LocalDate.parse(
                    caseData
                        .getGeneralAppDeadlineNotificationDate() + MANDATORY_SUFFIX,
                    FORMATTER), DATE)
        );
    }
}
