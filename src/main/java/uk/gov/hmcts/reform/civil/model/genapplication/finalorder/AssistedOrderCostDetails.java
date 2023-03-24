package uk.gov.hmcts.reform.civil.model.genapplication.finalorder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.dq.AssistedCostTypesList;

import java.util.List;

@Setter
@Data
@Builder(toBuilder = true)
public class AssistedOrderCostDetails {

    private final List<AssistedCostTypesList> assistedCostTypes;
    private final AssistedOrderCost claimantCostStandardBase;
    private final AssistedOrderCost defendantCostStandardBase;
    private final AssistedOrderCost claimantCostSummarilyBase;
    private final AssistedOrderCost defendantCostSummarilyBase;
    private final DetailText costReservedDetails;
    private final DetailText besPokeCostDetails;

    @JsonCreator
    AssistedOrderCostDetails(@JsonProperty("assistedCostTypes") List<AssistedCostTypesList> assistedCostTypes,
                             @JsonProperty("claimantCostStandardBase") AssistedOrderCost claimantCostStandardBase,
                             @JsonProperty("defendantCostStandardBase") AssistedOrderCost defendantCostStandardBase,
                             @JsonProperty("claimantCostSummarilyBase") AssistedOrderCost claimantCostSummarilyBase,
                             @JsonProperty("defendantCostSummarilyBase") AssistedOrderCost defendantCostSummarilyBase,
                             @JsonProperty("costReservedDetails") DetailText costReservedDetails,
                             @JsonProperty("besPokeCostDetails") DetailText besPokeCostDetails) {
        this.assistedCostTypes = assistedCostTypes;
        this.claimantCostStandardBase = claimantCostStandardBase;
        this.defendantCostStandardBase = defendantCostStandardBase;
        this.claimantCostSummarilyBase = claimantCostSummarilyBase;
        this.defendantCostSummarilyBase = defendantCostSummarilyBase;
        this.costReservedDetails = costReservedDetails;
        this.besPokeCostDetails = besPokeCostDetails;
    }
}
