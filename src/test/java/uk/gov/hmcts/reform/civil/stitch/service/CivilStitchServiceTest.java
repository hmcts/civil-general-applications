package uk.gov.hmcts.reform.civil.stitch.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.documents.DocumentMetaData;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.DocumentConversionService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;
import uk.gov.hmcts.reform.civil.stitch.PdfMerger;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.GENERAL_ORDER;

@ExtendWith(MockitoExtension.class)
class CivilStitchServiceTest {

    private static final String BEARER_TOKEN = "BEARER_TOKEN";

    @InjectMocks
    private CivilStitchService civilStitchService;

    @Mock
    private DocumentManagementService managementService;
    @Mock
    private DocumentConversionService conversionService;

    MockedStatic<PdfMerger> pdfMergerMockedStatic;

    @Test
    void shouldReturnStitchedDocuments() {
        byte[] docArray = {3, 5, 2, 4, 1};
        when(conversionService.convertDocumentToPdf(any(Document.class), anyLong(), anyString())).thenReturn(docArray);
        when(managementService.uploadDocument(anyString(), any(PDF.class))).thenReturn(STITCHED_DOC);
        pdfMergerMockedStatic = Mockito.mockStatic(PdfMerger.class);
        pdfMergerMockedStatic.when(() -> PdfMerger.mergeDocuments(anyList(), anyString())).thenReturn(docArray);

        CaseDocument caseDocument = civilStitchService.generateStitchedCaseDocument(documents,
                                                                                    "stitched-order-000-DC-123.pdf",
                                                                                    1L,
                                                                                    DocumentType.GENERAL_ORDER,
                                                                                    BEARER_TOKEN);
        assertThat(caseDocument).isEqualTo(STITCHED_DOC);
        pdfMergerMockedStatic.close();
    }

    private final List<DocumentMetaData> documents = Arrays.asList(
        new DocumentMetaData(
            GENERAL_ORDER_1.getDocumentLink(),
            "General Order 1",
            LocalDate.now().toString()
        ),
        new DocumentMetaData(
            GENERAL_ORDER_2.getDocumentLink(),
            "General Order 2",
            LocalDate.now().toString()
        )
    );

    private static final CaseDocument GENERAL_ORDER_1 =
        CaseDocument.builder()
            .createdBy("John")
            .documentName(String.format(DocmosisTemplates.GENERAL_ORDER.getDocumentTitle(), "000DC001"))
            .documentSize(0L)
            .documentType(GENERAL_ORDER)
            .createdDatetime(LocalDateTime.now())
            .documentLink(Document.builder()
                              .documentUrl("fake-url")
                              .documentFileName("file-name")
                              .documentBinaryUrl("binary-url")
                              .build())
            .build();

    private static final CaseDocument GENERAL_ORDER_2 =
        CaseDocument.builder()
            .createdBy("John")
            .documentName(String.format(DocmosisTemplates.GENERAL_ORDER.getDocumentTitle(), "000DC001"))
            .documentSize(0L)
            .documentType(GENERAL_ORDER)
            .createdDatetime(LocalDateTime.now())
            .documentLink(Document.builder()
                              .documentUrl("fake-url")
                              .documentFileName("file-name")
                              .documentBinaryUrl("binary-url")
                              .build())
            .build();

    private static final CaseDocument STITCHED_DOC =
        CaseDocument.builder()
            .createdBy("John")
            .documentName("Stitched document")
            .documentSize(0L)
            .documentType(GENERAL_ORDER)
            .createdDatetime(LocalDateTime.now())
            .documentLink(Document.builder()
                              .documentUrl("fake-url")
                              .documentFileName("file-name")
                              .documentBinaryUrl("binary-url")
                              .build())
            .build();

}
