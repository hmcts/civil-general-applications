package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandler;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;

import java.util.List;

import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPS_APPLICATION_FEE_REQUIRED_APPLICANT;

@Service
public class ApplicationIssuedFeeRequiredHandler extends DashboardCallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.CREATE_DASHBOARD_NOTIFICATION_FOR_GA_APPLICANT);

    public ApplicationIssuedFeeRequiredHandler(DashboardApiClient dashboardApiClient,
                                               DashboardNotificationsParamsMapper mapper,
                                               FeatureToggleService featureToggleService) {
        super(dashboardApiClient, mapper, featureToggleService);
    }

    @Override
    protected String getScenario(CaseData caseData) {
        return SCENARIO_AAA6_GENERAL_APPS_APPLICATION_FEE_REQUIRED_APPLICANT.getScenario();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

}
