package uk.gov.hmcts.reform.civil.model.documents;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder(toBuilder = true)
public class Document {

    String documentUrl;
    String documentBinaryUrl;
    String documentFileName;
    String documentHash;
    String categoryID;
    String uploadTimestamp;

    @JsonCreator
    public Document(@JsonProperty("document_url") String documentUrl,
                    @JsonProperty("document_binary_url") String documentBinaryUrl,
                    @JsonProperty("document_filename") String documentFileName,
                    @JsonProperty("document_hash") String documentHash,
                    @JsonProperty("category_id") String categoryID,
                    @JsonProperty("upload_timestamp") String uploadTimestamp) {
        this.documentUrl = documentUrl;
        this.documentBinaryUrl = documentBinaryUrl;
        this.documentFileName = documentFileName;
        this.documentHash = documentHash;
        this.categoryID = categoryID;
        this.uploadTimestamp = uploadTimestamp;
    }

    @JsonIgnore
    public static Document toDocument(Document document, DocumentType documentType) {
        return Document.builder()
            .documentUrl(document.getDocumentUrl())
            .documentBinaryUrl(document.getDocumentBinaryUrl())
            .documentFileName(getDocumentName(documentType, document.getDocumentFileName()))
            .documentHash(document.getDocumentHash())
            .categoryID(document.getCategoryID())
            .uploadTimestamp(document.getUploadTimestamp())
            .build();
    }

    public static String getDocumentName(DocumentType documentType, String documentFileName) {

        int lastDotIndex = documentFileName.lastIndexOf('.');
        String extension = documentFileName.substring(lastDotIndex + 1);

        switch (documentType) {
            case GENERAL_APPLICATION_DRAFT:
                return getFileName("Translated_draft_application_%s", extension);
            case GENERAL_ORDER:
                return getFileName("Translated_General_order_for_application_%s", extension);
            case DISMISSAL_ORDER:
                return getFileName("Translated_Dismissal_order_for_application_%s", extension);
            case DIRECTION_ORDER:
                return getFileName("Translated_Direction_order_for_application_%s", extension);
            case SEND_APP_TO_OTHER_PARTY:
                return "Translated Court document" + "." + extension;
            case HEARING_ORDER:
                return getFileName("Translated_Hearing_order_for_application_%s", extension);
            case HEARING_NOTICE:
                return getFileName("Translated_Application_Hearing_Notice_%s", extension);
            case REQUEST_FOR_INFORMATION:
                return getFileName("Translated_Request_for_information_for_application_%s", extension);
            case REQUEST_MORE_INFORMATION_APPLICANT_TRANSLATED, REQUEST_MORE_INFORMATION_RESPONDENT_TRANSLATED:
                return getFileName("Respond_for_information_for_application_%s", extension);
            case WRITTEN_REPRESENTATION_SEQUENTIAL:
                return getFileName("Translated_Order_Written_Representation_Sequential_for_application_%s", extension);
            case WRITTEN_REPRESENTATION_CONCURRENT:
                return getFileName("Translated_Order_Written_Representation_Concurrent_for_application_%s", extension);
            case WRITTEN_REPRESENTATION_APPLICANT_TRANSLATED, WRITTEN_REPRESENTATION_RESPONDENT_TRANSLATED:
                return getFileName("Respond_for_written_representation_for_application_%s", extension);
            default:
                return documentFileName;
        }
    }

    private static String getFileName(String documentName, String extension) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(documentName, LocalDateTime.now().format(formatter)) + "." + extension;
    }

}
