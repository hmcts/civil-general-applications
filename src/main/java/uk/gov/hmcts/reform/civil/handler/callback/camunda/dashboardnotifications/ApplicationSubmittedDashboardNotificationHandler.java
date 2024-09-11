package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandler;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_APPLICANT;

public class ApplicationSubmittedDashboardNotificationHandler extends DashboardCallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.UPDATE_GA_DASHBOARD_NOTIFICATION);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::gaAppSubmitNotification);
    }

    public ApplicationSubmittedDashboardNotificationHandler(DashboardApiClient dashboardApiClient,
                                                                DashboardNotificationsParamsMapper mapper,
                                                                FeatureToggleService featureToggleService) {
        super(dashboardApiClient, mapper, featureToggleService);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    @Override
    public String getScenario(CaseData caseData) {
        return SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_APPLICANT.getScenario();
    }

    private CallbackResponse gaAppSubmitNotification(CallbackParams callbackParams) {
        Map<String, Object> output = callbackParams.getRequest().getCaseDetails().getData();
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(output)
            .build();
    }

}
