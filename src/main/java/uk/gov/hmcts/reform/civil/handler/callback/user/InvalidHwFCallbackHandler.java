package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INVALID_HWF_REFERENCE_GA;

@Service
@Slf4j
public class InvalidHwFCallbackHandler extends HWFCallbackHandlerBase {

    private static final List<CaseEvent> EVENTS = List.of(INVALID_HWF_REFERENCE_GA);

    private final Map<String, Callback> callbackMap = Map.of(
        callbackKey(ABOUT_TO_START), this::setData,
        callbackKey(ABOUT_TO_SUBMIT), this::aboutToSubmit,
        callbackKey(SUBMITTED), this::emptySubmittedCallbackResponse
    );

    public InvalidHwFCallbackHandler(ObjectMapper objectMapper) {
        super(objectMapper, EVENTS);
    }

    @Override
    protected Map<String, Callback> callbacks() {
        return callbackMap;
    }

    private CallbackResponse aboutToSubmit(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData updatedCaseData = setUpBusinessProcess(caseData);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(updatedCaseData.toMap(objectMapper))
            .build();
    }

    private CaseData setUpBusinessProcess(CaseData caseData) {
        CaseData.CaseDataBuilder updatedData = caseData.toBuilder();

        if (caseData.isHWFTypeAdditional()) {
            HelpWithFeesDetails hearingFeeDetails =
                    Optional.ofNullable(caseData.getAdditionalHwfDetails()).orElse(new HelpWithFeesDetails());
            updatedData.additionalHwfDetails(hearingFeeDetails.toBuilder().hwfCaseEvent(INVALID_HWF_REFERENCE_GA).build());
        }
        if (caseData.isHWFTypeApplication()) {
            HelpWithFeesDetails claimIssuedHwfDetails =
                    Optional.ofNullable(caseData.getGaHwfDetails()).orElse(new HelpWithFeesDetails());
            updatedData.gaHwfDetails(claimIssuedHwfDetails.toBuilder().hwfCaseEvent(INVALID_HWF_REFERENCE_GA).build());
        }
        return updatedData.build();
    }
}
