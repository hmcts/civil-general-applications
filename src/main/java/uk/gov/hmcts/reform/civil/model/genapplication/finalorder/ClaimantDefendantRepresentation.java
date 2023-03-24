package uk.gov.hmcts.reform.civil.model.genapplication.finalorder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.dq.ClaimantDefendantNotAttendingType;
import uk.gov.hmcts.reform.civil.enums.dq.ClaimantRepresentationType;
import uk.gov.hmcts.reform.civil.enums.dq.DefendantRepresentationType;

import java.util.List;

@Setter
@Data
@Builder(toBuilder = true)
public class ClaimantDefendantRepresentation {

    private ClaimantRepresentationType claimantRepresentation;
    private DefendantRepresentationType defendantRepresentation;
    private ClaimantDefendantNotAttendingType heardFromClaimantNotAttending;
    private ClaimantDefendantNotAttendingType heardFromDefendantNotAttending;
    private String detailsRepresentationText;

    @JsonCreator
    ClaimantDefendantRepresentation(@JsonProperty("ClaimantRepresentation")
                                    ClaimantRepresentationType claimantRepresentation,
                                    @JsonProperty("defendantRepresentation")
                                    DefendantRepresentationType defendantRepresentation,
                                    @JsonProperty("heardFromClaimantNotAttending")
                                    ClaimantDefendantNotAttendingType heardFromClaimantNotAttending,
                                    @JsonProperty("heardFromDefendantNotAttending")
                                    ClaimantDefendantNotAttendingType heardFromDefendantNotAttending,
                                    @JsonProperty("detailsRepresentationText")
                                    String detailsRepresentationText) {

        this.claimantRepresentation = claimantRepresentation;
        this.defendantRepresentation = defendantRepresentation;
        this.heardFromClaimantNotAttending = heardFromClaimantNotAttending;
        this.heardFromDefendantNotAttending = heardFromDefendantNotAttending;
        this.detailsRepresentationText = detailsRepresentationText;
    }
}
