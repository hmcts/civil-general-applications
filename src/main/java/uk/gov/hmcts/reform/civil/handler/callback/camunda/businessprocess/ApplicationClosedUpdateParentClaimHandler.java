package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPLICATION_CLOSED_UPDATE_PARENT_CLAIM;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_CLOSED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationClosedUpdateParentClaimHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(
        APPLICATION_CLOSED_UPDATE_PARENT_CLAIM);
    public static final String TASK_ID = "ApplicationClosedUpdateParentClaim";

    private final ParentCaseUpdateHelper parentCaseUpdateHelper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT),
            this::applicationClosedUpdateParentClaim
        );
    }

    @Override
    public String camundaActivityId(CallbackParams callbackParams) {
        return TASK_ID;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse applicationClosedUpdateParentClaim(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        if (caseData.getBusinessProcess() != null
            && "MAIN_CASE_CLOSED".equals(caseData.getBusinessProcess().getCamundaEvent())
            && APPLICATION_CLOSED.equals(caseData.getCcdState())) {
            log.info("Updating parent with latest state of application-caseId: {}", caseData.getCcdCaseReference());
            parentCaseUpdateHelper.updateParentWithGAState(
                caseData,
                caseData.getCcdState().getDisplayedValue()
            );
        }
        return AboutToStartOrSubmitCallbackResponse.builder().build();
    }

}
