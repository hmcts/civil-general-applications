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
            .documentName(document.documentFileName)
            .documentType(setOnlyCCDDocumentTypes(documentType))
            .createdDatetime(LocalDateTime.now())
            .createdBy(translator)
            .build();
    }

    public static DocumentType setOnlyCCDDocumentTypes(DocumentType documentType) {
        switch (documentType) {
            case REQUEST_MORE_INFORMATION_APPLICANT_TRANSLATED:
            case REQUEST_MORE_INFORMATION_RESPONDENT_TRANSLATED:
            case JUDGES_DIRECTIONS_RESPONDENT_TRANSLATED:
            case JUDGES_DIRECTIONS_APPLICANT_TRANSLATED:
            case WRITTEN_REPRESENTATION_APPLICANT_TRANSLATED:
            case WRITTEN_REPRESENTATION_RESPONDENT_TRANSLATED:
            case UPLOADED_DOCUMENT_RESPONDENT:
            case UPLOADED_DOCUMENT_APPLICANT:
                return null;
            default:
                return documentType;
        }
    }
}
