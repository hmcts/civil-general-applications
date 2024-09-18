package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandler;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import java.util.List;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_JUDGE_UNCLOAK_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_ORDER_MADE_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_REQUEST_MORE_INFO_RESPONDENT;

@Service
public class CreateMakeDecisionDashboardNotificationForRespondentHandler extends DashboardCallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION);

    public CreateMakeDecisionDashboardNotificationForRespondentHandler(DashboardApiClient dashboardApiClient,
                                                                       DashboardNotificationsParamsMapper mapper,
                                                                       FeatureToggleService featureToggleService) {
        super(dashboardApiClient, mapper, featureToggleService);
    }

    @Override
    protected String getScenario(CaseData caseData) {
        if (isWithNoticeOrConsent(caseData)) {
            if (caseData.getJudicialDecisionRequestMoreInfo() != null
            && (GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION == caseData.getJudicialDecisionRequestMoreInfo().getRequestMoreInfoOption()
            || caseData.getCcdState().equals(CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION))) {
                return SCENARIO_AAA6_GENERAL_APPLICATION_REQUEST_MORE_INFO_RESPONDENT.getScenario();
            } else if (caseData.judgeHasMadeAnOrder()
                && caseData.getCcdState().equals(CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)) {
                return SCENARIO_AAA6_GENERAL_APPLICATION_ORDER_MADE_RESPONDENT.getScenario();
            }
        } else if (isWithoutNotice(caseData)
            && caseData.getApplicationIsUncloakedOnce() != null
            && caseData.getApplicationIsUncloakedOnce().equals(YES)
            && caseData.getMakeAppVisibleToRespondents() != null) {
            return SCENARIO_AAA6_GENERAL_APPLICATION_JUDGE_UNCLOAK_RESPONDENT.getScenario();
        }
        return "";
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private boolean isWithNoticeOrConsent(CaseData caseData) {
        return YES.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice())
            || caseData.getGeneralAppConsentOrder() == YES;
    }

    private boolean isWithoutNotice(CaseData caseData) {
        return NO.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice());
    }
}
