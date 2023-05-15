package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApproveConsentOrder;
import uk.gov.hmcts.reform.civil.service.docmosis.consentorder.ConsentOrderGenerator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPROVE_CONSENT_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STAY_THE_CLAIM;

@Service
@RequiredArgsConstructor
public class ApproveConsentOrderCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(APPROVE_CONSENT_ORDER);
    public static final String ORDER_DATE_IN_PAST = "The date, by which the order to end"
        + " should be given, cannot be in past.";

    private final ConsentOrderGenerator consentOrderGenerator;
    private final ObjectMapper objectMapper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START), this::validateApplicationType,
            callbackKey(MID, "populate-consent-order-doc"), this::populateConsentOrder,
            callbackKey(SUBMITTED), this::buildConfirmation
        );
    }

    private CallbackResponse validateApplicationType(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        List<GeneralApplicationTypes> validGATypes = Arrays.asList(STAY_THE_CLAIM);
        GAApproveConsentOrder.GAApproveConsentOrderBuilder gaApproveConsentOrderBuilder = GAApproveConsentOrder.builder();
        if (caseData.getGeneralAppDetailsOfOrder() != null) {
            gaApproveConsentOrderBuilder.consentOrderDescription(caseData.getGeneralAppDetailsOfOrder()).build();
        }

        if (caseData.getGeneralAppType().getTypes().stream().anyMatch(validGATypes::contains)) {
            gaApproveConsentOrderBuilder.showConsentOrderDate(YesOrNo.YES).build();
        }
        List<String> errors = new ArrayList<>();
        caseDataBuilder.approveConsentOrder(gaApproveConsentOrderBuilder.build());
        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    private CallbackResponse populateConsentOrder(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        List<String> errors = new ArrayList<>();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        if (caseData.getApproveConsentOrder() != null
            && caseData.getApproveConsentOrder().getShowConsentOrderDate().equals(YesOrNo.YES)
            && LocalDate.now().isAfter(caseData.getApproveConsentOrder().getConsentOrderDateToEnd())) {
            errors.add(ORDER_DATE_IN_PAST);
        }

        CaseDocument consentOrderDocument = null;

        consentOrderDocument = consentOrderGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString());

        caseDataBuilder.consentOrderDocPreview(consentOrderDocument.getDocumentLink());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();

    }

    private CallbackResponse buildConfirmation(CallbackParams callbackParams) {

        String confirmationHeader = "# Your order has been made";
        String body = "<br/><br/>";
        return SubmittedCallbackResponse.builder()
            .confirmationHeader(confirmationHeader)
            .confirmationBody(body)
            .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }
}
