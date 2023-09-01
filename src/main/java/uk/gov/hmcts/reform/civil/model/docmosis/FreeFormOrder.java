package uk.gov.hmcts.reform.civil.model.docmosis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;

@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class FreeFormOrder implements MappableObject {

    private final String caseNumber;
    private final String caseName;
    private final String receivedDate;
    private final String claimantReference;
    private final String defendantReference;
    private final String freeFormRecitalText;
    private final String freeFormOrderedText;
    private final String freeFormOrderValue;
}
