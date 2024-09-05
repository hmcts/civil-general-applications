package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.genapplication.UploadDocumentByType;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import uk.gov.hmcts.reform.civil.service.GaForLipService;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;
import uk.gov.hmcts.reform.civil.utils.DocUploadUtils;
import uk.gov.hmcts.reform.civil.utils.ElementUtils;
import uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil;
import uk.gov.hmcts.reform.dashboard.data.ScenarioRequestParams;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPLOAD_ADDL_DOCUMENTS;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_OTHER_PARTY_UPLOADED_DOC_APPLICANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_OTHER_PARTY_UPLOADED_DOC_RESPONDENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadAdditionalDocumentsCallbackHandler extends CallbackHandler {

    private static final String CONFIRMATION_MESSAGE = "### File has been uploaded successfully.";
    private static final List<CaseEvent> EVENTS = Collections.singletonList(UPLOAD_ADDL_DOCUMENTS);
    private static final String BUNDLE = "bundle";
    private final ObjectMapper objectMapper;
    private final AssignCategoryId assignCategoryId;
    private final CaseDetailsConverter caseDetailsConverter;

    private final IdamClient idamClient;
    private final DashboardApiClient dashboardApiClient;
    private final FeatureToggleService featureToggleService;
    private final GaForLipService gaForLipService;
    private final DashboardNotificationsParamsMapper mapper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::submitDocuments,
                      callbackKey(SUBMITTED), this::submittedConfirmation
        );
    }

    private CallbackResponse submitDocuments(CallbackParams callbackParams) {
        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        String authToken = callbackParams.getParams().get(BEARER_TOKEN).toString();
        String userId = idamClient.getUserInfo(authToken).getUid();
        caseData = buildBundleData(caseData, userId);
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        String role = DocUploadUtils.getUserRole(caseData, userId);
        DocUploadUtils.addUploadDocumentByTypeToAddl(caseData, caseDataBuilder,
                                                     caseData.getUploadDocument(), role, true
        );

        caseDataBuilder.uploadDocument(null);
        caseDataBuilder.businessProcess(BusinessProcess.ready(UPLOAD_ADDL_DOCUMENTS)).build();
        CaseData updatedCaseData = caseDataBuilder.build();

        if (isWithNoticeOrConsent(caseData)) {
            createDashboardNotification(caseData, role, authToken);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(updatedCaseData.toMap(objectMapper))
            .build();
    }

    private CaseData buildBundleData(CaseData caseData, String userId) {
        String role = DocUploadUtils.getUserRole(caseData, userId);
        if (Objects.nonNull(caseData.getUploadDocument())) {
            List<Element<UploadDocumentByType>> exBundle = caseData.getUploadDocument()
                .stream().filter(x -> !x.getValue().getDocumentType().toLowerCase()
                    .contains(BUNDLE))
                .collect(Collectors.toList());
            List<Element<CaseDocument>> bundle = caseData.getUploadDocument()
                .stream().filter(x -> x.getValue().getDocumentType().toLowerCase()
                    .contains(BUNDLE))
                .map(byType -> ElementUtils.element(CaseDocument.builder()
                                                        .documentLink(byType.getValue().getAdditionalDocument())
                                                        .documentName(byType.getValue().getDocumentType())
                                                        .createdBy(role)
                                                        .createdDatetime(LocalDateTime.now()).build()))
                .collect(Collectors.toList());
            assignCategoryId.assignCategoryIdToCollection(
                bundle,
                document -> document.getValue().getDocumentLink(),
                AssignCategoryId.APPLICATIONS
            );
            if (Objects.nonNull(caseData.getGaAddlDocBundle())) {
                bundle.addAll(caseData.getGaAddlDocBundle());
            }
            return caseData.toBuilder().uploadDocument(exBundle).gaAddlDocBundle(bundle).build();
        }
        return caseData;
    }

    private CallbackResponse submittedConfirmation(CallbackParams callbackParams) {
        String body = "<br/> <br/>";
        return SubmittedCallbackResponse.builder()
            .confirmationHeader(CONFIRMATION_MESSAGE)
            .confirmationBody(body)
            .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private void createDashboardNotification(CaseData caseData, String role, String authToken) {

        if (featureToggleService.isDashboardServiceEnabled() && gaForLipService.isGaForLip(caseData)) {
            String scenario = getDashboardScenario(role, caseData);
            ScenarioRequestParams scenarioParams = ScenarioRequestParams.builder().params(mapper.mapCaseDataToParams(
                caseData)).build();
            if (scenario != null) {
                dashboardApiClient.recordScenario(
                    caseData.getCcdCaseReference().toString(),
                    scenario,
                    authToken,
                    scenarioParams
                );
            }
        }
    }

    private String getDashboardScenario(String role, CaseData caseData) {
        if (DocUploadUtils.APPLICANT.equals(role) && gaForLipService.isLipResp(caseData)) {
            return SCENARIO_OTHER_PARTY_UPLOADED_DOC_RESPONDENT.getScenario();
        } else if (DocUploadUtils.RESPONDENT_ONE.equals(role) && gaForLipService.isLipApp(caseData)) {
            return SCENARIO_OTHER_PARTY_UPLOADED_DOC_APPLICANT.getScenario();
        }
        return null;
    }

    private boolean isWithNoticeOrConsent(CaseData caseData) {
        return JudicialDecisionNotificationUtil.isWithNotice(caseData)
            || caseData.getGeneralAppConsentOrder() == YesOrNo.YES;
    }
}
