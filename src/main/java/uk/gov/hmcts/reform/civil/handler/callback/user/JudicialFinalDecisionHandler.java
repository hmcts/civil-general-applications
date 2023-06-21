package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.genapplication.FreeFormOrderValues;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.AssistedOrderCost;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.AssistedOrderFurtherHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.AssistedOrderMadeDateHeardDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.DetailTextWithDate;
import uk.gov.hmcts.reform.civil.service.GeneralAppLocationRefDataService;
import uk.gov.hmcts.reform.civil.service.docmosis.finalorder.AssistedOrderFormGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.finalorder.FreeFormOrderGenerator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GENERATE_DIRECTIONS_ORDER;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.dq.FinalOrderSelection.ASSISTED_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.FinalOrderSelection.FREE_FORM_ORDER;
import static uk.gov.hmcts.reform.civil.model.common.DynamicList.fromList;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudicialFinalDecisionHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(GENERATE_DIRECTIONS_ORDER);
    public static final String DATE_HEARD_VALIDATION = "The date entered cannot be in the future";
    private final GeneralAppLocationRefDataService locationRefDataService;

    private static final String ON_INITIATIVE_SELECTION_TEST = "As this order was made on the court's own initiative "
            + "any party affected by the order may apply to set aside, vary or stay the order."
            + " Any such application must be made by 4pm on";
    private static final String WITHOUT_NOTICE_SELECTION_TEXT = "If you were not notified of the application before "
            + "this order was made, you may apply to set aside, vary or stay the order."
            + " Any such application must be made by 4pm on";
    private static final String ORDER_ISSUED = "# Your order has been issued %n%n ## Case number %n%n # %s";
    private static final String ORDER_1_CLAI = "<br/><p>The order has been sent to: </p>"
            + "%n%n ## Claimant 1 %n%n %s";
    private static final String ORDER_1_DEF = "%n%n ## Defendant 1 %n%n %s";
    private static final String ORDER_2_DEF = "%n%n ## Defendant 2 %n%n %s";
    private static final String POPULATE_FINAL_ORDER_FORM_VALUES = "populate-finalOrder-form-values";
    private static final String POPULATE_FINAL_ORDER_PREVIEW_DOC = "populate-final-order-preview-doc";
    private final ObjectMapper objectMapper;
    private final FreeFormOrderGenerator gaFreeFormOrderGenerator;
    private final AssistedOrderFormGenerator assistedOrderFormGenerator;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
                callbackKey(ABOUT_TO_START), this::setCaseName,
                callbackKey(MID, POPULATE_FINAL_ORDER_FORM_VALUES), this::populateFreeFormValues,
                callbackKey(MID, POPULATE_FINAL_ORDER_PREVIEW_DOC), this::gaPopulateFinalOrderPreviewDoc,
                callbackKey(ABOUT_TO_SUBMIT), this::setFinalDecisionBusinessProcess,
                callbackKey(SUBMITTED), this::buildConfirmation
        );
    }

    private CallbackResponse buildConfirmation(CallbackParams callbackParams) {
        var caseData = callbackParams.getCaseData();
        return SubmittedCallbackResponse.builder()
                .confirmationHeader(getHeader(caseData))
                .confirmationBody(getBody(caseData))
                .build();
    }

    private String getBody(final CaseData caseData) {
        if (Objects.nonNull(caseData.getDefendant2PartyName())
                && (NO.equals(caseData.getRespondent2SameLegalRepresentative())
                || Objects.isNull(caseData.getRespondent2SameLegalRepresentative()))) {
            return format(ORDER_1_CLAI, caseData.getClaimant1PartyName())
                    + format(ORDER_1_DEF, caseData.getDefendant1PartyName())
                    + format(ORDER_2_DEF, caseData.getDefendant2PartyName());

        } else {
            return format(ORDER_1_CLAI, caseData.getClaimant1PartyName())
                    + format(ORDER_1_DEF, caseData.getDefendant1PartyName());
        }
    }

    private String getHeader(CaseData caseData) {
        return format(ORDER_ISSUED, getCaseNumberFormatted(caseData));
    }

    private CallbackResponse gaPopulateFinalOrderPreviewDoc(final CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        List<String> errors = validAssistedOrderForm(caseData);
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        if (caseData.getFinalOrderSelection().equals(FREE_FORM_ORDER)) {
            CaseDocument freeform = gaFreeFormOrderGenerator.generate(
                    caseData,
                    callbackParams.getParams().get(BEARER_TOKEN).toString()
            );
            caseDataBuilder.gaFinalOrderDocPreview(freeform.getDocumentLink());
        } else if (caseData.getFinalOrderSelection().equals(ASSISTED_ORDER)) {
            CaseDocument assistedOrder = assistedOrderFormGenerator.generate(
                caseData,
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );
            caseDataBuilder.gaFinalOrderDocPreview(assistedOrder.getDocumentLink());
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .errors(errors)
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

        caseDataBuilder.orderMadeOnOwnInitiative(DetailTextWithDate.builder().detailText(ON_INITIATIVE_SELECTION_TEST)
                                                     .date(LocalDate.now()).build());
        caseDataBuilder.orderMadeOnWithOutNotice(DetailTextWithDate.builder().detailText(WITHOUT_NOTICE_SELECTION_TEXT)
                                                     .date(LocalDate.now()).build());

        caseDataBuilder.assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder()
                                                              .date(LocalDate.now()).build()).build();

        LocalDate localDatePlus14days = LocalDate.now().plusDays(14);
        caseDataBuilder.claimantCostStandardBase(AssistedOrderCost.builder()
                                                     .costPaymentDeadLine(localDatePlus14days)
                                                     .build());
        caseDataBuilder.claimantCostSummarilyBase(AssistedOrderCost.builder()
                                                      .costPaymentDeadLine(localDatePlus14days)
                                                      .build());
        caseDataBuilder.defendantCostStandardBase(AssistedOrderCost.builder()
                                                      .costPaymentDeadLine(localDatePlus14days)
                                                      .build());
        caseDataBuilder.defendantCostSummarilyBase(AssistedOrderCost.builder()
                                                       .costPaymentDeadLine(localDatePlus14days)
                                                       .build());

        String authToken = callbackParams.getParams().get(BEARER_TOKEN).toString();
        DynamicList dynamicLocationList = getLocationsFromList(locationRefDataService.getCourtLocations(authToken));
        caseDataBuilder.assistedOrderFurtherHearingDetails(AssistedOrderFurtherHearingDetails
                                                               .builder()
                                                               .alternativeHearingLocation(dynamicLocationList)
                                                               .build());
        caseDataBuilder.assistedOrderOrderedThatText(caseData.getGeneralAppDetailsOfOrder()).build();

        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
    }

    private CallbackResponse setFinalDecisionBusinessProcess(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.businessProcess(BusinessProcess.ready(GENERATE_DIRECTIONS_ORDER)).build();
        if (Objects.nonNull(caseData.getGaFinalOrderDocPreview())) {
            caseDataBuilder.gaFinalOrderDocPreview(null);
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
    }

    private List<String> validAssistedOrderForm(CaseData caseData) {
        List<String> errors = new ArrayList<>();
        if (caseData.getAssistedOrderMadeSelection() != null
            && caseData.getAssistedOrderMadeSelection().equals(YesOrNo.YES)
            && caseData.getAssistedOrderMadeDateHeardDetails().getDate().isAfter(LocalDate.now())) {
            errors.add(DATE_HEARD_VALIDATION);
        }

        return errors;
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

    private String getCaseNumberFormatted(CaseData caseData) {
        String[] parts = caseData.getCcdCaseReference().toString().split("(?<=\\G.{4})");
        return String.join("-", parts);
    }

    private DynamicList getLocationsFromList(final List<LocationRefData> locations) {
        return fromList(locations.stream().map(location -> new StringBuilder().append(location.getSiteName())
                .append(" - ").append(location.getCourtAddress())
                .append(" - ").append(location.getPostcode()).toString())
                            .toList());
    }
}
