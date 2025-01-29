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
import uk.gov.hmcts.reform.civil.service.GaForLipService;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;

import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPS_HWF_REQUESTED_APPLICANT;

@Service
public class ApplyForHwFDashboardNotificationHandler extends DashboardCallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.NOTIFY_HELP_WITH_FEE);
    private GaForLipService gaForLipService;
    private ParentCaseUpdateHelper parentCaseUpdateHelper;

    public ApplyForHwFDashboardNotificationHandler(DashboardApiClient dashboardApiClient,
                                                   DashboardNotificationsParamsMapper mapper,
                                                   FeatureToggleService featureToggleService,
                                                   GaForLipService gaForLipService,
                                                   ParentCaseUpdateHelper parentCaseUpdateHelper) {
        super(dashboardApiClient, mapper, featureToggleService);
        this.gaForLipService = gaForLipService;
        this.parentCaseUpdateHelper = parentCaseUpdateHelper;
    }

    @Override
    protected String getScenario(CaseData caseData) {

        if (caseData.getGeneralAppPBADetails().getPaymentDetails() == null
            || caseData.getGeneralAppPBADetails().getAdditionalPaymentDetails() == null) {

            /*
             * GA for LIP
             * When Caseworker should have access to GA to perform HelpWithFee then,
             * Add GA into collections
             * */
            if (gaForLipService.isGaForLip(caseData) && Objects.nonNull(caseData.getGeneralAppHelpWithFees())
                && caseData.getGeneralAppHelpWithFees().getHelpWithFee().equals(YesOrNo.YES)) {

                parentCaseUpdateHelper.updateMasterCollectionForHwf(caseData);
            }
        }

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
