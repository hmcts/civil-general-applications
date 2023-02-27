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
import uk.gov.hmcts.reform.civil.service.HearingScheduledNotificationService;
import uk.gov.hmcts.reform.civil.service.NotificationException;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_HEARING_NOTICE_DEFENDANT;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.isWithNotice;

@Service
@RequiredArgsConstructor
public class NotifyHearingNoticeDefendantHandler extends CallbackHandler {

    private final ObjectMapper objectMapper;
    private final HearingScheduledNotificationService hearingScheduledNotificationService;
    private static final List<CaseEvent> EVENTS = List.of(
        NOTIFY_HEARING_NOTICE_DEFENDANT
    );

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::notifyHearingNoticeToDefendant
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse notifyHearingNoticeToDefendant(CallbackParams callbackParams) {

        CaseData caseData = callbackParams.getCaseData();
        try {
            if (isWithNotice(caseData)) {
                caseData = hearingScheduledNotificationService.sendNotificationForDefendant(caseData);
            }
        } catch (NotificationException notificationException) {
            throw notificationException;
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .build();
    }
}
