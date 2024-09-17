package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandler;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil;

import java.util.List;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_HEARING_SCHEDULED_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_JUDGE_UNCLOAK_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_REQUEST_MORE_INFO_RESPONDENT;

@Service
public class CreateMakeDecisionDashboardNotificationForRespondentHandler extends DashboardCallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION);

    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;

    public CreateMakeDecisionDashboardNotificationForRespondentHandler(DashboardApiClient dashboardApiClient,
                                                                       DashboardNotificationsParamsMapper mapper,
                                                                       CoreCaseDataService coreCaseDataService,
                                                                       CaseDetailsConverter caseDetailsConverter,
                                                                       FeatureToggleService featureToggleService) {
        super(dashboardApiClient, mapper, featureToggleService);
        this.coreCaseDataService = coreCaseDataService;
        this.caseDetailsConverter = caseDetailsConverter;
    }

    @Override
    protected String getScenario(CaseData caseData) {

        if (isWithoutNotice(caseData)
            && caseData.getApplicationIsUncloakedOnce() != null
            && caseData.getApplicationIsUncloakedOnce().equals(YES)
            && caseData.getMakeAppVisibleToRespondents() != null) {
            return SCENARIO_AAA6_GENERAL_APPLICATION_JUDGE_UNCLOAK_RESPONDENT.getScenario();
        }

        if (isWithNoticeOrConsent(caseData)) {
            if (caseData.getJudicialDecisionRequestMoreInfo() != null
                && (GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION == caseData
                .getJudicialDecisionRequestMoreInfo().getRequestMoreInfoOption()
                || caseData.getCcdState().equals(CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION))) {

                return SCENARIO_AAA6_GENERAL_APPLICATION_REQUEST_MORE_INFO_RESPONDENT.getScenario();
            } else if (caseData.getCcdState().equals(CaseState.LISTING_FOR_A_HEARING) && caseData
                .getJudicialDecision().getDecision().equals(
                    GAJudgeDecisionOption.LIST_FOR_A_HEARING) && caseData.getGaHearingNoticeApplication() != null
                && caseData.getGaHearingNoticeDetail() != null) {

                return SCENARIO_AAA6_GENERAL_APPLICATION_HEARING_SCHEDULED_RESPONDENT.getScenario();
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
