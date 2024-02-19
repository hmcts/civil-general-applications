package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.GAUploadAdditionalDocFixedList;
import uk.gov.hmcts.reform.civil.model.documents.Document;

@Setter
@Data
@Builder(toBuilder = true)
public class UploadDocumentByType {

    private final GAUploadAdditionalDocFixedList documentType;
    private final Document additionalDocument;

    @JsonCreator
    UploadDocumentByType(@JsonProperty("typeOfDocument") GAUploadAdditionalDocFixedList documentType,
                         @JsonProperty("documentUpload") Document additionalDocument) {
        this.documentType = documentType;
        this.additionalDocument = additionalDocument;
    }

}
