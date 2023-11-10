package uk.gov.hmcts.reform.civil.service.docmosis.finalorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.civil.enums.dq.OrderOnCourts;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.genapplication.FreeFormOrderValues;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDocumentBuilder;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.ListGeneratorService;
import uk.gov.hmcts.reform.civil.service.documentmanagement.UnsecuredDocumentManagementService;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.OrderOnCourts.ORDER_ON_COURT_INITIATIVE;
import static uk.gov.hmcts.reform.civil.enums.dq.OrderOnCourts.ORDER_WITHOUT_NOTICE;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.GENERAL_ORDER;

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
    private UnsecuredDocumentManagementService documentManagementService;

    @MockBean
    private DocumentGeneratorService documentGeneratorService;

    @MockBean
    private IdamClient idamClient;
    @MockBean
    private ListGeneratorService listGeneratorService;

    @Autowired
    private FreeFormOrderGenerator generator;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void shouldHearingFormGeneratorOneForm_whenValidDataIsProvided() {
        when(documentGeneratorService
                .generateDocmosisDocument(
                        any(MappableObject.class), eq(DocmosisTemplates.FREE_FORM_ORDER)))
                .thenReturn(new DocmosisDocument(
                        DocmosisTemplates.FREE_FORM_ORDER.getDocumentTitle(), bytes));

        when(idamClient
                .getUserDetails(any()))
                .thenReturn(UserDetails.builder().forename("John").surname("Doe").build());
        when(listGeneratorService
                .claimantsName(any()))
                .thenReturn("claimant");
        when(listGeneratorService
                .defendantsName(any()))
                .thenReturn("defendant");

        when(documentManagementService
                .uploadDocument(any(), any()))
                .thenReturn(CASE_DOCUMENT);

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
    void test_getCaseNumberFormatted() {
        CaseData caseData = CaseDataBuilder.builder().ccdCaseReference(1644495739087775L).build();
        String formattedCaseNumber = generator.getCaseNumberFormatted(caseData);
        assertThat(formattedCaseNumber).isEqualTo("1644-4957-3908-7775");
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
        assertThat(generator.getTemplate()).isEqualTo(DocmosisTemplates.FREE_FORM_ORDER);
    }
}
