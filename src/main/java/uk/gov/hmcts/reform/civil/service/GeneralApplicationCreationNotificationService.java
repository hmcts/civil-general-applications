package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.enums.PaymentStatus;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.handler.callback.user.JudicialFinalDecisionHandler;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.isNotificationCriteriaSatisfied;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.isUrgentApplnNotificationCriteriaSatisfied;

@Service
@RequiredArgsConstructor
public class GeneralApplicationCreationNotificationService  implements NotificationData {

    private static final String REFERENCE_TEMPLATE = "general-application-respondent-notification-%s";

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationsProperties notificationProperties;

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;
    private final GaForLipService gaForLipService;

    private final SolicitorEmailValidation solicitorEmailValidation;

    public  CaseData sendNotification(CaseData caseData) throws NotificationException {

        CaseData civilCaseData = caseDetailsConverter
            .toCaseData(coreCaseDataService
                            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));

        CaseData updatedCaseData = solicitorEmailValidation.validateSolicitorEmail(civilCaseData, caseData);

        boolean isNotificationCriteriaSatisfied = isNotificationCriteriaSatisfied(updatedCaseData);

        /*
         * Send email to Respondents if application is withNotice and non-urgent
         * */
        if (isNotificationCriteriaSatisfied) {

            List<Element<GASolicitorDetailsGAspec>> respondentSolicitor = updatedCaseData
                .getGeneralAppRespondentSolicitors();

            respondentSolicitor
                .forEach((RS) ->
                             sendNotificationToGeneralAppRespondent(updatedCaseData,
                                                                    RS.getValue().getEmail(),
                                     getTemplate(updatedCaseData, false)
                             ));
        }

        /*
        * Send email to Respondent if application is urgent, with notice and fee is paid
        * */
        boolean isUrgentApplnNotificationCriteriaSatisfied
            = isUrgentApplnNotificationCriteriaSatisfied(updatedCaseData);

        if (isUrgentApplnNotificationCriteriaSatisfied
            && isFeePaid(updatedCaseData)) {

            List<Element<GASolicitorDetailsGAspec>> respondentSolicitor = updatedCaseData
                .getGeneralAppRespondentSolicitors();

            respondentSolicitor
                .forEach((RS) ->
                             sendNotificationToGeneralAppRespondent(
                                 updatedCaseData,
                                 RS.getValue().getEmail(),
                                 getTemplate(updatedCaseData, true)));
        }

        return caseData;
    }

    public boolean isFeePaid(CaseData caseData) {
        return caseData.getGeneralAppPBADetails() != null
            && (caseData.getGeneralAppPBADetails().getFee().getCode().equals("FREE")
            || (caseData.getGeneralAppPBADetails().getPaymentDetails() != null
            && caseData.getGeneralAppPBADetails().getPaymentDetails().getStatus().equals(PaymentStatus.SUCCESS)));
    }

    private String getTemplate(CaseData caseData, boolean urgent) {
        if (gaForLipService.isLipResp(caseData)) {
            return notificationProperties
                    .getLipGeneralAppRespondentEmailTemplate();
        } else {
            return urgent ? notificationProperties
                    .getUrgentGeneralAppRespondentEmailTemplate() : notificationProperties
                    .getGeneralApplicationRespondentEmailTemplate();
        }
    }

    private void sendNotificationToGeneralAppRespondent(CaseData caseData, String recipient, String emailTemplate)
        throws NotificationException {
        try {
            notificationService.sendMail(
                recipient,
                emailTemplate,
                addProperties(caseData),
                String.format(REFERENCE_TEMPLATE, caseData.getGeneralAppParentCaseLink().getCaseReference())
            );
        } catch (NotificationException e) {
            throw new NotificationException(e);
        }
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        String lipRespName = "";
        String caseTitle = "";
        if (gaForLipService.isLipResp(caseData)) {

            String surname = "";
            if (caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getSurname() != null) {
                surname = caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getSurname().orElse("");
            }

            lipRespName = caseData
                    .getGeneralAppRespondentSolicitors().get(0).getValue().getForename()
                    + " " + surname;
            caseTitle = JudicialFinalDecisionHandler.getAllPartyNames(caseData);

        }
        return Map.of(
            APPLICANT_REFERENCE, YES.equals(caseData.getParentClaimantIsApplicant()) ? "claimant" : "respondent",
            CASE_REFERENCE, caseData.getGeneralAppParentCaseLink().getCaseReference(),
            GA_NOTIFICATION_DEADLINE, DateFormatHelper
                .formatLocalDateTime(caseData
                                         .getGeneralAppNotificationDeadlineDate(), DATE),
            GA_LIP_RESP_NAME, lipRespName,
            CASE_TITLE, Objects.requireNonNull(caseTitle)
        );
    }

}
