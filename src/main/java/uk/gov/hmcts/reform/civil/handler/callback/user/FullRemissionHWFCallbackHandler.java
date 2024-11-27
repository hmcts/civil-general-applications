package uk.gov.hmcts.reform.civil.handler.callback.user;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.FULL_REMISSION_HWF_GA;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesDetails;
import uk.gov.hmcts.reform.civil.utils.HwFFeeTypeService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FullRemissionHWFCallbackHandler extends HWFCallbackHandlerBase {

    private static final List<CaseEvent> EVENTS = List.of(FULL_REMISSION_HWF_GA);
    private final Map<String, Callback> callbackMap = Map.of(
            callbackKey(ABOUT_TO_START), this::setData,
            callbackKey(ABOUT_TO_SUBMIT),
            this::fullRemissionHWF,
            callbackKey(SUBMITTED), this::emptySubmittedCallbackResponse
    );

    public FullRemissionHWFCallbackHandler(ObjectMapper objectMapper) {
        super(objectMapper, EVENTS);
    }

    @Override
    protected Map<String, Callback> callbacks() {
        return callbackMap;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse fullRemissionHWF(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder updatedData = caseData.toBuilder();
        BigDecimal feeAmount = HwFFeeTypeService.getCalculatedFeeInPence(caseData);

        if (caseData.getHwfFeeType().equals(FeeType.APPLICATION)
                && feeAmount.compareTo(BigDecimal.ZERO) != 0) {
            log.info("HWF fee type is application for caseId: {}", callbackParams.getCaseData().getCcdCaseReference());
            Optional.ofNullable(caseData.getGaHwfDetails())
                .ifPresentOrElse(
                    gaHwfDetails -> updatedData.gaHwfDetails(
                        gaHwfDetails.toBuilder().remissionAmount(feeAmount)
                            .outstandingFeeInPounds(BigDecimal.ZERO)
                            .hwfFeeType(FeeType.APPLICATION)
                            .hwfCaseEvent(FULL_REMISSION_HWF_GA)
                            .build()
                    ),
                    () -> updatedData.gaHwfDetails(
                        HelpWithFeesDetails.builder().remissionAmount(feeAmount)
                            .outstandingFeeInPounds(BigDecimal.ZERO)
                            .hwfFeeType(FeeType.APPLICATION)
                            .hwfCaseEvent(FULL_REMISSION_HWF_GA)
                            .build()
                    )
                );
        } else if (caseData.getHwfFeeType().equals(FeeType.ADDITIONAL)
                && feeAmount.compareTo(BigDecimal.ZERO) != 0) {
            log.info("HWF fee type is additional for caseId: {}", callbackParams.getCaseData().getCcdCaseReference());
            Optional.ofNullable(caseData.getAdditionalHwfDetails())
                .ifPresentOrElse(
                    hearingHwfDetails -> updatedData.additionalHwfDetails(
                        HelpWithFeesDetails.builder().remissionAmount(feeAmount)
                            .outstandingFeeInPounds(BigDecimal.ZERO)
                            .hwfCaseEvent(FULL_REMISSION_HWF_GA)
                            .hwfFeeType(FeeType.ADDITIONAL)
                            .build()
                    ),
                    () -> updatedData.additionalHwfDetails(
                        HelpWithFeesDetails.builder().remissionAmount(feeAmount)
                            .hwfCaseEvent(FULL_REMISSION_HWF_GA)
                            .hwfFeeType(FeeType.ADDITIONAL)
                            .build()
                    )
                );
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(updatedData.build().toMap(objectMapper))
                .build();
    }
}
