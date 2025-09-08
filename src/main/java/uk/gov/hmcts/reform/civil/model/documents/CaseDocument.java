package uk.gov.hmcts.reform.civil.model.documents;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
public class CaseDocument {

    private final Document documentLink;
    private final String documentName;
    private final DocumentType documentType;
    private final long documentSize;
    private final LocalDateTime createdDatetime;
    private final String createdBy;

    @JsonIgnore
    public static CaseDocument toCaseDocument(Document document, DocumentType documentType, String translator) {
        return CaseDocument.builder()
            .documentLink(document)
            .documentName(getDocumentName(documentType, document.documentFileName))
            .documentType(setOnlyCCDDocumentTypes(documentType))
            .createdDatetime(LocalDateTime.now())
            .createdBy(translator)
            .build();
    }

    public static String getDocumentName(DocumentType documentType, String documentFileName) {

        switch (documentType) {
            case GENERAL_APPLICATION_DRAFT:
                return getFileName("Translated_draft_application_%s.pdf");
            case GENERAL_ORDER:
                return getFileName("Translated_General_order_for_application_%s.pdf");
            case DISMISSAL_ORDER:
                return getFileName("Translated_Dismissal_order_for_application_%s.pdf");
            case DIRECTION_ORDER:
                return getFileName("Translated_Direction_order_for_application_%s.pdf");
            case SEND_APP_TO_OTHER_PARTY:
                return "Translated Court document";
            case HEARING_ORDER:
                return getFileName("Translated_Hearing_order_for_application_%s.pdf");
            case HEARING_NOTICE:
                return getFileName("Translated_Application_Hearing_Notice_%s.pdf");
            case REQUEST_FOR_INFORMATION:
                return getFileName("Translated_Request_for_information_for_application_%s.pdf");
            case REQUEST_MORE_INFORMATION_APPLICANT_TRANSLATED, REQUEST_MORE_INFORMATION_RESPONDENT_TRANSLATED:
                return getFileName("Respond_for_information_for_application_%s.pdf");
            case WRITTEN_REPRESENTATION_SEQUENTIAL:
                return getFileName("Translated_Order_Written_Representation_Sequential_for_application_%s.pdf");
            case WRITTEN_REPRESENTATION_CONCURRENT:
                return getFileName("Translated_Order_Written_Representation_Concurrent_for_application_%s.pdf");
            case WRITTEN_REPRESENTATION_APPLICANT_TRANSLATED, WRITTEN_REPRESENTATION_RESPONDENT_TRANSLATED:
                return getFileName("Respond_for_written_representation_for_application_%s.pdf");
            default:
                return documentFileName;
        }
    }

    private static String getFileName(String documentName) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(documentName, LocalDateTime.now().format(formatter));
    }

    public static DocumentType setOnlyCCDDocumentTypes(DocumentType documentType) {
        switch (documentType) {
            case JUDGES_DIRECTIONS_RESPONDENT_TRANSLATED:
            case JUDGES_DIRECTIONS_APPLICANT_TRANSLATED:
            case UPLOADED_DOCUMENT_RESPONDENT:
            case UPLOADED_DOCUMENT_APPLICANT:
                return null;
            default:
                return documentType;
        }
    }
}
