package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;
import uk.gov.hmcts.reform.civil.service.StateGeneratorService;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModifyStateAfterAdditionalFeeReceivedCallbackHandler extends CallbackHandler {

    private final ParentCaseUpdateHelper parentCaseUpdateHelper;
    private final StateGeneratorService stateGeneratorService;

    private static final List<CaseEvent> EVENTS = singletonList(MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
                callbackKey(ABOUT_TO_SUBMIT), this::changeApplicationState,
                callbackKey(SUBMITTED), this::changeGADetailsStatusInParent
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse changeApplicationState(CallbackParams callbackParams) {
        Long caseId = callbackParams.getCaseData().getCcdCaseReference();
        CaseData caseData = callbackParams.getCaseData();
        log.info("Changing state to ORDER_MADE for caseId: {}", caseId);
        return AboutToStartOrSubmitCallbackResponse.builder()
                .state(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData).getDisplayedValue())
                .build();
    }

    private CallbackResponse changeGADetailsStatusInParent(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        log.info("Updating parent with latest state of application-caseId: {}", caseData.getCcdCaseReference());
        parentCaseUpdateHelper.updateParentApplicationVisibilityWithNewState(
                caseData,
                stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData).getDisplayedValue()
        );

        return SubmittedCallbackResponse.builder().build();
    }

}
