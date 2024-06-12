package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_CLAIMANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_DEFENDANT;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandler;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;

@Service
public class ApplicationCreatedTaskListUpdateHandler extends DashboardCallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.UPDATE_TASK_LIST_GA_CREATED);

    public ApplicationCreatedTaskListUpdateHandler(DashboardApiClient dashboardApiClient,
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
        if (caseData.getParentClaimantIsApplicant() == YesOrNo.YES) {
            return SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_CLAIMANT.getScenario();
        } else {
            return SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_DEFENDANT.getScenario();
        }
    }

    @Override
    public boolean shouldRecordScenario(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        return caseData.getIsGaApplicantLip() == YesOrNo.YES;
    }

}
