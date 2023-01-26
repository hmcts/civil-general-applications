package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

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
    private static final String REFERENCE_TEMPLATE_HEARING = "general-apps-notice-of-hearing-%s";

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        String hourMinute = caseData.getGaHearingNoticeDetail().getHearingTimeHourMinute();
        int hours = Integer.parseInt(hourMinute.substring(0, 2));
        int minutes = Integer.parseInt(hourMinute.substring(2, 4));
        LocalTime hearingTime = LocalTime.of(hours, minutes, 0);
        return Map.of(
            CASE_REFERENCE, caseData.getGeneralAppParentCaseLink().getCaseReference(),
            GA_HEARING_DATE, DateFormatHelper.formatLocalDate(
                caseData.getGaHearingNoticeDetail().getHearingDate(),
                DATE
            ),
            GA_HEARING_TIME, hearingTime.toString()
        );
    }

    private void sendNotification(CaseData caseData, String recipient) throws NotificationException {
        try {
            notificationService.sendMail(recipient,  notificationProperties.getHearingNoticeTemplate(),
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

        sendNotification(caseData,  caseData.getGeneralAppApplnSolicitor().getEmail());

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
            respondent.getValue().getEmail()));

        return caseData;
    }

}
