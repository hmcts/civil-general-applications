package uk.gov.hmcts.reform.civil.service.docmosis.finalorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.civil.enums.dq.OrderOnCourts;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.FreeFormOrder;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.genapplication.FreeFormOrderValues;
import uk.gov.hmcts.reform.civil.model.genapplication.GACaseLocation;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDocumentBuilder;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.SecuredDocumentManagementService;
import uk.gov.hmcts.reform.civil.service.flowstate.FlowFlag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.OrderOnCourts.ORDER_ON_COURT_INITIATIVE;
import static uk.gov.hmcts.reform.civil.enums.dq.OrderOnCourts.ORDER_WITHOUT_NOTICE;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.GENERAL_ORDER;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.POST_JUDGE_FREE_FORM_ORDER_LIP;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    FreeFormOrderGenerator.class,
    JacksonAutoConfiguration.class
})
class FreeFormOrderGeneratorTest {

    private static final String BEARER_TOKEN = "Bearer Token";
    private static final byte[] bytes = {1, 2, 3, 4, 5, 6};

    private static final String templateName = "Free_form_order_%s.pdf";
    private static final String fileName_application = String.format(templateName,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    private static final CaseDocument CASE_DOCUMENT = CaseDocumentBuilder.builder()
            .documentName(fileName_application)
            .documentType(GENERAL_ORDER)
            .build();

    @MockBean
    private SecuredDocumentManagementService documentManagementService;

    @MockBean
    private DocumentGeneratorService documentGeneratorService;

    @Autowired
    private FreeFormOrderGenerator generator;
    @Autowired
    private ObjectMapper mapper;
    @MockBean
    private DocmosisService docmosisService;

    @Nested
    class GetTemplateDataLip {
        @Test
        void whenJudgeMakeDecision_ShouldGetFreeFormOrderData_1V1() {
            CaseData caseData = CaseDataBuilder.builder()
                .finalOrderFreeForm().build().toBuilder()
                .defendant2PartyName(null)
                .claimant2PartyName(null)
                .parentClaimantIsApplicant(NO)
                .caseManagementLocation(GACaseLocation.builder().baseLocation("3").build())
                .isMultiParty(NO)
                .build();

            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder()
                                .epimmsId("2")
                                .externalShortName("Manchester")
                                .build());
            FreeFormOrder templateDate = generator.getTemplateData(CaseDataBuilder.builder().getCivilCaseData(), caseData, "auth", FlowFlag.POST_JUDGE_ORDER_LIP_RESPONDENT);
            assertThatFieldsAreCorrect_FreeFormOrder_1V1(templateDate, caseData);
        }

        @Test
        void shouldHearingFormGeneratorOneForm_whenValidDataIsProvided() {
            when(documentGeneratorService
                     .generateDocmosisDocument(
                         any(MappableObject.class), eq(DocmosisTemplates.POST_JUDGE_FREE_FORM_ORDER_LIP)))
                .thenReturn(new DocmosisDocument(
                    DocmosisTemplates.POST_JUDGE_FREE_FORM_ORDER_LIP.getDocumentTitle(), bytes));
            when(documentManagementService
                     .uploadDocument(any(), any()))
                .thenReturn(CASE_DOCUMENT);
            when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
                .thenReturn(LocationRefData.builder().epimmsId("2").venueName("London").build());

            CaseData caseData = CaseDataBuilder.builder().hearingScheduledApplication(YES).build()
                .toBuilder()
                .freeFormRecitalText("RecitalText")
                .freeFormOrderedText("OrderedText")
                .orderOnCourtsList(OrderOnCourts.NOT_APPLICABLE)
                .build();
            CaseDocument caseDocuments = generator.generate(CaseDataBuilder.builder().getCivilCaseData(), caseData, BEARER_TOKEN, FlowFlag.POST_JUDGE_ORDER_LIP_RESPONDENT);

            assertThat(caseDocuments).isNotNull();

            verify(documentGeneratorService)
                .generateDocmosisDocument(any(FreeFormOrder.class), eq(POST_JUDGE_FREE_FORM_ORDER_LIP));
            verify(documentManagementService).uploadDocument(any(), any());
        }

        private void assertThatFieldsAreCorrect_FreeFormOrder_1V1(FreeFormOrder freeFormOrder,
                                                                  CaseData caseData) {
            Assertions.assertAll(
                "GeneralOrderDocument data should be as expected",
                () -> assertEquals(freeFormOrder.getClaimant1Name(), caseData.getClaimant1PartyName()),
                () -> assertEquals(freeFormOrder.getJudgeNameTitle(), caseData.getJudgeTitle()),
                () -> assertNull(freeFormOrder.getClaimant2Name()),
                () -> assertEquals(freeFormOrder.getCourtName(), "Manchester"),
                () -> assertEquals(freeFormOrder.getDefendant1Name(), caseData.getDefendant1PartyName()),
                () -> assertNull(freeFormOrder.getDefendant2Name()),
                () -> assertEquals(NO, freeFormOrder.getIsMultiParty()),
                () -> assertEquals("applicant1partyname", freeFormOrder.getPartyName()),
                () -> assertEquals("address1", freeFormOrder.getPartyAddressAddressLine1()),
                () -> assertEquals("address2", freeFormOrder.getPartyAddressAddressLine2()),
                () -> assertEquals("address3", freeFormOrder.getPartyAddressAddressLine3()),
                () -> assertEquals("posttown", freeFormOrder.getPartyAddressPostTown()),
                () -> assertEquals("postcode", freeFormOrder.getPartyAddressPostCode()));
        }
    }

    @Test
    void shouldHearingFormGeneratorOneForm_whenValidDataIsProvided() {
        when(documentGeneratorService
                .generateDocmosisDocument(
                        any(MappableObject.class), eq(DocmosisTemplates.FREE_FORM_ORDER)))
                .thenReturn(new DocmosisDocument(
                        DocmosisTemplates.FREE_FORM_ORDER.getDocumentTitle(), bytes));
        when(documentManagementService
                .uploadDocument(any(), any()))
                .thenReturn(CASE_DOCUMENT);
        when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
            .thenReturn(LocationRefData.builder().epimmsId("2").venueName("London").build());

        CaseData caseData = CaseDataBuilder.builder().hearingScheduledApplication(YES).build()
                .toBuilder()
                .freeFormRecitalText("RecitalText")
                .freeFormOrderedText("OrderedText")
                .orderOnCourtsList(OrderOnCourts.NOT_APPLICABLE)
                .build();
        CaseDocument caseDocuments = generator.generate(caseData, BEARER_TOKEN);

        assertThat(caseDocuments).isNotNull();

        verify(documentManagementService)
                .uploadDocument(any(), any());
    }

    @Test
    void shouldThrowExceptionWhenNoLocationMatch() {
        when(documentGeneratorService
                 .generateDocmosisDocument(
                     any(MappableObject.class), eq(DocmosisTemplates.FREE_FORM_ORDER)))
            .thenReturn(new DocmosisDocument(
                DocmosisTemplates.FREE_FORM_ORDER.getDocumentTitle(), bytes));
        when(documentManagementService
                 .uploadDocument(any(), any()))
            .thenReturn(CASE_DOCUMENT);
        doThrow(new IllegalArgumentException("Court Name is not found in location data"))
            .when(docmosisService).getCaseManagementLocationVenueName(any(), any());

        CaseData caseData = CaseDataBuilder.builder().hearingScheduledApplication(YES).build()
            .toBuilder()
            .freeFormRecitalText("RecitalText")
            .caseManagementLocation(GACaseLocation.builder().baseLocation("8").build())
            .freeFormOrderedText("OrderedText")
            .orderOnCourtsList(OrderOnCourts.NOT_APPLICABLE)
            .build();
        Exception exception =
            assertThrows(IllegalArgumentException.class, ()
                -> generator.generate(caseData, BEARER_TOKEN));
        String expectedMessage = "Court Name is not found in location data";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));

    }

    @Test
    void test_getFreeFormOrderValueOnCourt() {
        FreeFormOrderValues values = FreeFormOrderValues.builder()
                .onInitiativeSelectionTextArea("test")
                .onInitiativeSelectionDate(LocalDate.now())
                .build();
        CaseData caseData = CaseData.builder().orderOnCourtsList(ORDER_ON_COURT_INITIATIVE)
                .orderOnCourtInitiative(values).build();
        String orderString = generator.getFreeFormOrderValue(caseData);
        assertThat(orderString).contains("test");
    }

    @Test
    void test_getFreeFormOrderValueWithoutNotice() {
        FreeFormOrderValues values = FreeFormOrderValues.builder()
                .withoutNoticeSelectionTextArea("test")
                .withoutNoticeSelectionDate(LocalDate.now())
                .build();
        CaseData caseData = CaseData.builder().orderOnCourtsList(ORDER_WITHOUT_NOTICE)
                .orderWithoutNotice(values).build();
        String orderString = generator.getFreeFormOrderValue(caseData);
        assertThat(orderString).contains("test");

    }

    @Test
    void test_getFileName() {
        String name = generator.getFileName(DocmosisTemplates.FREE_FORM_ORDER);
        assertThat(name).startsWith("General_order_for_application_");
        assertThat(name).endsWith(".pdf");
    }

    @Test
    void test_getDateFormatted() {
        String dateString = generator.getDateFormatted(LocalDate.EPOCH);
        assertThat(dateString).isEqualTo(" 1 January 1970");
    }

    @Test
    void test_getTemplate() {
        CaseData caseData = CaseDataBuilder.builder().build();
        assertThat(generator.getTemplate(FlowFlag.ONE_RESPONDENT_REPRESENTATIVE)).isEqualTo(DocmosisTemplates.FREE_FORM_ORDER);
    }

    @Test
    void whenJudgeMakeDecision_ShouldGetFreeFormOrderData() {
        CaseData caseData = CaseDataBuilder.builder()
            .finalOrderFreeForm().isMultiParty(YES).build().toBuilder()
            .build();
        when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
            .thenReturn(LocationRefData.builder()
                            .epimmsId("2")
                            .externalShortName("London")
                            .build());
        FreeFormOrder templateDate = generator.getTemplateData(null, caseData, "auth", FlowFlag.ONE_RESPONDENT_REPRESENTATIVE);
        assertThatFieldsAreCorrect_FreeFormOrder(templateDate, caseData);
    }

    private void assertThatFieldsAreCorrect_FreeFormOrder(FreeFormOrder freeFormOrder,
                                                          CaseData caseData) {
        Assertions.assertAll(
            "GeneralOrderDocument data should be as expected",
            () -> assertEquals(freeFormOrder.getClaimant1Name(), caseData.getClaimant1PartyName()),
            () -> assertEquals(freeFormOrder.getJudgeNameTitle(), caseData.getJudgeTitle()),
            () -> assertEquals(freeFormOrder.getClaimant2Name(), caseData.getClaimant2PartyName()),
            () -> assertEquals(freeFormOrder.getCourtName(), "London"),
            () -> assertEquals(freeFormOrder.getDefendant1Name(), caseData.getDefendant1PartyName()),
            () -> assertEquals(freeFormOrder.getDefendant2Name(), caseData.getDefendant2PartyName()),
            () -> assertEquals(YES, freeFormOrder.getIsMultiParty())
        );
    }

    @Test
    void whenJudgeMakeDecision_ShouldGetFreeFormOrderData_1V1() {
        CaseData caseData = CaseDataBuilder.builder()
            .finalOrderFreeForm().build().toBuilder()
            .defendant2PartyName(null)
            .claimant2PartyName(null)
            .caseManagementLocation(GACaseLocation.builder().baseLocation("3").build())
            .isMultiParty(NO)
            .build();

        when(docmosisService.getCaseManagementLocationVenueName(any(), any()))
            .thenReturn(LocationRefData.builder()
                            .epimmsId("2")
                            .externalShortName("Manchester")
                            .build());
        FreeFormOrder templateDate = generator.getTemplateData(null, caseData, "auth", FlowFlag.ONE_RESPONDENT_REPRESENTATIVE);
        assertThatFieldsAreCorrect_FreeFormOrder_1V1(templateDate, caseData);
    }

    private void assertThatFieldsAreCorrect_FreeFormOrder_1V1(FreeFormOrder freeFormOrder,
                                                          CaseData caseData) {
        Assertions.assertAll(
            "GeneralOrderDocument data should be as expected",
            () -> assertEquals(freeFormOrder.getClaimant1Name(), caseData.getClaimant1PartyName()),
            () -> assertEquals(freeFormOrder.getJudgeNameTitle(), caseData.getJudgeTitle()),
            () -> assertNull(freeFormOrder.getClaimant2Name()),
            () -> assertEquals(freeFormOrder.getCourtName(), "Manchester"),
            () -> assertEquals(freeFormOrder.getDefendant1Name(), caseData.getDefendant1PartyName()),
            () -> assertNull(freeFormOrder.getDefendant2Name()),
            () -> assertEquals(NO, freeFormOrder.getIsMultiParty())
        );
    }
}

