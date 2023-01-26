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
import uk.gov.hmcts.reform.civil.service.NotificationException;
import uk.gov.hmcts.reform.civil.service.NotificationService;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_HEARING_NOTICE;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;

@Service
@RequiredArgsConstructor
public class HearingScheduledNotificationHandler extends CallbackHandler implements NotificationData {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationsProperties notificationProperties;
    private static final String REFERENCE_TEMPLATE_HEARING = "general-apps-notice-of-hearing-%s";
    private static final List<CaseEvent> EVENTS = List.of(
        NOTIFY_HEARING_NOTICE
    );

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::notifyHearingScheduled
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        String hourMinute = caseData.getGaHearingNoticeDetail().getHearingTimeHourMinute();
        int hours = Integer.parseInt(hourMinute.substring(0, 2));
        int minutes = Integer.parseInt(hourMinute.substring(2, 4));
        LocalTime hearingTime = LocalTime.of(hours, minutes, 0);
        return Map.of(
            CASE_REFERENCE, caseData.getGeneralAppParentCaseLink().getCaseReference(),
            GA_HEARING_DATE, DateFormatHelper.formatLocalDate(
                caseData.getGaHearingNoticeDetail().getHearingDate(),
                DATE
            ),
            GA_HEARING_TIME, hearingTime.toString()
        );
    }

    private CallbackResponse notifyHearingScheduled(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();

        try {
            List<Element<GASolicitorDetailsGAspec>> respondentSolicitor = caseData
                .getGeneralAppRespondentSolicitors();
            respondentSolicitor.forEach((respondent) ->
                                            notificationService.sendMail(
                                                respondent.getValue().getEmail(),
                                                notificationProperties.getHearingNoticeTemplate(),
                                                addProperties(caseData),
                                                String.format(
                                                    REFERENCE_TEMPLATE_HEARING,
                                                    caseData.getGeneralAppParentCaseLink().getCaseReference()
                                                )
                                            ));
            notificationService.sendMail(
                caseData.getGeneralAppApplnSolicitor().getEmail(),
                notificationProperties.getHearingNoticeTemplate(),
                addProperties(caseData),
                String.format(
                    REFERENCE_TEMPLATE_HEARING,
                    caseData.getGeneralAppParentCaseLink().getCaseReference()
                )
            );
        } catch (NotificationException notificationException) {
            throw notificationException;
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .build();
    }
}
