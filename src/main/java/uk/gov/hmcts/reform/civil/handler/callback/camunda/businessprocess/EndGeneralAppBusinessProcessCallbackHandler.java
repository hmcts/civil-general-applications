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
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.GaForLipService;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_BUSINESS_PROCESS_GASPEC;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_ADDITIONAL_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_APPLICATION_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_DIRECTIONS_ORDER_DOCS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.LISTING_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PENDING_APPLICATION_ISSUED;
import static uk.gov.hmcts.reform.civil.enums.dq.FinalOrderSelection.ASSISTED_ORDER;
import static uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil.isNotificationCriteriaSatisfied;
import static uk.gov.hmcts.reform.civil.utils.RespondentsResponsesUtil.isRespondentsResponseSatisfied;

@Service
@RequiredArgsConstructor
public class EndGeneralAppBusinessProcessCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(END_BUSINESS_PROCESS_GASPEC);

    private final CaseDetailsConverter caseDetailsConverter;
    private final GaForLipService gaForLipService;
    private final ParentCaseUpdateHelper parentCaseUpdateHelper;
    private static final String FREE_KEYWORD = "FREE";

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

        if (!gaForLipService.isGaForLip(data)
            && (data.getCcdState().equals(AWAITING_APPLICATION_PAYMENT) || isFreeFeeCode(data))) {

            parentCaseUpdateHelper.updateJudgeAndRespondentCollectionAfterPayment(data);
        }

        /*
        * GA for LIP
        * When payment is done via Service Request for GA then,
        * Add GA into collections
        * */
        if (gaForLipService.isGaForLip(data)
            && (isLipPaymentViaServiceRequest(data) || isFreeFeeCode(data))) {

            parentCaseUpdateHelper.updateJudgeAndRespondentCollectionAfterPayment(data);
        }

        CaseState newState;
        if (data.getGeneralAppPBADetails().getPaymentDetails() == null) {
            newState = AWAITING_APPLICATION_PAYMENT;

            /*
             * GA for LIP
             * When Caseworker should have access to GA to perform HelpWithFee then,
             * Add GA into collections
             * */
            if (gaForLipService.isGaForLip(data) && Objects.nonNull(data.getGeneralAppHelpWithFees())
                && data.getGeneralAppHelpWithFees().getHelpWithFee().equals(YesOrNo.YES)) {

                parentCaseUpdateHelper.updateMasterCollectionForHwf(data);
            }

        } else if (Objects.nonNull(data.getFinalOrderSelection())) {
            if (data.getFinalOrderSelection().equals(ASSISTED_ORDER)
                && Objects.nonNull(data.getAssistedOrderFurtherHearingDetails())) {
                newState = LISTING_FOR_A_HEARING;
            } else {
                newState = ORDER_MADE;
            }
        } else {
            newState = (isNotificationCriteriaSatisfied(data) && !isRespondentsResponseSatisfied(
                data,
                data.toBuilder().build()
            ))
                ? AWAITING_RESPONDENT_RESPONSE
                : APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
        }

        if (!(data.getCcdState().equals(AWAITING_DIRECTIONS_ORDER_DOCS)
            || data.getCcdState().equals(AWAITING_ADDITIONAL_INFORMATION)
            || data.getCcdState().equals(AWAITING_WRITTEN_REPRESENTATIONS))) {
            parentCaseUpdateHelper.updateParentWithGAState(data, newState.getDisplayedValue());
        } else {
            newState = data.getCcdState();
        }
        return evaluateReady(callbackParams, newState);
    }

    private CallbackResponse evaluateReady(CallbackParams callbackParams,
                                           CaseState newState) {
        Map<String, Object> output = callbackParams.getRequest().getCaseDetails().getData();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .state(newState.toString())
            .data(output)
            .build();
    }

    private boolean isFreeFeeCode(CaseData data) {
        return (data.getCcdState().equals(PENDING_APPLICATION_ISSUED)
            && Objects.nonNull(data.getGeneralAppPBADetails())
            && Objects.nonNull(data.getGeneralAppPBADetails().getFee())
            && (FREE_KEYWORD.equalsIgnoreCase(data.getGeneralAppPBADetails().getFee().getCode())));
    }

    private boolean isLipPaymentViaServiceRequest(CaseData data) {
        return data.getCcdState().equals(AWAITING_APPLICATION_PAYMENT)
            && (Objects.isNull(data.getGeneralAppHelpWithFees())
            || data.getGeneralAppHelpWithFees().getHelpWithFee() == YesOrNo.NO);
    }
}
