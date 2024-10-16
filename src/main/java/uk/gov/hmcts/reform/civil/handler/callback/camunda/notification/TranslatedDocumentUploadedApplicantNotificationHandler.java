package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.OrganisationResponse;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.DocUploadNotificationService;
import uk.gov.hmcts.reform.civil.service.GaForLipService;
import uk.gov.hmcts.reform.civil.service.NotificationService;
import uk.gov.hmcts.reform.civil.service.OrganisationService;
import uk.gov.hmcts.reform.civil.service.SolicitorEmailValidation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;

@Service
@RequiredArgsConstructor
public class TranslatedDocumentUploadedApplicantNotificationHandler extends CallbackHandler
    implements NotificationData {

    private final NotificationService notificationService;
    private final NotificationsProperties notificationsProperties;
    private final CaseDetailsConverter caseDetailsConverter;
    private final GaForLipService gaForLipService;
    private final CoreCaseDataService coreCaseDataService;
    private final SolicitorEmailValidation solicitorEmailValidation;
    private final OrganisationService organisationService;

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.NOTIFY_APPLICANT_TRANSLATED_DOCUMENT_UPLOADED_GA);
    private static final String REFERENCE_TEMPLATE = "translated-document-uploaded-applicant-notification-%s";

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::notifyApplicant
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        if (gaForLipService.isLipApp(caseData)) {
            String caseTitle = DocUploadNotificationService.getAllPartyNames(caseData);
            String isLipAppName = caseData.getApplicantPartyName();
            return Map.of(
                CASE_TITLE, Objects.requireNonNull(caseTitle),
                GA_LIP_APPLICANT_NAME, Objects.requireNonNull(isLipAppName),
                CASE_REFERENCE, caseData.getCcdCaseReference().toString()
            );
        }
        return Map.of(
            CASE_REFERENCE, caseData.getCcdCaseReference().toString(),
            CLAIM_LEGAL_ORG_NAME_SPEC, getApplicantLegalOrganizationName(caseData)
        );
    }

    private CallbackResponse notifyApplicant(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData civilCaseData = caseDetailsConverter
            .toCaseData(coreCaseDataService
                            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));

        caseData = solicitorEmailValidation.validateSolicitorEmail(civilCaseData, caseData);
        notificationService.sendMail(
            caseData.getGeneralAppApplnSolicitor().getEmail(),
            addTemplate(caseData),
            addProperties(caseData),
            String.format(REFERENCE_TEMPLATE, caseData.getCcdCaseReference())
        );
        return AboutToStartOrSubmitCallbackResponse.builder().build();
    }

    private String addTemplate(CaseData caseData) {
        if (gaForLipService.isLipApp(caseData)) {
            if (caseData.isApplicantBilingual(caseData.getParentClaimantIsApplicant())) {
                return notificationsProperties.getNotifyApplicantLiPTranslatedDocumentUploadedWhenParentCaseInBilingual();
            }
            return notificationsProperties.getLipGeneralAppApplicantEmailTemplate();
        }
        return notificationsProperties.getNotifyLRTranslatedDocumentUploaded();

    }

    public String getApplicantLegalOrganizationName(CaseData caseData) {
        var organisationId = caseData.getGeneralAppApplnSolicitor().getOrganisationIdentifier();
        return organisationService.findOrganisationById(organisationId)
            .map(OrganisationResponse::getName)
            .orElseThrow(RuntimeException::new);
    }
}
