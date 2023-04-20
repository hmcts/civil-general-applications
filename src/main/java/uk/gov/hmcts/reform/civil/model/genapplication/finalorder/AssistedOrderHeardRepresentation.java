package uk.gov.hmcts.reform.civil.model.genapplication.finalorder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.dq.FinalOrderConsideredToggle;
import uk.gov.hmcts.reform.civil.enums.dq.HeardFromRepresentationTypes;

import java.util.List;

@Setter
@Data
@Builder(toBuilder = true)
public class AssistedOrderHeardRepresentation {

    private final HeardFromRepresentationTypes representationType;
    private final ClaimantDefendantRepresentation claimantDefendantRepresentation;
    private final DetailText otherRepresentation;
    private final List<FinalOrderConsideredToggle> typeRepresentationJudgePapersList;

    @JsonCreator
    AssistedOrderHeardRepresentation(@JsonProperty("representationType")
                                     HeardFromRepresentationTypes representationType,
                                     @JsonProperty("claimantDefendantRepresentation")
                                     ClaimantDefendantRepresentation claimantDefendantRepresentation,
                                     @JsonProperty("otherRepresentation") DetailText otherRepresentation,
                                     @JsonProperty("typeRepresentationJudgePapersList")
                                     List<FinalOrderConsideredToggle> typeRepresentationJudgePapersList) {
        this.representationType = representationType;
        this.claimantDefendantRepresentation = claimantDefendantRepresentation;
        this.otherRepresentation = otherRepresentation;
        this.typeRepresentationJudgePapersList = typeRepresentationJudgePapersList;
    }
}
