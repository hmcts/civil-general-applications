package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.dq.OrderMadeOnTypes;

import java.util.List;

@Setter
@Data
@Builder(toBuilder = true)
public class AssistedOrderMadeOnDetails {

    private final List<OrderMadeOnTypes> orderMadeOnOption;
    private final DetailTextWithDate orderMadeOnOwnInitiative;
    private final DetailTextWithDate orderMadeOnWithOutNotice;

    @JsonCreator
    AssistedOrderMadeOnDetails(@JsonProperty("orderMadeOnOption") List<OrderMadeOnTypes> orderMadeOnOption,
                               @JsonProperty("orderMadeOnOwnInitiative") DetailTextWithDate orderMadeOnOwnInitiative,
                               @JsonProperty("orderMadeOnWithOutNotice") DetailTextWithDate orderMadeOnWithOutNotice) {

        this.orderMadeOnOption = orderMadeOnOption;
        this.orderMadeOnOwnInitiative = orderMadeOnOwnInitiative;
        this.orderMadeOnWithOutNotice = orderMadeOnWithOutNotice;
    }
}
