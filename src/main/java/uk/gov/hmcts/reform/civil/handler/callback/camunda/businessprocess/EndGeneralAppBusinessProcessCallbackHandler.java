package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_BUSINESS_PROCESS_GASPEC;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;
import static uk.gov.hmcts.reform.civil.utils.ApplicationNotificationUtil.isNotificationCriteriaSatisfied;

@Service
@RequiredArgsConstructor
public class EndGeneralAppBusinessProcessCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(END_BUSINESS_PROCESS_GASPEC);

    private final CaseDetailsConverter caseDetailsConverter;
    private final ParentCaseUpdateHelper parentCaseUpdateHelper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::endGeneralApplicationBusinessProcess);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse endGeneralApplicationBusinessProcess(CallbackParams callbackParams) {
        CaseData data = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        if (INITIATE_GENERAL_APPLICATION.toString().equals(data.getBusinessProcess().getCamundaEvent()) && !"CratePDFDOcument".equals(data.getBusinessProcess().getActivityId())) {
            CaseState newState = isNotificationCriteriaSatisfied(data)
                ? AWAITING_RESPONDENT_RESPONSE
                : APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
            parentCaseUpdateHelper.updateParentWithGAState(data, newState.getDisplayedValue());
            return evaluateReady(callbackParams, newState);
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(callbackParams.getRequest().getCaseDetails().getData())
            .build();
    }

    private CallbackResponse evaluateReady(CallbackParams callbackParams,
                                           CaseState newState) {
        Map<String, Object> output = callbackParams.getRequest().getCaseDetails().getData();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .state(newState.toString())
            .data(output)
            .build();
    }
}
