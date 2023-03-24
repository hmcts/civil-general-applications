package uk.gov.hmcts.reform.civil.model.genapplication.finalorder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import org.elasticsearch.search.DocValueFormat;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Data
@Builder(toBuilder = true)
public class AssistedOrderCost {

    private final BigDecimal costAmount;
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
}
