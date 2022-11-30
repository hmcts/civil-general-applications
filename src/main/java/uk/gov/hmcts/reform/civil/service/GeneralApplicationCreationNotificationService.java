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

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.civil.utils.ApplicationNotificationUtil.isNotificationCriteriaSatisfied;

@Service
@RequiredArgsConstructor
public class GeneralApplicationCreationNotificationService  implements NotificationData {

    private static final String REFERENCE_TEMPLATE = "general-application-respondent-notification-%s";

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationsProperties notificationProperties;

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;

    private final SolicitorEmailValidation solicitorEmailValidation;

    public  CaseData sendNotification(CaseData caseData) throws NotificationException {

        CaseData civilCaseData = caseDetailsConverter
            .toCaseData(coreCaseDataService
                            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));

        CaseData updatedCaseData = solicitorEmailValidation.validateSolicitorEmail(civilCaseData, caseData);

        boolean isNotificationCriteriaSatisfied = isNotificationCriteriaSatisfied(updatedCaseData);

        if (isNotificationCriteriaSatisfied) {

            List<Element<GASolicitorDetailsGAspec>> respondentSolicitor = updatedCaseData
                .getGeneralAppRespondentSolicitors();

            respondentSolicitor.forEach((RS) ->
                                            sendNotificationToGeneralAppRespondent(updatedCaseData,
                                                                                   RS.getValue().getEmail()));
        }
        return caseData;
    }

    private void sendNotificationToGeneralAppRespondent(CaseData caseData, String recipient)
        throws NotificationException {
        try {
            notificationService.sendMail(
                recipient,
                notificationProperties.getGeneralApplicationRespondentEmailTemplate(),
                addProperties(caseData),
                String.format(REFERENCE_TEMPLATE, caseData.getGeneralAppParentCaseLink().getCaseReference())
            );
        } catch (NotificationException e) {
            throw new NotificationException(e);
        }
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        return Map.of(
            APPLICANT_REFERENCE, YES.equals(caseData.getParentClaimantIsApplicant()) ? "claimant" : "respondent",
            CASE_REFERENCE, caseData.getGeneralAppParentCaseLink().getCaseReference(),
            GA_NOTIFICATION_DEADLINE, DateFormatHelper
                .formatLocalDateTime(caseData
                                         .getGeneralAppNotificationDeadlineDate(), DATE)
        );
    }

}
