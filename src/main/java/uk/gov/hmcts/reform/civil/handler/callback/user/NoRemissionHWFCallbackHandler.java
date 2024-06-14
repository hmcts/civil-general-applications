package uk.gov.hmcts.reform.civil.handler.callback.user;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NO_REMISSION_HWF_GA;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoRemissionHWFCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(NO_REMISSION_HWF_GA);
    private final ObjectMapper objectMapper;
    //private final HWFFeePaymentOutcomeService hwfFeePaymentOutcomeService;
    private final Map<String, Callback> callbackMap = Map.of(
        callbackKey(ABOUT_TO_START), this::setData,
        callbackKey(ABOUT_TO_SUBMIT),
        this::noRemissionHWF,
        callbackKey(SUBMITTED), this::emptySubmittedCallbackResponse
    );

    private CallbackResponse setData(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        if (Objects.isNull(caseData.getHwfFeeType())) {
            caseDataBuilder.hwfFeeType(FeeType.APPLICATION);
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
    }

    @Override
    protected Map<String, Callback> callbacks() {
        return callbackMap;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse noRemissionHWF(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
//        caseData = hwfFeePaymentOutcomeService.updateOutstandingFee(caseData, callbackParams.getRequest().getEventId());
//        CaseData.CaseDataBuilder<?, ?> updatedData = caseData.toBuilder()
//            .businessProcess(BusinessProcess.ready(NOTIFY_LIP_CLAIMANT_HWF_OUTCOME));
//
//        if (caseData.isHWFTypeHearing()) {
//            HelpWithFeesDetails hearingFeeDetails =
//                Optional.ofNullable(caseData.getHearingHwfDetails()).orElse(new HelpWithFeesDetails());
//            updatedData.hearingHwfDetails(hearingFeeDetails.toBuilder().hwfCaseEvent(NO_REMISSION_HWF).build());
//        }
//        if (caseData.isHWFTypeClaimIssued()) {
//            HelpWithFeesDetails claimIssuedHwfDetails =
//                Optional.ofNullable(caseData.getClaimIssuedHwfDetails()).orElse(new HelpWithFeesDetails());
//            updatedData.claimIssuedHwfDetails(claimIssuedHwfDetails.toBuilder().hwfCaseEvent(NO_REMISSION_HWF).build());
//        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .build();
    }
}
