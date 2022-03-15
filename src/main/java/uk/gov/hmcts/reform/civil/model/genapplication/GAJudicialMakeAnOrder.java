package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;

@Setter
@Data
@Builder(toBuilder = true)
public class GAJudicialMakeAnOrder {

    private String judgeRecitalText;
    private GAJudgeMakeAnOrderOption makeAnOrder;
    private String orderText;
    private String dismissalOrderText;
    private String reasonForDecisionText;

    @JsonCreator
    GAJudicialMakeAnOrder(@JsonProperty("judgeRecitalText") String judgeRecitalText,
                          @JsonProperty("makeAnOrder") GAJudgeMakeAnOrderOption makeAnOrder,
                          @JsonProperty("orderText") String orderText,
                          @JsonProperty("dismissalOrderText") String dismissalOrderText,
                          @JsonProperty("reasonForDecisionText") String reasonForDecisionText) {
        this.judgeRecitalText = judgeRecitalText;
        this.makeAnOrder = makeAnOrder;
        this.orderText = orderText;
        this.dismissalOrderText = dismissalOrderText;
        this.reasonForDecisionText = reasonForDecisionText;
    }
}
