package uk.gov.hmcts.reform.civil.handler.callback.camunda.fee;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.config.GeneralAppFeesConfiguration;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.service.GeneralAppFeesService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.OBTAIN_ADDITIONAL_FEE_VALUE;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.notificationCriterion;
import static uk.gov.hmcts.reform.civil.utils.NotificationCriterion.APPLICATION_CHANGE_TO_WITH_NOTICE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObtainAdditionalFeeCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(OBTAIN_ADDITIONAL_FEE_VALUE);
    private static final String ERROR_MESSAGE_NO_FEE_IN_CASEDATA = "Application case data does not have fee details";
    private static final String ERROR_MESSAGE_FEE_CHANGED = "Fee has changed since application was submitted. "
        + "It needs to be validated again";
    private static final String TASK_ID = "ObtainAdditionalFeeValue";

    private final GeneralAppFeesService feeService;
    private final GeneralAppFeesConfiguration feesConfiguration;
    private final ObjectMapper objectMapper;

    @Override
    public String camundaActivityId(CallbackParams callbackParams) {
        return TASK_ID;
    }

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::validateFee
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse validateFee(CallbackParams callbackParams) {
        var caseData = callbackParams.getCaseData();

        if (notificationCriterion(caseData).equals(APPLICATION_CHANGE_TO_WITH_NOTICE)) {
            Fee feeForGA = feeService.getFeeForGA(feesConfiguration.getApplicationUncloakAdditionalFee());

            GAPbaDetails generalAppPBADetails = caseData.getGeneralAppPBADetails()
                .toBuilder().additionalUncloakFee(feeForGA).build();

            caseData = caseData.toBuilder().generalAppPBADetails(generalAppPBADetails).build();

        }
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .build();
    }
}
