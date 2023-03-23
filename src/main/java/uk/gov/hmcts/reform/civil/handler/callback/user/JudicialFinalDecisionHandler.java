package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.genapplication.FreeFormOrderValues;
import uk.gov.hmcts.reform.civil.service.docmosis.finalorder.FreeFormOrderGenerator;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GENERATE_DIRECTIONS_ORDER;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.dq.FinalOrderSelection.FREE_FORM_ORDER;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudicialFinalDecisionHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(GENERATE_DIRECTIONS_ORDER);
    private static final String POPULATE_FREE_FORM_ORDER_PREVIEW_DOC = "populate-freeForm-values";
    private static final String POPULATE_FINAL_ORDER_PREVIEW_DOC = "populate-final-order-preview-doc";
    private static final String ON_INITIATIVE_SELECTION_TEST = "As this order was made on the court's own initiative "
            + "any party affected by the order may apply to set aside, vary or stay the order."
            + " Any such application must be made by 4pm on";
    private static final String WITHOUT_NOTICE_SELECTION_TEXT = "If you were not notified of the application before "
            + "this order was made, you may apply to set aside, vary or stay the order."
            + " Any such application must be made by 4pm on";
    private final ObjectMapper objectMapper;
    private final FreeFormOrderGenerator gaFreeFormOrderGenerator;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
                callbackKey(ABOUT_TO_START), this::setCaseName,
                callbackKey(MID, POPULATE_FREE_FORM_ORDER_PREVIEW_DOC), this::populateFreeFormValues,
                callbackKey(MID, POPULATE_FINAL_ORDER_PREVIEW_DOC), this::gaPopulateFinalOrderPreviewDoc,
                callbackKey(ABOUT_TO_SUBMIT), this::setFinalDecisionBusinessProcess
        );
    }

    private CallbackResponse gaPopulateFinalOrderPreviewDoc(final CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        if (caseData.getFinalOrderSelection().equals(FREE_FORM_ORDER)) {
            CaseDocument freeform = gaFreeFormOrderGenerator.generate(
                    caseData,
                    callbackParams.getParams().get(BEARER_TOKEN).toString()
            );
            caseDataBuilder.gaFinalOrderDocPreview(freeform.getDocumentLink());
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
    }

    private CallbackResponse setCaseName(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder()
                .caseNameHmctsInternal(getAllPartyNames(caseData));
        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
    }

    public CallbackResponse populateFreeFormValues(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        caseDataBuilder.orderOnCourtInitiative(FreeFormOrderValues.builder()
                .onInitiativeSelectionTextArea(ON_INITIATIVE_SELECTION_TEST)
                .onInitiativeSelectionDate(LocalDate.now())
                .build());
        caseDataBuilder.orderWithoutNotice(FreeFormOrderValues.builder()
                .withoutNoticeSelectionTextArea(WITHOUT_NOTICE_SELECTION_TEXT)
                .withoutNoticeSelectionDate(LocalDate.now())
                .build());
        caseDataBuilder.freeFormOrderedText(caseData.getGeneralAppDetailsOfOrder());

        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
    }

    private CallbackResponse setFinalDecisionBusinessProcess(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        //caseDataBuilder.businessProcess(BusinessProcess.ready(MAKE_DECISION)).build();
        if (Objects.nonNull(caseData.getGaFinalOrderDocPreview())) {
            caseDataBuilder.gaFinalOrderDocPreview(null);
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    public static String getAllPartyNames(CaseData caseData) {
        return format("%s v %s%s",
                caseData.getClaimant1PartyName(),
                caseData.getDefendant1PartyName(),
                Objects.nonNull(caseData.getDefendant2PartyName())
                        && (NO.equals(caseData.getRespondent2SameLegalRepresentative())
                            || Objects.isNull(caseData.getRespondent2SameLegalRepresentative()))
                        ? ", " + caseData.getDefendant2PartyName() : "");
    }
}
