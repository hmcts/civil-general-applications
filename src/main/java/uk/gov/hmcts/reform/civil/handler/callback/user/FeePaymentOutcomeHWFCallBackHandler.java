package uk.gov.hmcts.reform.civil.handler.callback.user;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.PaymentRequestUpdateCallbackService;
import uk.gov.hmcts.reform.civil.utils.HwFFeeTypeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FeePaymentOutcomeHWFCallBackHandler extends HWFCallbackHandlerBase {

    public static final String WRONG_REMISSION_TYPE_SELECTED = "Incorrect remission type selected";
    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.FEE_PAYMENT_OUTCOME_GA);

    public FeePaymentOutcomeHWFCallBackHandler (ObjectMapper objectMapper,
                                                PaymentRequestUpdateCallbackService paymentRequestUpdateCallbackService) {
        super(objectMapper, EVENTS, paymentRequestUpdateCallbackService);
    }

    @Override
    protected Map<String, Callback> callbacks() {
        return new ImmutableMap.Builder<String, Callback>()
            .put(callbackKey(ABOUT_TO_START), this::setData)
            .put(callbackKey(ABOUT_TO_SUBMIT), this::submitFeePaymentOutcome)
            .put(callbackKey(MID, "remission-type"), this::validateSelectedRemissionType)
            .put(callbackKey(SUBMITTED), this::emptySubmittedCallbackResponse)
            .build();
    }

    private CallbackResponse validateSelectedRemissionType(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        var errors = new ArrayList<String>();

        if ((caseData.isHWFTypeApplication()
            && caseData.getFeePaymentOutcomeDetails().getHwfFullRemissionGrantedForGa() == YesOrNo.YES
            && Objects.nonNull(caseData.getGaHwfDetails())
            && Objects.nonNull(caseData.getGaHwfDetails().getOutstandingFeeInPounds())
            && !Objects.equals(caseData.getGaHwfDetails().getOutstandingFeeInPounds(), BigDecimal.ZERO))
            || (caseData.isHWFTypeAdditional()
            && caseData.getFeePaymentOutcomeDetails().getHwfFullRemissionGrantedForAdditional() == YesOrNo.YES
            && Objects.nonNull(caseData.getAdditionalHwfDetails())
            && Objects.nonNull(caseData.getAdditionalHwfDetails().getOutstandingFeeInPounds())
            && !Objects.equals(caseData.getAdditionalHwfDetails().getOutstandingFeeInPounds(), BigDecimal.ZERO))) {
            errors.add(WRONG_REMISSION_TYPE_SELECTED);
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    private CallbackResponse submitFeePaymentOutcome(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        if (caseData.isHWFTypeApplication()) {
            LocalDate issueDate = LocalDate.now();
            caseDataBuilder.issueDate(issueDate).build();
        }
        caseData = caseDataBuilder.build();
        caseData = HwFFeeTypeService.updateHwfReferenceNumber(caseData);

        //paymentRequestUpdateCallbackService.processHWF(caseData);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .build();
    }

}
