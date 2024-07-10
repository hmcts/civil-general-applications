package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;

@Service
@RequiredArgsConstructor
public class HearingScheduledNotificationService implements NotificationData {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final NotificationsProperties notificationProperties;
    private final SolicitorEmailValidation solicitorEmailValidation;
    private final CoreCaseDataService coreCaseDataService;
    private final Map<String, String> customProps;
    private final GaForLipService gaForLipService;
    private static final String REFERENCE_TEMPLATE_HEARING = "general-apps-notice-of-hearing-%s";

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

        if (gaForLipService.isLipApp(caseData)) {
            String isLipAppName = caseData.getApplicantPartyName();
            customProps.put(
                GA_LIP_APPLICANT_NAME,
                Objects.requireNonNull(isLipAppName)
            );
            customProps.remove(GA_LIP_RESP_NAME);
        }

        if (gaForLipService.isLipResp(caseData)) {

            String surname = "";
            if (caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getSurname().isPresent()) {
                surname = caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getSurname().orElse("");
            }

            String isLipRespondentName = caseData
                .getGeneralAppRespondentSolicitors().get(0).getValue().getForename()
                + " " + surname;
            customProps.put(
                GA_LIP_RESP_NAME,
                Objects.requireNonNull(isLipRespondentName)
            );
            customProps.remove(GA_LIP_APPLICANT_NAME);
        }

        if (gaForLipService.isGaForLip(caseData)) {
            String caseTitle = JudicialFinalDecisionHandler.getAllPartyNames(caseData);
            customProps.put(
                CASE_TITLE,
                Objects.requireNonNull(caseTitle)
            );
        } else {
            customProps.remove(CASE_TITLE);
            customProps.remove(GA_LIP_APPLICANT_NAME);
            customProps.remove(GA_LIP_RESP_NAME);
        }

        return customProps;
    }

    private void sendNotification(CaseData caseData, String recipient, String template) throws NotificationException {
        try {
            notificationService.sendMail(recipient,  template,
                                         addProperties(caseData),
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
                             ? notificationProperties.getLipGeneralAppApplicantEmailTemplate()
                             : notificationProperties.getHearingNoticeTemplate());

        return caseData;
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
                ? notificationProperties.getLipGeneralAppRespondentEmailTemplate()
                : notificationProperties.getHearingNoticeTemplate()));

        return caseData;
    }

}
