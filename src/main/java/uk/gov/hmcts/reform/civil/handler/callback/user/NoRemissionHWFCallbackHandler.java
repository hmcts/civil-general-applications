package uk.gov.hmcts.reform.civil.handler.callback.user;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NO_REMISSION_HWF_GA;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesDetails;
import uk.gov.hmcts.reform.civil.utils.HwFFeeTypeService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class NoRemissionHWFCallbackHandler extends HWFCallbackHandlerBase {

    private static final List<CaseEvent> EVENTS = List.of(NO_REMISSION_HWF_GA);
    private final Map<String, Callback> callbackMap = Map.of(
        callbackKey(ABOUT_TO_START), this::setData,
        callbackKey(ABOUT_TO_SUBMIT),
        this::noRemissionHWF,
        callbackKey(SUBMITTED), this::emptySubmittedCallbackResponse
    );

    public NoRemissionHWFCallbackHandler(ObjectMapper objectMapper) {
        super(objectMapper, EVENTS);
    }

    @Override
    protected Map<String, Callback> callbacks() {
        return callbackMap;
    }

    private CallbackResponse noRemissionHWF(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        caseData = HwFFeeTypeService.updateOutstandingFee(caseData, callbackParams.getRequest().getEventId());
        CaseData.CaseDataBuilder updatedData = caseData.toBuilder();

        if (caseData.getHwfFeeType().equals(FeeType.ADDITIONAL)) {
            HelpWithFeesDetails additionalFeeDetails =
                    Optional.ofNullable(caseData.getAdditionalHwfDetails()).orElse(new HelpWithFeesDetails());
            updatedData.additionalHwfDetails(additionalFeeDetails.toBuilder().hwfCaseEvent(NO_REMISSION_HWF_GA).build());
        }
        if (caseData.getHwfFeeType().equals(FeeType.APPLICATION)) {
            HelpWithFeesDetails gaHwfDetails =
                    Optional.ofNullable(caseData.getGaHwfDetails()).orElse(new HelpWithFeesDetails());
            updatedData.gaHwfDetails(gaHwfDetails.toBuilder().hwfCaseEvent(NO_REMISSION_HWF_GA).build());

        }
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .build();
    }
}
