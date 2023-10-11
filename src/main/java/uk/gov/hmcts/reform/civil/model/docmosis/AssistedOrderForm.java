package uk.gov.hmcts.reform.civil.model.docmosis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
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
    private final String claimant1Name;
    private final String defendant1Name;
    private final String defendant2Name;
    private final String judgeNameTitle;
    private final String courtLocation;
    //Order Made
    private final YesOrNo isOrderMade;
    private final String orderMadeDate;
    private final Boolean isSingleDate;
    private final String orderMadeSingleDate;
    private final Boolean isDateRange;
    private final String orderMadeDateRangeFrom;
    private final String orderMadeDateRangeTo;
    private final Boolean isBeSpokeRange;
    private final String orderMadeBeSpokeText;
    //Judge HeardFrom Section
    private final boolean judgeHeardFromShowHide;
    private final String judgeHeardSelection;
    private final String claimantRepresentation;
    private final String defendantRepresentation;
    private final String defendantTwoRepresentation;
    private final boolean isOtherRepresentation;
    private final String otherRepresentationText;
    private final String heardClaimantNotAttend;
    private final String heardDefendantNotAttend;
    private final Boolean isDefendantTwoExists;
    private final String heardDefendantTwoNotAttend;
    private final Boolean isJudgeConsidered;
    //Ordered
    private final String orderedText;
    //Recitals
    private final Boolean showRecitals;
    private final String recitalRecordedText;
    private final String judgeHeardFromText;
    private final String costsText;
    private final String furtherHearingText;
    private final String permissionToAppealText;
    private final String orderMadeOnText;
    private final String reasonText;
}
