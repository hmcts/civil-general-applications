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
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.OrganisationResponse;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.DocUploadNotificationService;
import uk.gov.hmcts.reform.civil.service.GaForLipService;
import uk.gov.hmcts.reform.civil.service.NotificationService;
import uk.gov.hmcts.reform.civil.service.OrganisationService;
import uk.gov.hmcts.reform.civil.service.SolicitorEmailValidation;
import uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.areRespondentSolicitorsPresent;

@Service
@RequiredArgsConstructor
public class TranslatedDocumentUploadedRespondentNotificationHandler extends CallbackHandler
    implements NotificationData {

    private final NotificationService notificationService;
    private final NotificationsProperties notificationsProperties;
    private final CaseDetailsConverter caseDetailsConverter;
    private final GaForLipService gaForLipService;
    private final CoreCaseDataService coreCaseDataService;
    private final SolicitorEmailValidation solicitorEmailValidation;
    private final OrganisationService organisationService;

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.NOTIFY_RESPONDENT_TRANSLATED_DOCUMENT_UPLOADED_GA);
    private static final String REFERENCE_TEMPLATE = "translated-document-uploaded-applicant-notification-%s";

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::notifyRespondent
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        return Map.of();
    }

    public Map<String, String> addPropertiesForRespondent(CaseData caseData,
                                                          Element<GASolicitorDetailsGAspec> respondentSolicitor) {
        if (gaForLipService.isLipResp(caseData)) {
            String caseTitle = DocUploadNotificationService.getAllPartyNames(caseData);
            String isLipResName =
                caseData.getParentClaimantIsApplicant().equals(NO) ? caseData.getClaimant1PartyName() :
                    caseData.getDefendant1PartyName();
            return Map.of(
                CASE_TITLE, Objects.requireNonNull(caseTitle),
                GA_LIP_RESP_NAME, Objects.requireNonNull(isLipResName),
                CASE_REFERENCE, caseData.getCcdCaseReference().toString()
            );
        }
        return Map.of(
            CASE_REFERENCE, caseData.getCcdCaseReference().toString(),
            CLAIM_LEGAL_ORG_NAME_SPEC, getApplicantLegalOrganizationName(respondentSolicitor)
        );
    }

    private CallbackResponse notifyRespondent(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();

        CaseData civilCaseData = caseDetailsConverter
            .toCaseData(coreCaseDataService
                            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));

        CaseData validatedCaseData = solicitorEmailValidation.validateSolicitorEmail(civilCaseData, caseData);

        if (areRespondentSolicitorsPresent(validatedCaseData)
            && (JudicialDecisionNotificationUtil.isWithNotice(caseData)
            || caseData.getGeneralAppConsentOrder() == YesOrNo.YES)) {
            validatedCaseData.getGeneralAppRespondentSolicitors().forEach(respondentSolicitor ->
                                                                              notificationService.sendMail(
                                                                                  respondentSolicitor.getValue()
                                                                                      .getEmail(),
                                                                                  addTemplate(
                                                                                      caseData,
                                                                                      civilCaseData
                                                                                  ),
                                                                                  addPropertiesForRespondent(
                                                                                      caseData,
                                                                                      respondentSolicitor
                                                                                  ),
                                                                                  String.format(
                                                                                      REFERENCE_TEMPLATE,
                                                                                      caseData.getCcdCaseReference()
                                                                                  )
                                                                              )
            );
        }

        return AboutToStartOrSubmitCallbackResponse.builder().build();
    }

    private String addTemplate(CaseData caseData, CaseData civilCaseData) {
        if (gaForLipService.isLipResp(caseData)) {
            if (civilCaseData.isRespondentBilingual(caseData.getParentClaimantIsApplicant())) {
                return notificationsProperties.getNotifyRespondentLiPTranslatedDocumentUploadedWhenParentCaseInBilingual();
            }
            return notificationsProperties.getLipGeneralAppRespondentEmailTemplate();
        }
        return notificationsProperties.getNotifyLRTranslatedDocumentUploaded();

    }

    public String getApplicantLegalOrganizationName(Element<GASolicitorDetailsGAspec> respondentSolicitor) {
        var organisationId = respondentSolicitor.getValue().getOrganisationIdentifier();
        return organisationService.findOrganisationById(organisationId)
            .map(OrganisationResponse::getName)
            .orElseThrow(RuntimeException::new);
    }
}