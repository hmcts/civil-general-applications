package uk.gov.hmcts.reform.civil.model.citizenui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentUploadException;

import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.DIRECTION_ORDER;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.DISMISSAL_ORDER;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.GENERAL_ORDER;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.HEARING_ORDER;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.JUDGES_DIRECTIONS_APPLICANT_TRANSLATED;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.JUDGES_DIRECTIONS_RESPONDENT_TRANSLATED;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.REQUEST_FOR_INFORMATION;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.REQUEST_MORE_INFORMATION_APPLICANT_TRANSLATED;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.REQUEST_MORE_INFORMATION_RESPONDENT_TRANSLATED;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.SEND_APP_TO_OTHER_PARTY;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TranslatedDocument {

    private Document file;
    private TranslatedDocumentType documentType;

    @JsonIgnore
    public DocumentType getCorrespondingDocumentType(TranslatedDocumentType documentType) {
        switch (documentType) {
            case REQUEST_FOR_MORE_INFORMATION_ORDER:
                return REQUEST_FOR_INFORMATION;
            case REQUEST_MORE_INFORMATION_APPLICANT:
                return REQUEST_MORE_INFORMATION_APPLICANT_TRANSLATED;
            case REQUEST_MORE_INFORMATION_RESPONDENT:
                return REQUEST_MORE_INFORMATION_RESPONDENT_TRANSLATED;
            case GENERAL_ORDER:
                return GENERAL_ORDER;
            case JUDGES_DIRECTIONS_ORDER:
                return DIRECTION_ORDER;
            case JUDGES_DIRECTIONS_APPLICANT:
                return JUDGES_DIRECTIONS_APPLICANT_TRANSLATED;
            case JUDGES_DIRECTIONS_RESPONDENT:
                return JUDGES_DIRECTIONS_RESPONDENT_TRANSLATED;
            case HEARING_ORDER:
                return HEARING_ORDER;
            case WITHOUT_NOTICE_TO_WITH_NOTICE_DOCUMENT:
                return SEND_APP_TO_OTHER_PARTY;
            case DISMISSAL_ORDER:
                return DISMISSAL_ORDER;
            default:
                throw new DocumentUploadException("No document file type found for Translated document");
        }
    }
}
