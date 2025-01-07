package uk.gov.hmcts.reform.civil.handler.callback.user;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_APPLICANT_LIP_HWF;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.PARTIAL_REMISSION_HWF_GA;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.utils.HwFFeeTypeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PartialRemissionHWFCallbackHandler extends HWFCallbackHandlerBase {

    private static final List<CaseEvent> EVENTS = List.of(PARTIAL_REMISSION_HWF_GA);
    public static final String ERR_MSG_REMISSION_AMOUNT_LESS_THAN_GA_FEE = "Remission amount must be less than application fee";
    public static final String ERR_MSG_REMISSION_AMOUNT_LESS_THAN_ADDITIONAL_FEE = "Remission amount must be less than additional application fee";
    public static final String ERR_MSG_REMISSION_AMOUNT_LESS_THAN_ZERO = "Remission amount must be greater than zero";

    private final Map<String, Callback> callbackMap = Map.of(
        callbackKey(ABOUT_TO_START), this::emptyCallbackResponse,
        callbackKey(MID, "remission-amount"), this::validateRemissionAmount,
        callbackKey(ABOUT_TO_SUBMIT),
        this::partRemissionHWF,
        callbackKey(SUBMITTED), this::emptySubmittedCallbackResponse
    );

    public PartialRemissionHWFCallbackHandler(ObjectMapper objectMapper) {
        super(objectMapper, EVENTS);
    }

    @Override
    protected Map<String, Callback> callbacks() {
        return callbackMap;
    }

    private CallbackResponse validateRemissionAmount(CallbackParams callbackParams) {
        var caseData = callbackParams.getCaseData();
        var gaRemissionAmount = HwFFeeTypeService.getGaRemissionAmount(caseData);
        var additionalRemissionAmount = HwFFeeTypeService.getAdditionalRemissionAmount(caseData);
        var feeAmount = HwFFeeTypeService.getCalculatedFeeInPence(caseData);
        var errors = new ArrayList<String>();

        if (gaRemissionAmount.signum() == -1 || additionalRemissionAmount.signum() == -1) {
            errors.add(ERR_MSG_REMISSION_AMOUNT_LESS_THAN_ZERO);
        } else if (caseData.getHwfFeeType().equals(FeeType.APPLICATION)
                && gaRemissionAmount.compareTo(feeAmount) >= 0) {
            errors.add(ERR_MSG_REMISSION_AMOUNT_LESS_THAN_GA_FEE);
        } else if (caseData.getHwfFeeType().equals(FeeType.ADDITIONAL)
                && additionalRemissionAmount.compareTo(feeAmount) >= 0) {
            errors.add(ERR_MSG_REMISSION_AMOUNT_LESS_THAN_ADDITIONAL_FEE);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    private CallbackResponse partRemissionHWF(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        caseData = HwFFeeTypeService.updateOutstandingFee(caseData, callbackParams.getRequest().getEventId());
        CaseData.CaseDataBuilder updatedData = caseData.toBuilder()
                .businessProcess(BusinessProcess.ready(NOTIFY_APPLICANT_LIP_HWF));
        HwFFeeTypeService.updateEventInHwfDetails(caseData, updatedData, PARTIAL_REMISSION_HWF_GA);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(updatedData.build().toMap(objectMapper))
            .build();
    }
}
