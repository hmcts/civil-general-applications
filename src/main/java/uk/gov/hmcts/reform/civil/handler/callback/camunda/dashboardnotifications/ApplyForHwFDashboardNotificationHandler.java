package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import uk.gov.hmcts.reform.civil.service.GaForLipService;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;
import uk.gov.hmcts.reform.civil.utils.HwFFeeTypeService;
import uk.gov.hmcts.reform.dashboard.data.ScenarioRequestParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;

@Service
@RequiredArgsConstructor
public class ApplyForHwFDashboardNotificationHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.NOTIFY_HELP_WITH_FEE);
    private final ObjectMapper objectMapper;
    private final DashboardApiClient dashboardApiClient;
    protected final DashboardNotificationsParamsMapper mapper;
    private final GaForLipService gaForLipService;
    private final ParentCaseUpdateHelper parentCaseUpdateHelper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::updateHWFDetailsAndSendNotification
        );
    }

    private CallbackResponse updateHWFDetailsAndSendNotification(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData().toBuilder().build();
        CaseData.CaseDataBuilder caseDataBuilder = HwFFeeTypeService.updateHwfDetails(caseData);
        String authToken = callbackParams.getParams().get(BEARER_TOKEN).toString();
        HashMap<String, Object> paramsMap = mapper.mapCaseDataToParams(caseData);
        if ((caseData.getGeneralAppPBADetails().getPaymentDetails() == null
            || caseData.getGeneralAppPBADetails().getAdditionalPaymentDetails() == null)
            && gaForLipService.isGaForLip(caseData) && Objects.nonNull(caseData.getGeneralAppHelpWithFees())
                && caseData.getGeneralAppHelpWithFees().getHelpWithFee().equals(YesOrNo.YES)) {
            parentCaseUpdateHelper.updateMasterCollectionForHwf(caseData);
        }

        dashboardApiClient.recordScenario(caseData.getCcdCaseReference().toString(),
                                          DashboardScenarios.SCENARIO_AAA6_GENERAL_APPS_HWF_REQUESTED_APPLICANT.getScenario(),
                                          authToken,
                                          ScenarioRequestParams.builder().params(paramsMap).build()

        );
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }
}
