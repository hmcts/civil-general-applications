package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;

import java.time.LocalDate;

@Setter
@Data
@Builder(toBuilder = true)
public class AssistedOrderCost {

    private final String costAmount;
    private final LocalDate costPaymentDeadLine;
    private YesOrNo isPartyCostProtection;

    @JsonCreator
    AssistedOrderCost(@JsonProperty("costAmount") String costAmount,
                               @JsonProperty("costPaymentDeadLine") LocalDate costPaymentDeadLine,
                               @JsonProperty("isPartyCostProtection") YesOrNo isPartyCostProtection
    ) {

        this.costAmount = costAmount;
        this.costPaymentDeadLine = costPaymentDeadLine;
        this.isPartyCostProtection = isPartyCostProtection;
    }
}
