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
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.NotificationService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.SUMMARY_JUDGEMENT;
import static uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder.LEGACY_CASE_REFERENCE;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;
import static java.time.LocalDate.EPOCH;


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

    private CaseData caseData;

    private static final String STRING_CONSTANT = "any String";
    private static final LocalDate APP_DATE_EPOCH = EPOCH;
    private static final String PROCESS_INSTANCE_ID = "1";
    private static final String DUMMY_EMAIL = "test@email.com";

    @Nested
    class AboutToSubmitCallback {

        @BeforeEach
        void setup() {
            when(notificationsProperties.getGeneralApplicationRespondentEmailTemplate())
                .thenReturn("general-application-respondent-template-id");
            GeneralApplication generalApplication = getGeneralApplication();
            caseData = new CaseDataBuilder()
                .generalApplications(getGeneralApplications(generalApplication))
                .generalApplicationRespondentEmailAddress(DUMMY_EMAIL)
                .legacyCaseReference(LEGACY_CASE_REFERENCE)
                .businessProcess(BusinessProcess.builder().status(STARTED)
                                     .processInstanceId(PROCESS_INSTANCE_ID).build()).build();
        }

        @Test
        void notificationShouldSendWhenInvoked() {
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId("NOTIFY_GENERAL_APPLICATION_RESPONDENT").build()).build();
            handler.handle(params);

            verify(notificationService).sendMail(
                caseData.getGeneralApplicationRespondentEmailAddress(),
                "general-application-respondent-template-id",
                getNotificationDataMap(),
                "general-application-respondent-notification-000DC001"
            );
        }

        private Map<String, String> getNotificationDataMap() {
            return Map.of(
                NotificationData.GENERAL_APPLICATION_REFERENCE, caseData.getLegacyCaseReference()
            );
        }

        private List<Element<GeneralApplication>> getGeneralApplications(GeneralApplication generalApplication) {
            return wrapElements(generalApplication);
        }

        private GeneralApplication getGeneralApplication() {
            GeneralApplication.GeneralApplicationBuilder builder = GeneralApplication.builder();
            return builder.generalAppType(GAApplicationType.builder()
                                              .types(singletonList(SUMMARY_JUDGEMENT))
                                              .build())
                .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                                   .hasAgreed(NO)
                                                   .build())
                .generalAppInformOtherParty(GAInformOtherParty.builder()
                                                .isWithNotice(YES)
                                                .reasonsForWithoutNotice(STRING_CONSTANT)
                                                .build())
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder()
                                                  .generalAppUrgency(NO)
                                                  .reasonsForUrgency(STRING_CONSTANT)
                                                  .urgentAppConsiderationDate(APP_DATE_EPOCH)
                                                  .build())
                .isMultiParty(YesOrNo.NO)
                .businessProcess(BusinessProcess.builder()
                                     .status(STARTED)
                                     .processInstanceId(PROCESS_INSTANCE_ID)
                                     .build())
                .build();
        }
    }
}
