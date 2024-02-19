package uk.gov.hmcts.reform.civil.service;

import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocUploadNotificationService implements NotificationData {

    private final NotificationService notificationService;
    private final NotificationsProperties notificationProperties;
    private static final String REFERENCE_TEMPLATE_DOC_UPLOAD = "general-apps-notice-of-document-upload-%s";

    public void notifyApplicantEvidenceUpload(CaseData caseData) throws NotificationException {
        String email = caseData.getGeneralAppApplnSolicitor().getEmail();
        if (null != email) {
            notificationService.sendMail(
                    email,
                    notificationProperties.getEvidenceUploadTemplate(),
                    addProperties(caseData),
                    String.format(
                            REFERENCE_TEMPLATE_DOC_UPLOAD,
                            caseData.getLegacyCaseReference()
                    )
            );
        }
    }

    public void notifyRespondentEvidenceUpload(CaseData caseData) throws NotificationException {
        caseData.getGeneralAppRespondentSolicitors().forEach(
                respondentSolicitor -> {
                    notificationService.sendMail(
                            respondentSolicitor.getValue().getEmail(),
                            notificationProperties.getEvidenceUploadTemplate(),
                            addProperties(caseData),
                            String.format(
                                    REFERENCE_TEMPLATE_DOC_UPLOAD,
                                    caseData.getLegacyCaseReference()
                            )
                    );
                });
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        return Map.of(
                CASE_REFERENCE, caseData.getLegacyCaseReference()
        );
    }
}
