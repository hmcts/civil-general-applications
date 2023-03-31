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
public class AssistedOrderForm implements MappableObject {

    private final String caseNumber;
    private final String caseName;
    private final String receivedDate;
    private final String claimantReference;
    private final String defendantReference;
    //Order Made
    private final Boolean isOrderMade;
    private final String orderMadeDate;
    //Judge HearFrom Section
    private final boolean isBothClaimantDefendantAttended;
    private final boolean isOnlyClaimantAttended;
    private final boolean isOnlyDefendantAttended;
    private final boolean isBothClaimantDefendantNotAttended;
    private final boolean isOtherRepresentation;
    private final String judgeHeardFromText;
    private final String recitalRecordedText;
    private final String orderedText;
    private final String costsText;


    private final String freeFormRecitalText;
    private final String freeFormRecordedText;
    private final String freeFormOrderedText;
    private final String freeFormOrderValue;
}
