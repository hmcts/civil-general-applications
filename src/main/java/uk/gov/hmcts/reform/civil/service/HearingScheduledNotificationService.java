package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.handler.callback.user.JudicialFinalDecisionHandler;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;

@Slf4j
@Service
@RequiredArgsConstructor
public class HearingScheduledNotificationService implements NotificationData {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final NotificationsProperties notificationProperties;
    private final SolicitorEmailValidation solicitorEmailValidation;
    private final CoreCaseDataService coreCaseDataService;
    private final Map<String, String> customProps = new HashMap<>();
    private final GaForLipService gaForLipService;
    private static final String REFERENCE_TEMPLATE_HEARING = "general-apps-notice-of-hearing-%s";
    private static final String RESPONDENT = "respondent";
    private static final String APPLICANT = "applicant";

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        String hourMinute = caseData.getGaHearingNoticeDetail().getHearingTimeHourMinute();
        int hours = Integer.parseInt(hourMinute.substring(0, 2));
        int minutes = Integer.parseInt(hourMinute.substring(2, 4));
        LocalTime hearingTime = LocalTime.of(hours, minutes, 0);

        customProps.put(CASE_REFERENCE, caseData.getGeneralAppParentCaseLink().getCaseReference());
        customProps.put(GA_HEARING_DATE, DateFormatHelper
            .formatLocalDate(caseData.getGaHearingNoticeDetail().getHearingDate(), DATE));
        customProps.put(GA_HEARING_TIME, hearingTime.toString());
        if (gaForLipService.isGaForLip(caseData)) {
            customProps.put(CASE_TITLE, Objects.requireNonNull(JudicialFinalDecisionHandler
                                                                   .getAllPartyNames(caseData)));
        } else {
            customProps.remove(CASE_TITLE);
            customProps.remove(GA_LIP_APPLICANT_NAME);
            customProps.remove(GA_LIP_RESP_NAME);
        }

        return customProps;
    }

    public Map<String, String> addPropertiesByType(CaseData caseData, String gaLipType) {
        if (gaForLipService.isLipApp(caseData) && gaLipType.equals(APPLICANT)) {
            String isLipAppName = caseData.getApplicantPartyName();
            customProps.put(
                GA_LIP_APPLICANT_NAME,
                Objects.requireNonNull(isLipAppName)
            );
            customProps.remove(GA_LIP_RESP_NAME);
        }

        if (gaForLipService.isLipResp(caseData) && gaLipType.equals(RESPONDENT)) {
            String isLipRespondentName = caseData.getDefendant1PartyName();
            customProps.remove(GA_LIP_APPLICANT_NAME);
            customProps.put(GA_LIP_RESP_NAME, Objects.requireNonNull(isLipRespondentName));
        }

        addProperties(caseData);
        return customProps;
    }

    private void sendNotification(CaseData caseData, String recipient, String template, String gaLipType) throws NotificationException {
        try {
            notificationService.sendMail(recipient,  template,
                                         addPropertiesByType(caseData, gaLipType),
                                         String.format(REFERENCE_TEMPLATE_HEARING,
                                                       caseData.getGeneralAppParentCaseLink().getCaseReference()));
        } catch (NotificationException e) {
            throw new NotificationException(e);
        }
    }

    public CaseData sendNotificationForClaimant(CaseData caseData) throws NotificationException {

        CaseData civilCaseData = caseDetailsConverter
            .toCaseData(coreCaseDataService
                            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));

        caseData = solicitorEmailValidation.validateSolicitorEmail(civilCaseData, caseData);

        sendNotification(caseData,  caseData.getGeneralAppApplnSolicitor().getEmail(),
                         gaForLipService.isLipApp(caseData)
                             ? getLiPApplicantTemplates(caseData)
                             : notificationProperties.getHearingNoticeTemplate(), APPLICANT);
        log.info("Sending hearing scheduled notification for claimant for Case ID: {}", caseData.getCcdCaseReference());

        return caseData;
    }

    private String getLiPApplicantTemplates(CaseData caseData) {
        return caseData.isApplicantBilingual(caseData.getParentClaimantIsApplicant())
            ? notificationProperties.getLipGeneralAppApplicantEmailTemplateInWelsh()
            : notificationProperties.getLipGeneralAppApplicantEmailTemplate();
    }

    public CaseData sendNotificationForDefendant(CaseData caseData) throws NotificationException {

        CaseData civilCaseData = caseDetailsConverter
            .toCaseData(coreCaseDataService
                            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));

        caseData = solicitorEmailValidation.validateSolicitorEmail(civilCaseData, caseData);

        List<Element<GASolicitorDetailsGAspec>> respondentSolicitor = caseData
            .getGeneralAppRespondentSolicitors();
        CaseData updatedCaseData = caseData;
        respondentSolicitor.forEach((respondent) -> sendNotification(
            updatedCaseData,
            respondent.getValue().getEmail(), gaForLipService.isLipResp(updatedCaseData)
                ? getLiPRespondentTemplate(civilCaseData, updatedCaseData)
                : notificationProperties.getHearingNoticeTemplate(), RESPONDENT));

        log.info("Sending hearing scheduled notification for respondent for Case ID: {}", caseData.getCcdCaseReference());
        return caseData;
    }

    private String getLiPRespondentTemplate(CaseData civilCaseData, CaseData caseData) {
        return civilCaseData.isRespondentBilingual(caseData.getParentClaimantIsApplicant())
            ? notificationProperties.getLipGeneralAppRespondentEmailTemplateInWelsh()
            : notificationProperties.getLipGeneralAppRespondentEmailTemplate();
    }
}
