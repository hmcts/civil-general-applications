package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandler;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;

import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_HEARING_SCHEDULED_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_JUDGE_UNCLOAK_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_ORDER_MADE_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_REQUEST_MORE_INFO_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_WRITTEN_REPRESENTATION_REQUIRED_RESPONDENT;

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
        if (isWithoutNotice(caseData)
            && caseData.getApplicationIsUncloakedOnce() != null
            && caseData.getApplicationIsUncloakedOnce().equals(YES)) {
            if (caseData.getMakeAppVisibleToRespondents() != null) {
                return SCENARIO_AAA6_GENERAL_APPLICATION_JUDGE_UNCLOAK_RESPONDENT.getScenario();
            } else {
                return getScenarioBasedOnDecision(caseData);
            }
        }

        if (isWithNoticeOrConsent(caseData)) {
            return getScenarioBasedOnDecision(caseData);
        }

        return "";
    }

    private String getScenarioBasedOnDecision(CaseData caseData) {
        if (caseData.getCcdState().equals(CaseState.LISTING_FOR_A_HEARING) && caseData
            .getJudicialDecision().getDecision().equals(
                GAJudgeDecisionOption.LIST_FOR_A_HEARING) && caseData.getGaHearingNoticeApplication() != null
            && caseData.getGaHearingNoticeDetail() != null) {
            return SCENARIO_AAA6_GENERAL_APPLICATION_HEARING_SCHEDULED_RESPONDENT.getScenario();
        } else if (caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations() != null
            && caseData.getJudicialDecision() != null
            && caseData.getJudicialDecision().getDecision() == GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS) {
            return SCENARIO_AAA6_GENERAL_APPLICATION_WRITTEN_REPRESENTATION_REQUIRED_RESPONDENT.getScenario();
        } else if (caseData.judgeHasMadeAnOrder()
            && caseData.getCcdState().equals(CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)) {
            return SCENARIO_AAA6_GENERAL_APPLICATION_ORDER_MADE_RESPONDENT.getScenario();
        } else if (caseData.getJudicialDecisionRequestMoreInfo() != null
            && (GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION == caseData.getJudicialDecisionRequestMoreInfo().getRequestMoreInfoOption()
            || caseData.getCcdState().equals(CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION))) {
            if (Objects.isNull(caseData.getJudicialDecisionRequestMoreInfo().getRequestMoreInfoOption())) {
                return SCENARIO_AAA6_GENERAL_APPLICATION_REQUEST_MORE_INFO_RESPONDENT.getScenario();
            }
        }
        return "";
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private boolean isWithNoticeOrConsent(CaseData caseData) {
        return YES.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice())
            || caseData.getGeneralAppConsentOrder() == YesOrNo.YES;
    }

    private boolean isWithoutNotice(CaseData caseData) {
        return NO.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice());
    }

}
