package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandler;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import uk.gov.hmcts.reform.civil.service.GeneralAppFeesService;

import java.util.List;

import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_APPLICANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPS_APPLICATION_FEE_REQUIRED_APPLICANT;

@Service
public class ApplicationIssuedFeeRequiredHandler extends DashboardCallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.CREATE_DASHBOARD_NOTIFICATION_FOR_GA_APPLICANT);
    private final GeneralAppFeesService generalAppFeesService;

    public ApplicationIssuedFeeRequiredHandler(DashboardApiClient dashboardApiClient,
                                               DashboardNotificationsParamsMapper mapper,
                                               FeatureToggleService featureToggleService,
                                               GeneralAppFeesService generalAppFeesService) {
        super(dashboardApiClient, mapper, featureToggleService);
        this.generalAppFeesService = generalAppFeesService;
    }

    @Override
    protected String getScenario(CaseData caseData) {

        if (generalAppFeesService.isFreeApplication(caseData)) {
            return SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_APPLICANT.getScenario();
        }
        return SCENARIO_AAA6_GENERAL_APPS_APPLICATION_FEE_REQUIRED_APPLICANT.getScenario();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

}
