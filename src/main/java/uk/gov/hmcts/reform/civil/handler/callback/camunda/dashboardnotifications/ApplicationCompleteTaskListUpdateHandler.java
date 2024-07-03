package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_COMPLETE_CLAIMANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_COMPLETE_DEFENDANT;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandler;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;

@Service
public class ApplicationCompleteTaskListUpdateHandler extends DashboardCallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.UPDATE_TASK_LIST_GA_COMPLETE);

    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;

    public ApplicationCompleteTaskListUpdateHandler(DashboardApiClient dashboardApiClient,
                                                    DashboardNotificationsParamsMapper mapper,
                                                    CoreCaseDataService coreCaseDataService,
                                                    CaseDetailsConverter caseDetailsConverter,
                                                    FeatureToggleService featureToggleService) {
        super(dashboardApiClient, mapper, featureToggleService);
        this.coreCaseDataService = coreCaseDataService;
        this.caseDetailsConverter = caseDetailsConverter;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    @Override
    public String getScenario(CaseData caseData) {
        if (caseData.getParentClaimantIsApplicant() == YesOrNo.YES) {
            return SCENARIO_AAA6_GENERAL_APPLICATION_COMPLETE_CLAIMANT.getScenario();
        } else {
            return SCENARIO_AAA6_GENERAL_APPLICATION_COMPLETE_DEFENDANT.getScenario();
        }
    }

    @Override
    public boolean shouldRecordScenario(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseDetails caseDetails = coreCaseDataService.getCase(Long.parseLong(caseData.getParentCaseReference()));
        CaseData parentCaseData = caseDetailsConverter.toCaseData(caseDetails);
        boolean allApplicationsOrderMade = !parentCaseData.getClaimantGaAppDetails().stream()
            .map(Element::getValue)
            .filter(gaDetails -> !gaDetails.getCaseState().equals(ORDER_MADE.getDisplayedValue()))
            .findFirst()
            .isPresent();
        return caseData.getIsGaApplicantLip() == YesOrNo.YES && allApplicationsOrderMade;
    }
}
