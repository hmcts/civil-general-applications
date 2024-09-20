package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandler;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import java.util.List;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPS_HWF_REQUESTED_APPLICANT;

@Service
public class ApplyForHwFDashboardNotificationHandler extends DashboardCallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.NOTIFY_HELP_WITH_FEE);
    public ApplyForHwFDashboardNotificationHandler(DashboardApiClient dashboardApiClient,
                                                   DashboardNotificationsParamsMapper mapper,
                                                   FeatureToggleService featureToggleService) {
        super(dashboardApiClient, mapper, featureToggleService);
    }

    @Override
    protected String getScenario(CaseData caseData) {
        return SCENARIO_AAA6_GENERAL_APPS_HWF_REQUESTED_APPLICANT.getScenario();
    }

    @Override
    public boolean shouldRecordScenario(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        return caseData.getIsGaApplicantLip() == YesOrNo.YES;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }
}
