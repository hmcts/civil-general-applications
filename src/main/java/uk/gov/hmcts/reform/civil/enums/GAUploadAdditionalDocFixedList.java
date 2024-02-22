package uk.gov.hmcts.reform.civil.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;

@Getter
@RequiredArgsConstructor
public enum GAUploadAdditionalDocFixedList {

    BUNDLE(DocumentType.BUNDLE),
    COSTS_SCHEDULE(DocumentType.COSTS_SCHEDULE),
    WITNESS(DocumentType.WITNESS);

    private final DocumentType displayedValue;
}
