package uk.gov.hmcts.reform.civil.handler.callback.user;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.FULL_REMISSION_HWF_GA;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.utils.HwFFeeTypeService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FullRemissionHWFCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(FULL_REMISSION_HWF_GA);
    private final ObjectMapper objectMapper;
    private final Map<String, Callback> callbackMap = Map.of(
            callbackKey(ABOUT_TO_START), this::setData,
            callbackKey(ABOUT_TO_SUBMIT),
            this::fullRemissionHWF,
            callbackKey(SUBMITTED), this::emptySubmittedCallbackResponse
    );

    @Override
    protected Map<String, Callback> callbacks() {
        return callbackMap;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse setData(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = HwFFeeTypeService.updateFeeType(caseData);
        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
    }

    private CallbackResponse fullRemissionHWF(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
//        CaseData.CaseDataBuilder<?, ?> updatedData = caseData.toBuilder()
//            .businessProcess(BusinessProcess.ready(NOTIFY_LIP_CLAIMANT_HWF_OUTCOME));
//        BigDecimal claimFeeAmount = caseData.getCalculatedGaFeeInPence();
//        BigDecimal hearingFeeAmount = caseData.getCalculatedAdditionalFeeInPence();
//
//        if (caseData.isHWFTypeClaimIssued() && claimFeeAmount.compareTo(BigDecimal.ZERO) != 0) {
//            Optional.ofNullable(caseData.getClaimIssuedHwfDetails())
//                .ifPresentOrElse(
//                    claimIssuedHwfDetails -> updatedData.claimIssuedHwfDetails(
//                        claimIssuedHwfDetails.toBuilder().remissionAmount(claimFeeAmount)
//                            .outstandingFeeInPounds(BigDecimal.ZERO)
//                            .hwfCaseEvent(FULL_REMISSION_HWF)
//                            .build()
//                    ),
//                    () -> updatedData.claimIssuedHwfDetails(
//                        HelpWithFeesDetails.builder().remissionAmount(claimFeeAmount)
//                            .outstandingFeeInPounds(BigDecimal.ZERO)
//                            .hwfCaseEvent(FULL_REMISSION_HWF)
//                            .build()
//                    )
//                );
//        } else if (caseData.isHWFTypeHearing() && hearingFeeAmount.compareTo(BigDecimal.ZERO) != 0) {
//            Optional.ofNullable(caseData.getHearingHwfDetails())
//                .ifPresentOrElse(
//                    hearingHwfDetails -> updatedData.hearingHwfDetails(
//                        HelpWithFeesDetails.builder().remissionAmount(hearingFeeAmount)
//                            .outstandingFeeInPounds(BigDecimal.ZERO)
//                            .hwfCaseEvent(FULL_REMISSION_HWF)
//                            .build()
//                    ),
//                    () -> updatedData.hearingHwfDetails(
//                        HelpWithFeesDetails.builder().remissionAmount(hearingFeeAmount)
//                            .hwfCaseEvent(FULL_REMISSION_HWF)
//                            .build()
//                    )
//                );
//        }
//        helpWithFeesForTabService.setUpHelpWithFeeTab(updatedData);

        return AboutToStartOrSubmitCallbackResponse.builder()
                //.data(updatedData.build().toMap(objectMapper))
                .build();
    }
}
