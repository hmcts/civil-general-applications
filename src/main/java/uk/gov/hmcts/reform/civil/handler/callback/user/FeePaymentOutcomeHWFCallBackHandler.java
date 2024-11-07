package uk.gov.hmcts.reform.civil.handler.callback.user;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_COSC_APPLICATION_AFTER_PAYMENT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_GA_ADD_HWF;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.HwfNotificationService;
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
    public static final String CASE_STATE_INVALID = "Case is in invalid state";
    public static final String PROCESS_FEE_PAYMENT_FAILED = "Process fee payment failed";
    private static final List<CaseEvent> EVENTS = List.of(CaseEvent.FEE_PAYMENT_OUTCOME_GA);

    public FeePaymentOutcomeHWFCallBackHandler(ObjectMapper objectMapper,
                                               PaymentRequestUpdateCallbackService paymentRequestUpdateCallbackService,
                                               HwfNotificationService hwfNotificationService, FeatureToggleService featureToggleService) {
        super(objectMapper, EVENTS, paymentRequestUpdateCallbackService, hwfNotificationService, featureToggleService);
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
            && caseData.getFeePaymentOutcomeDetails().getHwfFullRemissionGrantedForAdditionalFee() == YesOrNo.YES
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

        List<String> errors = new ArrayList<>();

        assert paymentRequestUpdateCallbackService != null;
        CaseData processedCaseData = paymentRequestUpdateCallbackService.processHwf(caseData);
        assert hwfNotificationService != null;
        hwfNotificationService.sendNotification(processedCaseData, CaseEvent.FEE_PAYMENT_OUTCOME_GA);
        if (Objects.isNull(processedCaseData)) {
            errors.add(PROCESS_FEE_PAYMENT_FAILED);
        } else {
            if (processedCaseData.isHWFTypeApplication()) {

                CaseEvent caseEvent = INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT;
                assert featureToggleService != null;
                if (featureToggleService.isCoSCEnabled() && caseData.getGeneralAppType().getTypes().contains(
                    GeneralApplicationTypes.CONFIRM_CCJ_DEBT_PAID)) {
                    caseEvent = INITIATE_COSC_APPLICATION_AFTER_PAYMENT;
                }
                caseData = processedCaseData.toBuilder()
                    .gaHwfDetails(caseData.getGaHwfDetails().toBuilder()
                                      .fee(caseData.getGeneralAppPBADetails().getFee())
                                      .hwfReferenceNumber(caseData
                                                              .getGeneralAppHelpWithFees()
                                                              .getHelpWithFeesReferenceNumber()).build())
                    .businessProcess(BusinessProcess.ready(caseEvent)).build();
            } else if (processedCaseData.isHWFTypeAdditional()) {
                caseData = processedCaseData.toBuilder()
                    .additionalHwfDetails(caseData.getAdditionalHwfDetails().toBuilder()
                                              .fee(caseData.getGeneralAppPBADetails().getFee())
                                              .hwfReferenceNumber(caseData
                                                                      .getGeneralAppHelpWithFees()
                                                                      .getHelpWithFeesReferenceNumber()).build())
                    .businessProcess(BusinessProcess.ready(UPDATE_GA_ADD_HWF))
                    .build();
            } else {
                errors.add(CASE_STATE_INVALID);
            }
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .errors(errors)
            .build();
    }

}
