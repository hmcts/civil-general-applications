package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.NotificationException;
import uk.gov.hmcts.reform.civil.service.NotificationService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_HEARING_NOTICE;

@SpringBootTest(classes = {
    HearingScheduledNotificationHandler.class,
    JacksonAutoConfiguration.class,
})
public class HearingScheduledNotificationHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private HearingScheduledNotificationHandler handler;
    @MockBean
    private NotificationsProperties notificationsProperties;
    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @MockBean
    private NotificationService notificationService;

    private static final Long CASE_REFERENCE = 111111L;
    private static final LocalDate GA_HEARING_DATE_SAMPLE = LocalDate.now().plusDays(10);
    private static final LocalTime GA_HEARING_TIME_SAMPLE = LocalTime.of(15, 30, 0);
    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";
    private CallbackParams params;

    @BeforeEach
    void setup() {
        when(notificationsProperties.getHearingNoticeTemplate())
            .thenReturn("general-apps-notice-of-hearing-template-id");
    }

    @Test
    public void shouldReturnCorrectEvent() {
        CaseData caseData = CaseDataBuilder.builder().hearingScheduledApplication(YesOrNo.NO).build();
        params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        assertThat(handler.handledEvents()).contains(NOTIFY_HEARING_NOTICE);
    }

    @Test
    void shouldThrowException_whenNotificationSendingFails() {
        var caseData = CaseDataBuilder.builder().hearingScheduledApplication(YesOrNo.NO)
            .build();
        doThrow(buildNotificationException())
            .when(notificationService)
            .sendMail(any(), any(), any(), any());
        params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        assertThrows(NotificationException.class, () -> handler.handle(params));
    }

    @Test
    void shouldSendNotificationToApplicantAndRespondentsWhenInvoked() {
        var caseData = CaseDataBuilder.builder().hearingScheduledApplication(YesOrNo.NO)
            .build();

        params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        handler.handle(params);
        verify(notificationService, times(3)).sendMail(
            DUMMY_EMAIL,
            "general-apps-notice-of-hearing-template-id",
            getNotificationDataMap(),
            "general-apps-notice-of-hearing-" + CASE_REFERENCE
        );

    }

    private NotificationException buildNotificationException() {
        return new NotificationException(new Exception("Notification Exception"));
    }

    private Map<String, String> getNotificationDataMap() {
        return Map.of(
            NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
            NotificationData.GA_HEARING_DATE, DateFormatHelper.formatLocalDate(
                                                    GA_HEARING_DATE_SAMPLE, DateFormatHelper.DATE),
            NotificationData.GA_HEARING_TIME, GA_HEARING_TIME_SAMPLE.toString()
        );
    }
}
