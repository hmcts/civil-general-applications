package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.NotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@SpringBootTest(classes = {
    GeneralApplicationCreationNotificationHandler.class,
    JacksonAutoConfiguration.class,
})
public class GeneralApplicationCreationNotificationHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private GeneralApplicationCreationNotificationHandler handler;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationsProperties notificationsProperties;

    private static final Long GENERAL_APP_REFERENCE = 111111L;
    private static final String PROCESS_INSTANCE_ID = "1";
    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";
    public static final LocalDateTime NOTIFICATION_DEADLINE = LocalDate.now().atStartOfDay().plusDays(5);

    @Nested
    class AboutToSubmitCallback {

        @BeforeEach
        void setup() {
            when(notificationsProperties.getGeneralApplicationRespondentEmailTemplate())
                .thenReturn("general-application-respondent-template-id");
        }

        @Test
        void notificationShouldSendWhenInvoked() {
            CaseData caseData = getCaseData();
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId("NOTIFY_GENERAL_APPLICATION_RESPONDENT").build()).build();
            handler.handle(params);

            verify(notificationService).sendMail(
                DUMMY_EMAIL,
                "general-application-respondent-template-id",
                getNotificationDataMap(),
                "general-application-respondent-notification-" + GENERAL_APP_REFERENCE
            );
        }

        private Map<String, String> getNotificationDataMap() {
            return Map.of(
                NotificationData.GENERAL_APPLICATION_REFERENCE, GENERAL_APP_REFERENCE.toString(),
                NotificationData.GA_NOTIFICATION_DEADLINE,
                DateFormatHelper.formatLocalDate(NOTIFICATION_DEADLINE.toLocalDate(), DateFormatHelper.DATE)
            );
        }

        private CaseData getCaseData() {
            return new CaseDataBuilder()
                .businessProcess(BusinessProcess.builder().status(STARTED)
                                     .processInstanceId(PROCESS_INSTANCE_ID).build())
                .gaInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
                .gaUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(NO).build())
                .gaRespondentOrderAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .ccdCaseReference(GENERAL_APP_REFERENCE)
                .respondentSolicitor1EmailAddress(DUMMY_EMAIL)
                .build();
        }
    }
}
