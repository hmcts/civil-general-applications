package uk.gov.hmcts.reform.civil.service;

import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.Map;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;

@Service
@RequiredArgsConstructor
public class DocUploadNotificationService implements NotificationData {

    private final NotificationService notificationService;
    private final NotificationsProperties notificationProperties;
    private static final String REFERENCE_TEMPLATE_DOC_UPLOAD = "general-apps-notice-of-document-upload-%s";
    private final GaForLipService gaForLipService;
    private final Map<String, String> customProps;

    public void notifyApplicantEvidenceUpload(CaseData caseData) throws NotificationException {
        String email = caseData.getGeneralAppApplnSolicitor().getEmail();
        if (null != email) {
            notificationService.sendMail(
                    email,
                    gaForLipService.isLipApp(caseData) ? notificationProperties.getLipGeneralAppApplicantEmailTemplate()
                        : notificationProperties.getEvidenceUploadTemplate(),
                    addProperties(caseData),
                    String.format(
                            REFERENCE_TEMPLATE_DOC_UPLOAD,
                            caseData.getCcdCaseReference()
                    )
            );
        }
    }

    public void notifyRespondentEvidenceUpload(CaseData caseData) throws NotificationException {
        caseData.getGeneralAppRespondentSolicitors().forEach(
                respondentSolicitor -> {
                    notificationService.sendMail(
                            respondentSolicitor.getValue().getEmail(),
                            gaForLipService.isLipResp(caseData)
                                ? notificationProperties.getLipGeneralAppRespondentEmailTemplate()
                                : notificationProperties.getEvidenceUploadTemplate(),
                            addProperties(caseData),
                            String.format(
                                    REFERENCE_TEMPLATE_DOC_UPLOAD,
                                    caseData.getCcdCaseReference()
                            )
                    );
                });
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {

        if (gaForLipService.isGaForLip(caseData)) {
            String caseTitle = getAllPartyNames(caseData);
            customProps.put(
                CASE_TITLE,
                Objects.requireNonNull(caseTitle)
            );
        }
        if (gaForLipService.isLipApp(caseData)) {
            String isLipAppName = caseData.getApplicantPartyName();

            customProps.put(GA_LIP_APPLICANT_NAME, Objects.requireNonNull(isLipAppName));

        } else if (gaForLipService.isLipResp(caseData)) {

            String isLipRespondentName = caseData
                .getGeneralAppRespondentSolicitors().get(0).getValue().getForename()
                + " " + getSurname(caseData);
            customProps.put(
                GA_LIP_RESP_NAME,
                Objects.requireNonNull(isLipRespondentName)
            );
        } else {
            customProps.remove(GA_LIP_APPLICANT_NAME);
            customProps.remove(GA_LIP_RESP_NAME);
            customProps.remove(CASE_TITLE);
        }

        customProps.put(CASE_REFERENCE, caseData.getCcdCaseReference().toString());
        return customProps;
    }

    public static String getAllPartyNames(CaseData caseData) {
        return format("%s v %s%s",
                      caseData.getClaimant1PartyName(),
                      caseData.getDefendant1PartyName(),
                      nonNull(caseData.getDefendant2PartyName())
                          && (NO.equals(caseData.getRespondent2SameLegalRepresentative())
                          || Objects.isNull(caseData.getRespondent2SameLegalRepresentative()))
                          ? ", " + caseData.getDefendant2PartyName() : "");
    }

    public String getSurname(CaseData caseData) {
        String surname = "";

        if (caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getSurname().isPresent()) {
            surname = caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getSurname().get();
        }
        return surname;
    }
}
