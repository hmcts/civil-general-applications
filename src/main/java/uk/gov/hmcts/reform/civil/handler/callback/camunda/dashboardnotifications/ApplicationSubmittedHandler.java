package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandlerNew;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;

import java.util.List;

import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_CLAIMANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_DEFENDANT;

@Service
public class ApplicationSubmittedHandler extends DashboardCallbackHandlerNew {

        private static final List<CaseEvent> EVENTS = List.of(CaseEvent.CREATE_DASHBOARD_NOTIFICATION_FOR_GA_APPLICANT_SUBMITTED);

        public ApplicationSubmittedHandler(DashboardApiClient dashboardApiClient,
                                                       DashboardNotificationsParamsMapper mapper,
                                                       FeatureToggleService featureToggleService) {
            super(dashboardApiClient, mapper, featureToggleService);
        }

        @Override
        protected String getScenario(CaseData caseData) {
            if (caseData.getParentClaimantIsApplicant() == YesOrNo.YES) {
                return SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_CLAIMANT.getScenario();
            } else {
                return SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_DEFENDANT.getScenario();
            }
        }

        @Override
        public List<CaseEvent> handledEvents() {
            return EVENTS;
        }

        @Override
        public boolean shouldRecordScenario(CallbackParams callbackParams) {
            return true;

        }
}
