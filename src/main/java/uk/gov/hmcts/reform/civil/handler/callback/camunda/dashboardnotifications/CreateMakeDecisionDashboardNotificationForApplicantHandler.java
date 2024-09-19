package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandler;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import uk.gov.hmcts.reform.civil.service.JudicialDecisionHelper;

import java.util.List;

import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_HEARING_SCHEDULED_APPLICANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_ADDITIONAL_PAYMENT_APPLICANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_REQUEST_MORE_INFO_APPLICANT;

@Service
public class CreateMakeDecisionDashboardNotificationForApplicantHandler extends DashboardCallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.CREATE_APPLICANT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION);

    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final JudicialDecisionHelper judicialDecisionHelper;

    public CreateMakeDecisionDashboardNotificationForApplicantHandler(DashboardApiClient dashboardApiClient,
                                                                      DashboardNotificationsParamsMapper mapper,
                                                                      CoreCaseDataService coreCaseDataService,
                                                                      CaseDetailsConverter caseDetailsConverter,
                                                                      FeatureToggleService featureToggleService,
                                                                      JudicialDecisionHelper judicialDecisionHelper) {
        super(dashboardApiClient, mapper, featureToggleService);
        this.coreCaseDataService = coreCaseDataService;
        this.caseDetailsConverter = caseDetailsConverter;
        this.judicialDecisionHelper = judicialDecisionHelper;
    }

    @Override
    protected String getScenario(CaseData caseData) {
        if (caseData.getJudicialDecisionRequestMoreInfo() != null
            && (GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION == caseData.getJudicialDecisionRequestMoreInfo().getRequestMoreInfoOption()
            || caseData.getCcdState().equals(CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION))) {
            if (GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY == caseData.getJudicialDecisionRequestMoreInfo().getRequestMoreInfoOption()
                && judicialDecisionHelper.isApplicationUncloakedWithAdditionalFee(caseData)) {
                return SCENARIO_AAA6_GENERAL_APPLICATION_ADDITIONAL_PAYMENT_APPLICANT.getScenario();
            }
            return SCENARIO_AAA6_GENERAL_APPLICATION_REQUEST_MORE_INFO_APPLICANT.getScenario();
        } else if (caseData.getCcdState().equals(CaseState.LISTING_FOR_A_HEARING)
            && caseData.getJudicialDecision().getDecision().equals(GAJudgeDecisionOption.LIST_FOR_A_HEARING)
            && caseData.getGaHearingNoticeApplication() != null
            && caseData.getGaHearingNoticeDetail() != null) {
            return SCENARIO_AAA6_GENERAL_APPLICATION_HEARING_SCHEDULED_APPLICANT.getScenario();
        }
        return "";
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }
}
