package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.FinalOrderSelection;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.AssistedOrderDateHeard;
import uk.gov.hmcts.reform.civil.model.genapplication.finalorder.AssistedOrderMadeDateHeardDetails;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.GeneralAppLocationRefDataService;
import uk.gov.hmcts.reform.civil.service.docmosis.finalorder.AssistedOrderFormGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.finalorder.FreeFormOrderGenerator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GENERATE_DIRECTIONS_ORDER;

@SpringBootTest(classes = {
    JudicialFinalDecisionHandler.class,
    JacksonAutoConfiguration.class,
})
class JudicialFinalDecisionHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private JudicialFinalDecisionHandler handler;
    @MockBean
    private FreeFormOrderGenerator freeFormOrderGenerator;
    @MockBean
    private AssistedOrderFormGenerator assistedOrderFormGenerator;
    @Autowired
    private ObjectMapper objMapper;
    @MockBean
    private GeneralAppLocationRefDataService locationRefDataService;

    private static final String ON_INITIATIVE_SELECTION_TEST = "As this order was made on the court's own initiative, "
            + "any party affected by the order may apply to set aside, vary, or stay the order."
            + " Any such application must be made by 4pm on";
    private static final String WITHOUT_NOTICE_SELECTION_TEXT = "If you were not notified of the application before "
            + "this order was made, you may apply to set aside, vary, or stay the order."
            + " Any such application must be made by 4pm on";
    private static final String ORDERED_TEXT = "order test";

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(GENERATE_DIRECTIONS_ORDER);
    }

    @Test
    void setCaseName() {
        CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .build().toBuilder()
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();
        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_START);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response.getData().get("caseNameHmctsInternal")
                .toString()).isEqualTo("Mr. John Rambo v Mr. Sole Trader");
    }

    @Test
    void shouldPopulateFreeFormOrderValues_onMidEventCallback() {
        // Given
        CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft()
                .build().toBuilder().generalAppDetailsOfOrder("order test").build();
        CallbackParams params = callbackParamsOf(caseData, MID, "populate-finalOrder-form-values");
        // When
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        // Then
        assertThat(response.getData()).extracting("orderOnCourtInitiative").extracting("onInitiativeSelectionTextArea")
                .isEqualTo(ON_INITIATIVE_SELECTION_TEST);
        assertThat(response.getData()).extracting("orderOnCourtInitiative").extracting("onInitiativeSelectionDate")
                .isEqualTo(LocalDate.now().plusDays(7).toString());
        assertThat(response.getData()).extracting("orderWithoutNotice").extracting("withoutNoticeSelectionTextArea")
                .isEqualTo(WITHOUT_NOTICE_SELECTION_TEXT);
        assertThat(response.getData()).extracting("orderWithoutNotice").extracting("withoutNoticeSelectionDate")
                .isEqualTo(LocalDate.now().plusDays(7).toString());

    }

    @Test
    void shouldPopulate_AssistedOrderFormOrderValues_onMidEventCallback() {

        // Given
        List<LocationRefData> locations = new ArrayList<>();
        locations.add(LocationRefData.builder().siteName("Site Name 1").courtAddress("Address1").postcode("18000")
                          .build());
        locations.add(LocationRefData.builder().siteName("Site Name 2").courtAddress("Address2").postcode("28000")
                          .build());
        when(locationRefDataService.getCourtLocations(any())).thenReturn(locations);
        CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft()
            .build().toBuilder().generalAppDetailsOfOrder("order test").build();
        CallbackParams params = callbackParamsOf(caseData, MID, "populate-finalOrder-form-values");
        // When
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        // Then
        assertThat(response.getData()).extracting("orderMadeOnOwnInitiative").extracting("detailText")
            .isEqualTo(ON_INITIATIVE_SELECTION_TEST);
        assertThat(response.getData()).extracting("orderMadeOnWithOutNotice").extracting("detailText")
            .isEqualTo(WITHOUT_NOTICE_SELECTION_TEXT);
        assertThat(response.getData()).extracting("assistedOrderMadeDateHeardDetails").extracting("singleDateSelection")
            .isNotNull();

        LocalDate localDatePlus14days = LocalDate.now().plusDays(14);
        assertThat(response.getData()).extracting("claimantCostStandardBase").extracting("costPaymentDeadLine")
            .isEqualTo(localDatePlus14days.toString());
        assertThat(response.getData()).extracting("claimantCostSummarilyBase").extracting("costPaymentDeadLine")
            .isEqualTo(localDatePlus14days.toString());
        assertThat(response.getData()).extracting("defendantCostStandardBase").extracting("costPaymentDeadLine")
            .isEqualTo(localDatePlus14days.toString());
        assertThat(response.getData()).extracting("defendantCostSummarilyBase").extracting("costPaymentDeadLine")
            .isEqualTo(localDatePlus14days.toString());

        assertThat(((Map)((ArrayList)((Map)((Map)(response.getData().get("assistedOrderFurtherHearingDetails")))
            .get("alternativeHearingLocation")).get("list_items")).get(0))
                       .get("label")).isEqualTo("Site Name 1 - Address1 - 18000");

        assertThat(response.getData()).extracting("assistedOrderOrderedThatText")
            .isEqualTo(ORDERED_TEXT);

    }

    @Test
    void shouldShowError_When_OrderDateIsFutureDate_FinalOrderPreviewDoc_onMidEventCallback() {

        // Given
        when(freeFormOrderGenerator.generate(any(), any())).thenReturn(
            CaseDocument.builder().documentLink(Document.builder().build()).build());
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
            .build()
            .toBuilder()
            .finalOrderSelection(FinalOrderSelection.FREE_FORM_ORDER)
            .generalAppDetailsOfOrder("order test")
            .assistedOrderMadeSelection(YesOrNo.YES)
            .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().singleDateSelection(
                    AssistedOrderDateHeard.builder().singleDate(LocalDate.now().plusDays(1)).build()).build())
            .build();
        CallbackParams params = callbackParamsOf(caseData, MID, "populate-final-order-preview-doc");
        // When
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        // Then
        assertThat(response.getErrors()).isNotEmpty();

    }

    @Test
    void shouldNotShowError_When_OrderDateIsTodayDate_FinalOrderPreviewDoc_onMidEventCallback() {

        // Given
        when(freeFormOrderGenerator.generate(any(), any())).thenReturn(
            CaseDocument.builder().documentLink(Document.builder().build()).build());
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
            .build()
            .toBuilder()
            .finalOrderSelection(FinalOrderSelection.FREE_FORM_ORDER)
            .generalAppDetailsOfOrder("order test")
            .assistedOrderMadeSelection(YesOrNo.YES)
            .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().singleDateSelection(
                AssistedOrderDateHeard.builder().singleDate(LocalDate.now()).build()).build())
            .build();
        CallbackParams params = callbackParamsOf(caseData, MID, "populate-final-order-preview-doc");
        // When
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        // Then
        assertThat(response.getErrors()).isEmpty();

    }

    @Test
    void shouldNotShowError_When_OrderDateIsSingleSelection() {

        when(assistedOrderFormGenerator.generate(any(), any()))
            .thenReturn(CaseDocument.builder().documentLink(Document.builder().build()).build());
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
            .build()
            .toBuilder().finalOrderSelection(FinalOrderSelection.ASSISTED_ORDER)
            .generalAppDetailsOfOrder("order test")
            .assistedOrderMadeSelection(YesOrNo.YES)
            .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().singleDateSelection(
                AssistedOrderDateHeard.builder().singleDate(LocalDate.now()).build()).build()).build();
        CallbackParams params = callbackParamsOf(caseData, MID, "populate-final-order-preview-doc");

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        CaseData updatedData = objMapper.convertValue(response.getData(), CaseData.class);
        assertThat(updatedData.getGaFinalOrderDocPreview()).isNotNull();

    }

    @Test
    void shouldShowError_When_OrderDateIsSingleSelectionAfterTodayDate() {

        when(assistedOrderFormGenerator.generate(any(), any()))
            .thenReturn(CaseDocument.builder().documentLink(Document.builder().build()).build());
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
            .build()
            .toBuilder().finalOrderSelection(FinalOrderSelection.ASSISTED_ORDER)
            .generalAppDetailsOfOrder("order test")
            .assistedOrderMadeSelection(YesOrNo.YES)
            .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().singleDateSelection(
                AssistedOrderDateHeard.builder().singleDate(LocalDate.now().plusDays(1)).build()).build()).build();
        CallbackParams params = callbackParamsOf(caseData, MID, "populate-final-order-preview-doc");

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response.getErrors()).isNotNull();

    }

    @Test
    void shouldNotShowError_When_OrderDateIsDateRange_FromIsAfter() {

        when(assistedOrderFormGenerator.generate(any(), any()))
            .thenReturn(CaseDocument.builder().documentLink(Document.builder().build()).build());
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
            .build()
            .toBuilder().finalOrderSelection(FinalOrderSelection.ASSISTED_ORDER)
            .generalAppDetailsOfOrder("order test")
            .assistedOrderMadeSelection(YesOrNo.YES)
            .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().dateRangeSelection(
                AssistedOrderDateHeard.builder().dateRangeFrom(LocalDate.now().plusDays(1))
                    .dateRangeTo(LocalDate.now()).build()).build()).build();
        CallbackParams params = callbackParamsOf(caseData, MID, "populate-final-order-preview-doc");

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response.getErrors()).isNotNull();

    }

    @Test
    void shouldNotShowError_When_OrderDateIsDateRange_ToIsAfter() {

        when(assistedOrderFormGenerator.generate(any(), any()))
            .thenReturn(CaseDocument.builder().documentLink(Document.builder().build()).build());
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
            .build()
            .toBuilder().finalOrderSelection(FinalOrderSelection.ASSISTED_ORDER)
            .generalAppDetailsOfOrder("order test")
            .assistedOrderMadeSelection(YesOrNo.YES)
            .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().dateRangeSelection(
                AssistedOrderDateHeard.builder().dateRangeFrom(LocalDate.now())
                    .dateRangeTo(LocalDate.now().plusDays(1)).build()).build()).build();
        CallbackParams params = callbackParamsOf(caseData, MID, "populate-final-order-preview-doc");

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response.getErrors()).isNotNull();
    }

    @Test
    void shouldShowError_When_OrderDateIsDateRange_FromIsAfterDateTo() {

        when(assistedOrderFormGenerator.generate(any(), any()))
            .thenReturn(CaseDocument.builder().documentLink(Document.builder().build()).build());
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
            .build()
            .toBuilder().finalOrderSelection(FinalOrderSelection.ASSISTED_ORDER)
            .generalAppDetailsOfOrder("order test")
            .assistedOrderMadeSelection(YesOrNo.YES)
            .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().dateRangeSelection(
                AssistedOrderDateHeard.builder().dateRangeFrom(LocalDate.now().minusDays(2))
                    .dateRangeTo(LocalDate.now().minusDays(3)).build()).build()).build();
        CallbackParams params = callbackParamsOf(caseData, MID, "populate-final-order-preview-doc");

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response.getErrors()).isNotNull();
    }

    @Test
    void shouldNotShowError_When_OrderDateIsDateRange_FromIsAfterDateTo() {

        when(assistedOrderFormGenerator.generate(any(), any()))
            .thenReturn(CaseDocument.builder().documentLink(Document.builder().build()).build());
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
            .build()
            .toBuilder().finalOrderSelection(FinalOrderSelection.ASSISTED_ORDER)
            .generalAppDetailsOfOrder("order test")
            .assistedOrderMadeSelection(YesOrNo.YES)
            .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().dateRangeSelection(
                AssistedOrderDateHeard.builder().dateRangeFrom(LocalDate.now().minusDays(2))
                    .dateRangeTo(LocalDate.now()).build()).build()).build();
        CallbackParams params = callbackParamsOf(caseData, MID, "populate-final-order-preview-doc");

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void shouldNotShowError_When_AssistedOrderNotMade_FinalOrderPreviewDoc_onMidEventCallback() {

        // Given
        when(freeFormOrderGenerator.generate(any(), any())).thenReturn(
            CaseDocument.builder().documentLink(Document.builder().build()).build());
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
            .build()
            .toBuilder()
            .finalOrderSelection(FinalOrderSelection.FREE_FORM_ORDER)
            .generalAppDetailsOfOrder("order test")
            .assistedOrderMadeSelection(YesOrNo.NO)
            .assistedOrderMadeDateHeardDetails(AssistedOrderMadeDateHeardDetails.builder().singleDateSelection(
                AssistedOrderDateHeard.builder().singleDate(LocalDate.now()).build()).build())
            .build();
        CallbackParams params = callbackParamsOf(caseData, MID, "populate-final-order-preview-doc");
        // When
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        // Then
        assertThat(response.getErrors()).isEmpty();

    }

    @Test
    void shouldGenerateFinalOrderPreviewDocumentWhenPopulateFinalOrderPreviewDocIsCalled() {
        when(freeFormOrderGenerator.generate(any(), any())).thenReturn(CaseDocument
                .builder().documentLink(Document.builder().build()).build());
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
                .build()
                .toBuilder().finalOrderSelection(FinalOrderSelection.FREE_FORM_ORDER).build();
        CallbackParams params = callbackParamsOf(caseData, MID, "populate-final-order-preview-doc");

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        CaseData updatedData = objMapper.convertValue(response.getData(), CaseData.class);
        assertThat(updatedData.getGaFinalOrderDocPreview()).isNotNull();
    }

    @Test
    void shouldGenerateAssistedOrderPreviewDocumentWhenPopulateFinalOrderPreviewDocIsCalled() {
        when(assistedOrderFormGenerator.generate(any(), any()))
            .thenReturn(CaseDocument.builder().documentLink(Document.builder().build()).build());
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
            .build()
            .toBuilder().finalOrderSelection(FinalOrderSelection.ASSISTED_ORDER).build();
        CallbackParams params = callbackParamsOf(caseData, MID, "populate-final-order-preview-doc");

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        CaseData updatedData = objMapper.convertValue(response.getData(), CaseData.class);
        assertThat(updatedData.getGaFinalOrderDocPreview()).isNotNull();
    }

    @Nested
    class GetAllPartyNames {
        @Test
        void oneVOne() {
            CaseData caseData = CaseDataBuilder.builder()
                    .atStateClaimDraft()
                    .build().toBuilder()
                    .claimant1PartyName("Mr. John Rambo")
                    .defendant1PartyName("Mr. Sole Trader")
                    .build();
            String title = JudicialFinalDecisionHandler.getAllPartyNames(caseData);
            assertThat(title).isEqualTo("Mr. John Rambo v Mr. Sole Trader");
        }

        @Test
        void oneVTwoSameSol() {
            CaseData caseData = CaseDataBuilder.builder()
                    .atStateClaimDraft()
                    .build().toBuilder()
                    .respondent2SameLegalRepresentative(YesOrNo.YES)
                    .claimant1PartyName("Mr. John Rambo")
                    .defendant1PartyName("Mr. Sole Trader")
                    .defendant2PartyName("Mr. John Rambo")
                    .build();

            String title = JudicialFinalDecisionHandler.getAllPartyNames(caseData);
            assertThat(title).isEqualTo("Mr. John Rambo v Mr. Sole Trader");
        }

        @Test
        void oneVTwo() {
            CaseData caseData = CaseDataBuilder.builder()
                    .atStateClaimDraft()
                    .build().toBuilder()
                    .respondent2SameLegalRepresentative(YesOrNo.NO)
                    .claimant1PartyName("Mr. John Rambo")
                    .defendant1PartyName("Mr. Sole Trader")
                    .defendant2PartyName("Mr. John Rambo")
                    .build();

            String title = JudicialFinalDecisionHandler.getAllPartyNames(caseData);
            assertThat(title).isEqualTo("Mr. John Rambo v Mr. Sole Trader, Mr. John Rambo");
        }
    }

    @Nested
    class AboutToSubmitHandling {

        @Test
        void shouldSetUpReadyBusinessProcess() {
            CaseData caseData = CaseData.builder().gaFinalOrderDocPreview(Document.builder().build()).build();

            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getGaFinalOrderDocPreview()).isNull();
            //assertThat(responseCaseData.getBusinessProcess().getStatus()).isEqualTo(BusinessProcessStatus.READY);
            //assertThat(responseCaseData.getBusinessProcess().getCamundaEvent()).isEqualTo("MAKE_DECISION");
        }
    }

    @Nested
    class SubmittedCallback {
        @Test
        void shouldReturnExpectedSubmittedCallbackResponse_whenInvoked1v1() {
            String body = "<br/><p>The order has been sent to: </p>%n%n ## Claimant 1 %n%n Mr. John Rambo%n%n "
                    + "## Defendant 1 %n%n Mr. Sole Trader";
            String header = "# Your order has been issued %n%n ## Case number %n%n # 1678-3567-4955-5475";
            CaseData caseData = CaseDataBuilder.builder()
                    .atStateClaimDraft()
                    .ccdCaseReference(1678356749555475L)
                    .build().toBuilder()
                    .respondent2SameLegalRepresentative(YesOrNo.NO)
                    .claimant1PartyName("Mr. John Rambo")
                    .defendant1PartyName("Mr. Sole Trader")
                    .build();
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);
            SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);
            assertThat(response).usingRecursiveComparison().isEqualTo(
                    SubmittedCallbackResponse.builder()
                            .confirmationHeader(format(header))
                            .confirmationBody(format(body))
                            .build());
        }
    }
}
