package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.utils.DocUploadUtils;
import uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil;
import uk.gov.hmcts.reform.dashboard.data.ScenarioRequestParams;

import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_OTHER_PARTY_UPLOADED_DOC_APPLICANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_OTHER_PARTY_UPLOADED_DOC_RESPONDENT;

@Service
@RequiredArgsConstructor
public class DocUploadDashboardNotificationService {

    private final DashboardApiClient dashboardApiClient;
    private final FeatureToggleService featureToggleService;
    private final GaForLipService gaForLipService;
    private final DashboardNotificationsParamsMapper mapper;

    public void createDashboardNotification(CaseData caseData, String role, String authToken) {

        if (isWithNoticeOrConsent(caseData) && featureToggleService.isDashboardServiceEnabled()) {
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