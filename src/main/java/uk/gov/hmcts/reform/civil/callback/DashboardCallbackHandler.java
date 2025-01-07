package uk.gov.hmcts.reform.civil.callback;

import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;

import com.google.common.base.Strings;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import uk.gov.hmcts.reform.dashboard.data.ScenarioRequestParams;

@Slf4j
@RequiredArgsConstructor
public abstract class DashboardCallbackHandler extends CallbackHandler {

    protected final DashboardApiClient dashboardApiClient;
    protected final DashboardNotificationsParamsMapper mapper;
    protected final FeatureToggleService featureToggleService;

    @Override
    protected Map<String, Callback> callbacks() {
        return featureToggleService.isDashboardServiceEnabled() && featureToggleService.isGaForLipsEnabled()
            ? Map.of(callbackKey(ABOUT_TO_SUBMIT), this::configureDashboardScenario)
            : Map.of(callbackKey(ABOUT_TO_SUBMIT), this::emptyCallbackResponse);
    }

    protected abstract String getScenario(CaseData caseData);

    protected boolean isMainCase() {
        return false;
    }

    /**
     * Depending on the case data, the scenario may or may not be applicable.
     *
     * @param callbackParams handler's callback params
     * @return true if the scenario/notification should be recorded
     */
    protected boolean shouldRecordScenario(CallbackParams callbackParams) {
        return true;
    }

    public CallbackResponse configureDashboardScenario(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        String authToken = callbackParams.getParams().get(BEARER_TOKEN).toString();
        String scenario = getScenario(caseData);
        ScenarioRequestParams scenarioParams = ScenarioRequestParams.builder().params(mapper.mapCaseDataToParams(
            caseData)).build();
        log.info("Configure dashboard scenario for case id: {}", callbackParams.getCaseData().getCcdCaseReference());

        if (!Strings.isNullOrEmpty(scenario) && shouldRecordScenario(callbackParams)) {
            dashboardApiClient.recordScenario(
                isMainCase() ? caseData.getParentCaseReference() : caseData.getCcdCaseReference().toString(),
                scenario,
                authToken,
                scenarioParams
            );
        }

        return AboutToStartOrSubmitCallbackResponse.builder().build();
    }
}
