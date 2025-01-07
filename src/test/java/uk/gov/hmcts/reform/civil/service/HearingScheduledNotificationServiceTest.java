package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.Language;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

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
    @MockBean
    private GaForLipService gaForLipService;

    private static final Long CASE_REFERENCE = 111111L;
    private static final LocalDate GA_HEARING_DATE_SAMPLE = LocalDate.now().plusDays(10);
    private static final LocalTime GA_HEARING_TIME_SAMPLE = LocalTime.of(15, 30, 0);
    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";
    private Map<String, String> customProp = new HashMap<>();

    @BeforeEach
    void setup() {
        when(notificationsProperties.getHearingNoticeTemplate())
            .thenReturn("general-apps-notice-of-hearing-template-id");
        when(notificationsProperties.getLipGeneralAppApplicantEmailTemplate())
            .thenReturn("ga-notice-of-hearing-applicant-template-id");
        when(notificationsProperties.getLipGeneralAppApplicantEmailTemplateInWelsh())
            .thenReturn("ga-notice-of-hearing-applicant-welsh-template-id");
        when(notificationsProperties.getLipGeneralAppRespondentEmailTemplate())
            .thenReturn("ga-notice-of-hearing-respondent-template-id");
        when(notificationsProperties.getLipGeneralAppRespondentEmailTemplateInWelsh())
            .thenReturn("ga-notice-of-hearing-respondent-welsh-template-id");
    }

    private Map<String, String> getNotificationDataMap() {
        return Map.of(
            NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString(),
            NotificationData.GA_HEARING_DATE, DateFormatHelper.formatLocalDate(
                GA_HEARING_DATE_SAMPLE, DateFormatHelper.DATE),
            NotificationData.GA_HEARING_TIME, GA_HEARING_TIME_SAMPLE.toString()
        );
    }

    private Map<String, String> getNotificationDataMapLip(YesOrNo isLipAppln, YesOrNo isLipRespondent) {
        customProp.put(NotificationData.CASE_REFERENCE, CASE_REFERENCE.toString());
        customProp.put(NotificationData.GA_HEARING_DATE, DateFormatHelper.formatLocalDate(
            GA_HEARING_DATE_SAMPLE, DateFormatHelper.DATE));
        customProp.put(NotificationData.CASE_TITLE, "Test Claimant1 Name v Test Defendant1 Name");

        customProp.put(NotificationData.GA_HEARING_TIME, GA_HEARING_TIME_SAMPLE.toString());
        if (isLipAppln == YES) {
            customProp.put(NotificationData.GA_LIP_APPLICANT_NAME, "Test Applicant Name");
        }

        if (isLipRespondent == YES) {
            customProp.put(NotificationData.GA_LIP_RESP_NAME, "Test Defendant1 Name");
        }
        return customProp;
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

    @Test
    void notificationShouldSendToLipDefendantWhenInvoked() {
        when(gaForLipService.isLipApp(any())).thenReturn(false);
        when(gaForLipService.isLipResp(any())).thenReturn(true);
        when(gaForLipService.isGaForLip(any())).thenReturn(true);

        List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();
        GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).surname(Optional.of("surname"))
            .forename("forename").organisationIdentifier("2").build();
        respondentSols.add(element(respondent1));

        CaseData caseData = CaseDataBuilder.builder().hearingScheduledApplication(YesOrNo.NO)
            .isGaApplicantLip(YesOrNo.NO)
            .isGaRespondentOneLip(YES)
            .generalAppRespondentSolicitors(respondentSols)
            .defendant2PartyName(null)
            .build();
        when(solicitorEmailValidation
                 .validateSolicitorEmail(any(), any()))
            .thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());

        hearingScheduledNotificationService.sendNotificationForDefendant(caseData);
        verify(notificationService, times(1)).sendMail(
            DUMMY_EMAIL,
            "ga-notice-of-hearing-respondent-template-id",
            getNotificationDataMapLip(NO, YES),
            "general-apps-notice-of-hearing-" + CASE_REFERENCE
        );
    }

    @Test
    void notificationShouldSendToLipApplicantWhenInvoked() {
        when(gaForLipService.isLipApp(any())).thenReturn(true);
        when(gaForLipService.isLipResp(any())).thenReturn(false);
        when(gaForLipService.isGaForLip(any())).thenReturn(true);
        CaseData caseData = CaseDataBuilder.builder().hearingScheduledApplication(YesOrNo.NO)
            .isGaApplicantLip(YesOrNo.YES)
            .isGaRespondentOneLip(YesOrNo.NO)
            .defendant2PartyName(null)
            .build();
        when(solicitorEmailValidation
                 .validateSolicitorEmail(any(), any()))
            .thenReturn(caseData);
        when(caseDetailsConverter.toCaseData(any())).thenReturn(CaseData.builder().build());

        hearingScheduledNotificationService.sendNotificationForClaimant(caseData);
        verify(notificationService, times(1)).sendMail(
            DUMMY_EMAIL,
            "ga-notice-of-hearing-applicant-template-id",
            getNotificationDataMapLip(YES, NO),
            "general-apps-notice-of-hearing-" + CASE_REFERENCE
        );
    }

    @Test
    void notificationWelshShouldSendToLipApplicantWhenInvoked() {
        when(gaForLipService.isLipApp(any())).thenReturn(true);
        when(gaForLipService.isLipResp(any())).thenReturn(false);
        when(gaForLipService.isGaForLip(any())).thenReturn(true);
        CaseData caseData = CaseDataBuilder.builder().hearingScheduledApplication(YesOrNo.NO)
            .isGaApplicantLip(YesOrNo.YES)
            .isGaRespondentOneLip(YesOrNo.NO)
            .parentClaimantIsApplicant(YES)
            .applicantBilingualLanguagePreference(YES)
            .defendant2PartyName(null)
            .build();
        when(solicitorEmailValidation
                 .validateSolicitorEmail(any(), any()))
            .thenReturn(caseData);
        CaseData claimantClaimIssueFlag = CaseData.builder().claimantBilingualLanguagePreference(Language.WELSH.toString()).build();
        when(caseDetailsConverter.toCaseData(any())).thenReturn(claimantClaimIssueFlag);

        hearingScheduledNotificationService.sendNotificationForClaimant(caseData);
        verify(notificationService, times(1)).sendMail(
            DUMMY_EMAIL,
            "ga-notice-of-hearing-applicant-welsh-template-id",
            getNotificationDataMapLip(YES, NO),
            "general-apps-notice-of-hearing-" + CASE_REFERENCE
        );
    }

}
