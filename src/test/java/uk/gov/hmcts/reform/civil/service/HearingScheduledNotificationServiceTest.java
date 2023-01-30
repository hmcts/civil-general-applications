package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    HearingScheduledNotificationService.class,
    JacksonAutoConfiguration.class
})
public class HearingScheduledNotificationServiceTest {

    @Autowired
    private HearingScheduledNotificationService hearingScheduledNotificationService;
    @MockBean
    private SolicitorEmailValidation solicitorEmailValidation;
    @MockBean
    private NotificationService notificationService;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;
    @MockBean
    private NotificationsProperties notificationsProperties;

    private static final Long CASE_REFERENCE = 111111L;
    private static final LocalDate GA_HEARING_DATE_SAMPLE = LocalDate.now().plusDays(10);
    private static final LocalTime GA_HEARING_TIME_SAMPLE = LocalTime.of(15, 30, 0);
    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";

    @BeforeEach
    void setup() {
        when(notificationsProperties.getHearingNoticeTemplate())
            .thenReturn("general-apps-notice-of-hearing-template-id");
    }

    private Map<String, String> getNotificationDataMap() {
        return Map.of(
            NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
            NotificationData.GA_HEARING_DATE, DateFormatHelper.formatLocalDate(
                GA_HEARING_DATE_SAMPLE, DateFormatHelper.DATE),
            NotificationData.GA_HEARING_TIME, GA_HEARING_TIME_SAMPLE.toString()
        );
    }

    @Test
    void notificationShouldSendToDefendantsWhenInvoked() {
        CaseData caseData = CaseDataBuilder.builder().hearingScheduledApplication(YesOrNo.NO)
            .build();
        when(solicitorEmailValidation
                 .validateSolicitorEmail(any(), any()))
            .thenReturn(caseData);

        hearingScheduledNotificationService.sendNotificationForDefendant(caseData);
        verify(notificationService, times(2)).sendMail(
            DUMMY_EMAIL,
            "general-apps-notice-of-hearing-template-id",
            getNotificationDataMap(),
            "general-apps-notice-of-hearing-" + CASE_REFERENCE
        );
    }

    @Test
    void notificationShouldSendToClaimantWhenInvoked() {
        CaseData caseData = CaseDataBuilder.builder().hearingScheduledApplication(YesOrNo.NO)
            .build();
        when(solicitorEmailValidation
                 .validateSolicitorEmail(any(), any()))
            .thenReturn(caseData);

        hearingScheduledNotificationService.sendNotificationForClaimant(caseData);
        verify(notificationService, times(1)).sendMail(
            DUMMY_EMAIL,
            "general-apps-notice-of-hearing-template-id",
            getNotificationDataMap(),
            "general-apps-notice-of-hearing-" + CASE_REFERENCE
        );
    }

}
