package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.utils.DocUploadUtils;
import uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil;
import uk.gov.hmcts.reform.dashboard.data.ScenarioRequestParams;

import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_RESPONSE_SUBMITTED_APPLICANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_RESPONSE_SUBMITTED_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_OTHER_PARTY_UPLOADED_DOC_APPLICANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_OTHER_PARTY_UPLOADED_DOC_RESPONDENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocUploadDashboardNotificationService {

    private final DashboardApiClient dashboardApiClient;
    private final FeatureToggleService featureToggleService;
    private final GaForLipService gaForLipService;
    private final DashboardNotificationsParamsMapper mapper;

    public void createDashboardNotification(CaseData caseData, String role, String authToken) {

        if (isWithNoticeOrConsent(caseData) && featureToggleService.isDashboardServiceEnabled()) {
            log.info("Case {} is with notice or consent and the dashboard service is enabled", caseData.getCcdCaseReference());
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

    public void createResponseDashboardNotification(CaseData caseData, String role, String authToken) {

        if ((role.equalsIgnoreCase("APPLICANT")
            || (isWithNoticeOrConsent(caseData) && role.equalsIgnoreCase("RESPONDENT")))
            && featureToggleService.isDashboardServiceEnabled()) {
            String scenario = getResponseDashboardScenario(role, caseData);
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

    private String getResponseDashboardScenario(String role, CaseData caseData) {
        if (role.equalsIgnoreCase("APPLICANT") && gaForLipService.isLipApp(caseData)) {
            return SCENARIO_AAA6_GENERAL_APPLICATION_RESPONSE_SUBMITTED_APPLICANT.getScenario();
        } else if (role.equalsIgnoreCase("RESPONDENT") && gaForLipService.isLipResp(caseData)) {
            return SCENARIO_AAA6_GENERAL_APPLICATION_RESPONSE_SUBMITTED_RESPONDENT.getScenario();
        }
        return null;
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
