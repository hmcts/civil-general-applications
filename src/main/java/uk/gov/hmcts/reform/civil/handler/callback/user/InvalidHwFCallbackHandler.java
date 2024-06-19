package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.utils.HwFFeeTypeService;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INVALID_HWF_REFERENCE_GA;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_ADD_PAYMENT;

@Service
@RequiredArgsConstructor
public class InvalidHwFCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(INVALID_HWF_REFERENCE_GA);
    private final ObjectMapper objectMapper;

    private final Map<String, Callback> callbackMap = Map.of(
        callbackKey(ABOUT_TO_START), this::setData,
        callbackKey(ABOUT_TO_SUBMIT), this::aboutToSubmit,
        callbackKey(SUBMITTED), this::emptySubmittedCallbackResponse);

    @Override
    protected Map<String, Callback> callbacks() {
        return callbackMap;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse aboutToSubmit(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
       // CaseData updatedCaseData = setUpBusinessProcess(caseData);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .build();
    }

    private CallbackResponse setData(CallbackParams callbackParams) {
        CaseData.CaseDataBuilder  caseDataBuilder= HwFFeeTypeService.updateFeeType(callbackParams.getCaseData());
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

//    static CaseData.CaseDataBuilder updateFeeType( CaseData caseData) {
//        //CaseData caseData = callbackParams.getCaseData();
//        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
//        if (Objects.nonNull(caseData.getGeneralAppHelpWithFees())
//            && Objects.isNull(caseData.getHwfFeeType())) {
//            if (caseData.getCcdState().equals(APPLICATION_ADD_PAYMENT)) {
//                caseDataBuilder.hwfFeeType(FeeType.ADDITIONAL);
//            } else {
//                caseDataBuilder.hwfFeeType(FeeType.APPLICATION);
//            }
//        }
//        return  caseDataBuilder;
//    }
}
