package uk.gov.hmcts.reform.civil.handler.callback.user;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_APPLICANT_LIP_HWF;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_HELP_WITH_FEE_NUMBER_GA;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesDetails;

@Service
@RequiredArgsConstructor
public class UpdatedRefNumberHWFCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(UPDATE_HELP_WITH_FEE_NUMBER_GA);
    private final ObjectMapper objectMapper;

    private final Map<String, Callback> callbackMap = Map.of(
        callbackKey(ABOUT_TO_START), this::emptyCallbackResponse,
        callbackKey(ABOUT_TO_SUBMIT),
        this::updatedRefNumberHWF,
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

    private CallbackResponse updatedRefNumberHWF(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData updatedCaseData = updateHwFReference(caseData);
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(updatedCaseData.toMap(objectMapper))
            .build();
    }

    private CaseData updateHwFReference(CaseData caseData) {
        CaseData.CaseDataBuilder updatedData = caseData.toBuilder()
                .businessProcess(BusinessProcess.ready(NOTIFY_APPLICANT_LIP_HWF));
        if (caseData.isHWFTypeApplication()) {
            String newRefNumber = getHwFNewReferenceNumber(caseData.getGaHwfDetails());
            ofNullable(caseData.getGeneralAppHelpWithFees())
                .ifPresent(hwf -> updatedData.generalAppHelpWithFees(hwf.toBuilder().helpWithFeesReferenceNumber(newRefNumber).build()));
            if (caseData.getGaHwfDetails() != null) {
                updatedData.gaHwfDetails(caseData.getGaHwfDetails().toBuilder()
                        .hwfCaseEvent(UPDATE_HELP_WITH_FEE_NUMBER_GA).hwfReferenceNumber(null).build());
            }
            return updatedData.build();
        }
        if (caseData.isHWFTypeAdditional()) {
            String newRefNumber = getHwFNewReferenceNumber(caseData.getAdditionalHwfDetails());
            ofNullable(caseData.getGeneralAppHelpWithFees())
                .ifPresent(hwf -> updatedData.generalAppHelpWithFees(hwf.toBuilder().helpWithFeesReferenceNumber(newRefNumber).build()));
            if (caseData.getAdditionalHwfDetails() != null) {
                updatedData.additionalHwfDetails(caseData.getAdditionalHwfDetails().toBuilder()
                        .hwfCaseEvent(UPDATE_HELP_WITH_FEE_NUMBER_GA).hwfReferenceNumber(null).build());
            }
            return updatedData.build();
        }
        return caseData;
    }

    private String getHwFNewReferenceNumber(HelpWithFeesDetails helpWithFeesDetails) {
        return ofNullable(helpWithFeesDetails)
            .map(HelpWithFeesDetails::getHwfReferenceNumber)
            .orElse(null);
    }
}
