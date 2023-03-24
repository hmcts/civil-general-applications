package uk.gov.hmcts.reform.civil.model.genapplication.finalorder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.dq.ClaimantRepresentationType;
import uk.gov.hmcts.reform.civil.enums.dq.DefendantRepresentationType;

import java.util.List;

@Setter
@Data
@Builder(toBuilder = true)
public class ClaimantDefendantRepresentation {

    private List<ClaimantRepresentationType> claimantRepresentation;
    private List<DefendantRepresentationType> defendantRepresentation;

    @JsonCreator
    ClaimantDefendantRepresentation(@JsonProperty("ClaimantRepresentation")
                                    List<ClaimantRepresentationType> claimantRepresentation,
                                    @JsonProperty("date") List<DefendantRepresentationType> defendantRepresentation) {
        this.claimantRepresentation = claimantRepresentation;
        this.defendantRepresentation = defendantRepresentation;
    }
}
