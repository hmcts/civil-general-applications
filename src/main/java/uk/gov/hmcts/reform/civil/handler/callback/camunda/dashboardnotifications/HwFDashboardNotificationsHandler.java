package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandler;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NO_REMISSION_HWF_GA;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPLICANT_LIP_HWF_DASHBOARD_NOTIFICATION;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPS_HWF_REJECTED_APPLICANT;

@Service
public class HwFDashboardNotificationsHandler extends DashboardCallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(APPLICANT_LIP_HWF_DASHBOARD_NOTIFICATION);

    public HwFDashboardNotificationsHandler(DashboardApiClient dashboardApiClient,
                                            DashboardNotificationsParamsMapper mapper,
                                            FeatureToggleService featureToggleService) {
        super(dashboardApiClient, mapper, featureToggleService);
    }

    public final Map<CaseEvent, String> dashboardScenarios = Map.of(
        NO_REMISSION_HWF_GA, SCENARIO_AAA6_GENERAL_APPS_HWF_REJECTED_APPLICANT.getScenario()
    );

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    @Override
    public String getScenario(CaseData caseData) {
        if (FeeType.APPLICATION == caseData.getHwfFeeType()) {
            return dashboardScenarios.get(caseData.getGaHwfDetails().getHwfCaseEvent());
        } else if (FeeType.ADDITIONAL == caseData.getHwfFeeType()) {
            return dashboardScenarios.get(caseData.getAdditionalHwfDetails().getHwfCaseEvent());
        }
        return "";
    }
}
