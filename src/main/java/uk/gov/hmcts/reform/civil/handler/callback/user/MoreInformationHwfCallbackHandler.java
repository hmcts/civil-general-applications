package uk.gov.hmcts.reform.civil.handler.callback.user;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MORE_INFORMATION_HWF_GA;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesMoreInformation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MoreInformationHwfCallbackHandler extends HWFCallbackHandlerBase {

    private static final String ERROR_MESSAGE_DOCUMENT_DATE_MUST_BE_AFTER_TODAY = "Documents date must be future date";

    private final Map<String, Callback> callbackMap = Map.of(
        callbackKey(ABOUT_TO_START), this::setData,
        callbackKey(MID, "more-information-hwf"), this::validationMoreInformation,
        callbackKey(ABOUT_TO_SUBMIT), this::submitMoreInformationHwf,
        callbackKey(SUBMITTED), this::emptySubmittedCallbackResponse
    );

    public MoreInformationHwfCallbackHandler(ObjectMapper objectMapper) {
        super(objectMapper, Collections.singletonList(
                MORE_INFORMATION_HWF_GA
        ));
    }

    @Override
    protected Map<String, Callback> callbacks() {
        return callbackMap;
    }

    private CallbackResponse validationMoreInformation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        List<String> errors = new ArrayList<>();
        HelpWithFeesMoreInformation moreInformationData =
            FeeType.ADDITIONAL == caseData.getHwfFeeType()
                ? caseData.getHelpWithFeesMoreInformationAdditional()
                : caseData.getHelpWithFeesMoreInformationGa();
        LocalDate hwFMoreInfoDocumentDate = moreInformationData.getHwFMoreInfoDocumentDate();
        if (!hwFMoreInfoDocumentDate.isAfter(LocalDate.now())) {
            errors.add(ERROR_MESSAGE_DOCUMENT_DATE_MUST_BE_AFTER_TODAY);
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    private CallbackResponse submitMoreInformationHwf(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder updatedData = caseData.toBuilder();

        if (caseData.getHwfFeeType().equals(FeeType.ADDITIONAL)) {
            HelpWithFeesDetails additionalFeeDetails =
                Optional.ofNullable(caseData.getAdditionalHwfDetails()).orElse(new HelpWithFeesDetails());
            updatedData.additionalHwfDetails(additionalFeeDetails.toBuilder().hwfCaseEvent(MORE_INFORMATION_HWF_GA).build());
        }
        if (caseData.getHwfFeeType().equals(FeeType.APPLICATION)) {
            HelpWithFeesDetails gaHwfDetails =
                Optional.ofNullable(caseData.getGaHwfDetails()).orElse(new HelpWithFeesDetails());
            updatedData.gaHwfDetails(gaHwfDetails.toBuilder().hwfCaseEvent(MORE_INFORMATION_HWF_GA).build());

        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(updatedData.build().toMap(objectMapper))
            .build();
    }
}
