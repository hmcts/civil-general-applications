package uk.gov.hmcts.reform.civil.model.genapplication.finalorder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.dq.ClaimantRepresentationType;
import uk.gov.hmcts.reform.civil.enums.dq.DefendantRepresentationType;

@Setter
@Data
@Builder(toBuilder = true)
public class ClaimantDefendantRepresentation {

    private ClaimantRepresentationType claimantRepresentation;
    private DefendantRepresentationType defendantRepresentation;
    private HeardClaimantNotAttend heardFromClaimantNotAttend;
    private HeardDefendantNotAttend heardFromDefendantNotAttend;
    private String detailsRepresentationText;

    @JsonCreator
    ClaimantDefendantRepresentation(@JsonProperty("ClaimantRepresentation")
                                    ClaimantRepresentationType claimantRepresentation,
                                    @JsonProperty("defendantRepresentation")
                                    DefendantRepresentationType defendantRepresentation,
                                    @JsonProperty("heardFromClaimantNotAttend")
                                    HeardClaimantNotAttend heardFromClaimantNotAttend,
                                    @JsonProperty("heardFromDefendantNotAttend")
                                    HeardDefendantNotAttend heardFromDefendantNotAttend,
                                    @JsonProperty("detailsRepresentationText")
                                    String detailsRepresentationText) {

        this.claimantRepresentation = claimantRepresentation;
        this.defendantRepresentation = defendantRepresentation;
        this.heardFromClaimantNotAttend = heardFromClaimantNotAttend;
        this.heardFromDefendantNotAttend = heardFromDefendantNotAttend;
        this.detailsRepresentationText = detailsRepresentationText;
    }
}
