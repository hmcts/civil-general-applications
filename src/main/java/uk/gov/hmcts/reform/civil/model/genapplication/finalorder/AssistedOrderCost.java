package uk.gov.hmcts.reform.civil.model.genapplication.finalorder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.utils.MonetaryConversions;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Data
@Builder(toBuilder = true)
public class AssistedOrderCost {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal costAmount;
    private final LocalDate costPaymentDeadLine;
    private YesOrNo isPartyCostProtection;

    @JsonCreator
    AssistedOrderCost(@JsonProperty("costAmount") BigDecimal costAmount,
                               @JsonProperty("costPaymentDeadLine") LocalDate costPaymentDeadLine,
                               @JsonProperty("isPartyCostProtection") YesOrNo isPartyCostProtection
    ) {

        this.costAmount = costAmount;
        this.costPaymentDeadLine = costPaymentDeadLine;
        this.isPartyCostProtection = isPartyCostProtection;
    }

    private BigDecimal toPounds() {
        return MonetaryConversions.penniesToPounds(this.costAmount);
    }

    public String formatCaseAmountToPounds() {
        return "£" + this.toPounds();
    }
}
