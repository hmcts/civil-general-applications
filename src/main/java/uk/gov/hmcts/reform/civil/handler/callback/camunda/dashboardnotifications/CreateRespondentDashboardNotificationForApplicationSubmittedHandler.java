package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandler;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;

import java.util.List;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_URGENT_RESPONDENT;

@Service
public class CreateRespondentDashboardNotificationForApplicationSubmittedHandler extends DashboardCallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.CREATE_APPLICATION_SUBMITTED_DASHBOARD_NOTIFICATION_FOR_RESPONDENT);

    public CreateRespondentDashboardNotificationForApplicationSubmittedHandler(DashboardApiClient dashboardApiClient,
                                                                DashboardNotificationsParamsMapper mapper,
                                                                FeatureToggleService featureToggleService) {
    super(dashboardApiClient, mapper, featureToggleService);
    }

    @Override
    protected String getScenario(CaseData caseData) {
        if (isWithNoticeOrConsent(caseData)) {
            if (isUrgent(caseData)) {
                return SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_URGENT_RESPONDENT.getScenario();
            } else {
                return SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_RESPONDENT.getScenario();
            }
        }
        return "";
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    @Override
    public boolean shouldRecordScenario(CallbackParams callbackParams) {
        return isWithNoticeOrConsent(callbackParams.getCaseData());
    }

    private boolean isWithNoticeOrConsent(CaseData caseData) {
        return (YES.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice())
            || caseData.getGeneralAppConsentOrder() == YesOrNo.YES);
    }

    private boolean isUrgent(CaseData caseData) {
        return Optional.ofNullable(caseData.getGeneralAppUrgencyRequirement())
            .map(GAUrgencyRequirement::getGeneralAppUrgency)
            .filter(urgency -> urgency == YES)
            .isPresent();
    }
}
