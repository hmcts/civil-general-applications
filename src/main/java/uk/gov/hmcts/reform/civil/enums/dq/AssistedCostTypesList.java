package uk.gov.hmcts.reform.civil.enums.dq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssistedCostTypesList {

    COSTS_IN_CASE("Costs in the case"),
    NO_ORDER_TO_COST("No order as to costs"),
    COSTS_RESERVED("Costs reserved"),
    DEFENDANT_COST_STANDARD_BASE("The defendant shall pay the claimant's costs of the claim to be subject "
                                     +  "to a detailed assessment on the standard basis if not agreed"),
    CLAIMANT_COST_STANDARD_BASE("The claimant shall pay the defendant's costs of the claim to be subject "
                                    + "to a detailed assessment on the standard basis if not agreed"),
    DEFENDANT_COST_SUMMARILY_BASE("The defendant shall pay the claimant's costs of the claim summarily "
                                      + "assessed in the sum of"),
    CLAIMANT_COST_SUMMARILY_BASE("The claimant shall pay the defendant's costs of the claim summarily "
                                     + "assessed in the sum of"),
    BESPOKE_COSTS_ORDER("Bespoke costs order"),

    REFUSED("refused");

    private final String displayedValue;
}
