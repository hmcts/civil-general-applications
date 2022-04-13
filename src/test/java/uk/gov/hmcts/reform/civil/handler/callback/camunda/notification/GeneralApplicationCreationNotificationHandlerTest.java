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
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.NotificationService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.FORMATTER;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.MANDATORY_SUFFIX;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

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

    private static final Long CASE_REFERENCE = 111111L;
    private static final String PROCESS_INSTANCE_ID = "1";
    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";
    private static final String DUMMY_DATE = "2022-02-15T12:00:00";
    public static LocalDate NOTIFICATION_DEADLINE = LocalDate.parse(DUMMY_DATE + MANDATORY_SUFFIX, FORMATTER);

    @Nested
    class AboutToSubmitCallback {

        @BeforeEach
        void setup() {
            when(notificationsProperties.getGeneralApplicationRespondentEmailTemplate())
                .thenReturn("general-application-respondent-template-id");
        }

        @Test
        void notificationShouldSendWhenInvoked() {
            CaseData caseData = getCaseData(true);
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId("NOTIFY_GENERAL_APPLICATION_RESPONDENT").build()).build();
            handler.handle(params);

            verify(notificationService, times(2)).sendMail(
                DUMMY_EMAIL,
                "general-application-respondent-template-id",
                getNotificationDataMap(),
                "general-application-respondent-notification-" + CASE_REFERENCE
            );
        }

        @Test
        void notificationShouldSendWhenInvokedTwice() {
            CaseData caseData = getCaseData(true);
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId("NOTIFY_GENERAL_APPLICATION_RESPONDENT").build()).build();
            handler.handle(params);

            verify(notificationService, times(2)).sendMail(
                any(),
                any(),
                any(),
                any()
            );
        }

        @Test
        void notificationShouldNotSendWhenInvokedWhenConditionsAreNotMet() {
            CaseData caseData = getCaseData(false);
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId("NOTIFY_GENERAL_APPLICATION_RESPONDENT").build()).build();
            handler.handle(params);
            verifyNoInteractions(notificationService);
        }

        private Map<String, String> getNotificationDataMap() {
            return Map.of(
                NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                NotificationData.APPLICANT_REFERENCE, "claimant",
                NotificationData.GA_NOTIFICATION_DEADLINE,
                DateFormatHelper.formatLocalDate(NOTIFICATION_DEADLINE, DATE)
            );
        }

        private CaseData getCaseData(boolean isMet) {

            List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

            GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
                .email(DUMMY_EMAIL).organisationIdentifier("org2").build();

            GASolicitorDetailsGAspec respondent2 = GASolicitorDetailsGAspec.builder().id("id")
                .email(DUMMY_EMAIL).organisationIdentifier("org2").build();

            respondentSols.add(element(respondent1));
            respondentSols.add(element(respondent2));

            if (isMet) {

                return new CaseDataBuilder()
                    .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id")
                                                 .email(DUMMY_EMAIL).organisationIdentifier("org2").build())
                    .generalAppRespondentSolicitors(respondentSols)
                    .businessProcess(BusinessProcess.builder().status(STARTED)
                                         .processInstanceId(PROCESS_INSTANCE_ID).build())
                    .gaInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
                    .gaUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(NO).build())
                    .parentClaimantIsApplicant(YES)
                    .gaRespondentOrderAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                    .generalAppParentCaseLink(
                        GeneralAppParentCaseLink
                            .builder()
                            .caseReference(CASE_REFERENCE.toString())
                            .build())
                    .respondentSolicitor1EmailAddress(DUMMY_EMAIL)
                    .generalAppDeadlineNotificationDate(DUMMY_DATE)
                    .build();
            } else {
                return new CaseDataBuilder()
                    .businessProcess(BusinessProcess.builder().status(STARTED)
                                         .processInstanceId(PROCESS_INSTANCE_ID).build())
                    .gaInformOtherParty(GAInformOtherParty.builder().isWithNotice(NO).build())
                    .gaUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(NO).build())
                    .gaRespondentOrderAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                    .ccdCaseReference(CASE_REFERENCE)
                    .respondentSolicitor1EmailAddress(DUMMY_EMAIL)
                    .generalAppDeadlineNotificationDate(DUMMY_DATE)
                    .build();
            }
        }
    }
}
