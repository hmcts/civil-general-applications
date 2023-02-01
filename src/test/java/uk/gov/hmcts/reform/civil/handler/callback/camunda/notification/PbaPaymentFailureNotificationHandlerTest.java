package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.payment.PbaPaymentFailureNotificationHandler;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.IdamUserDetails;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.NotificationService;
import uk.gov.hmcts.reform.civil.service.SolicitorEmailValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@SpringBootTest(classes = {
    PbaPaymentFailureNotificationHandler.class,
    JacksonAutoConfiguration.class,
})
public class PbaPaymentFailureNotificationHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    PbaPaymentFailureNotificationHandler handler;

    @MockBean
    private SolicitorEmailValidation solicitorEmailValidation;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationsProperties notificationsProperties;

    private static final Long CASE_REFERENCE = 111111L;
    private static final String PROCESS_INSTANCE_ID = "1";
    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";

    @Nested
    class AboutToSubmitCallback {

        @BeforeEach
        void setup() {
            when(notificationsProperties.getGeneralApplicationPaymentFailure())
                .thenReturn("payment-failure-applicant-notification-template-id");
        }

        @Test
        void notificationShouldSendWhenInvoked() {
            CaseData caseData = getCaseData();

            when(solicitorEmailValidation
                     .validateApplicantSolicitorEmail(any(), any()))
                .thenReturn(caseData);
            when(caseDetailsConverter.toCaseData(any()))
                .thenReturn(caseData);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId("PBA_PAYMENT_FAILED").build()).build();

            handler.handle(params);

            verify(notificationService, times(1)).sendMail(
                DUMMY_EMAIL,
                "payment-failure-applicant-notification-template-id",
                getNotificationDataMap(),
                "payment-failure-applicant-notification-" + CASE_REFERENCE
            );

        }

        private Map<String, String> getNotificationDataMap() {
            return Map.of(
                NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
                NotificationData.APPLICANT_REFERENCE, "claimant"
            );
        }

        private CaseData getCaseData() {

            List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

            GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
                .email(DUMMY_EMAIL).organisationIdentifier("2").build();

            GASolicitorDetailsGAspec respondent2 = GASolicitorDetailsGAspec.builder().id("id")
                .email(DUMMY_EMAIL).organisationIdentifier("3").build();

            respondentSols.add(element(respondent1));
            respondentSols.add(element(respondent2));

            return new CaseDataBuilder()
                .businessProcess(BusinessProcess.builder().status(STARTED)
                                     .processInstanceId(PROCESS_INSTANCE_ID).build())
                .gaInformOtherParty(GAInformOtherParty.builder().isWithNotice(NO).build())
                .gaUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(NO).build())
                .gaRespondentOrderAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
                .ccdCaseReference(CASE_REFERENCE)
                .generalAppApplnSolicitor(
                    GASolicitorDetailsGAspec.builder().email(DUMMY_EMAIL).build())
                .applicant1OrganisationPolicy(OrganisationPolicy.builder()
                                                  .organisation(Organisation.builder().organisationID("1").build())
                                                  .build())
                .respondent1OrganisationPolicy(OrganisationPolicy.builder()
                                                   .organisation(Organisation.builder().organisationID("2").build())
                                                   .build())
                .respondent2OrganisationPolicy(OrganisationPolicy.builder()
                                                   .organisation(Organisation.builder().organisationID("3").build())
                                                   .build())
                .applicantSolicitor1UserDetails(IdamUserDetails.builder().email(DUMMY_EMAIL).build())
                .parentClaimantIsApplicant(YES)
                .generalAppParentCaseLink(
                    GeneralAppParentCaseLink
                        .builder()
                        .caseReference(CASE_REFERENCE.toString())
                        .build())
                .build();
        }
    }
}

