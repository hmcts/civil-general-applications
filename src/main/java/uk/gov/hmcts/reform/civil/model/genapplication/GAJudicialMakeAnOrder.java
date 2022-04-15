package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.GAJudgeOrderClaimantOrDefenseFixedList;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;

import java.time.LocalDate;

@Setter
@Data
@Builder(toBuilder = true)
public class GAJudicialMakeAnOrder {

    private String judgeRecitalText;
    private GAJudgeMakeAnOrderOption makeAnOrder;
    private String orderText;
    private String dismissalOrderText;
    private String directionsText;
    private LocalDate directionsResponseByDate;
    private String reasonForDecisionText;
    private LocalDate judgeApporveEditOptionDate;
    private YesOrNo displayJudgeApporveEditOptionDate;
    private YesOrNo displayJudgeApporveEditOptionParty;
    private GAJudgeOrderClaimantOrDefenseFixedList judgeApporveEditOptionParty;

    @JsonCreator
    GAJudicialMakeAnOrder(@JsonProperty("judgeRecitalText") String judgeRecitalText,
                          @JsonProperty("makeAnOrder") GAJudgeMakeAnOrderOption makeAnOrder,
                          @JsonProperty("orderText") String orderText,
                          @JsonProperty("dismissalOrderText") String dismissalOrderText,
                          @JsonProperty("directionsText") String directionsText,
                          @JsonProperty("directionsResponseByDate") LocalDate directionsResponseByDate,
                          @JsonProperty("reasonForDecisionText") String reasonForDecisionText,
                          @JsonProperty("judgeApporveEditOptionDate") LocalDate judgeApporveEditOptionDate,
                          @JsonProperty("displayJudgeApporveEditOptionDate") YesOrNo displayJudgeApporveEditOptionDate,
                          @JsonProperty("displayJudgeApporveEditOptionParty")
                              YesOrNo displayJudgeApporveEditOptionParty,
                          @JsonProperty("judgeApporveEditOptionParty")
                              GAJudgeOrderClaimantOrDefenseFixedList judgeApporveEditOptionParty) {
        this.judgeRecitalText = judgeRecitalText;
        this.makeAnOrder = makeAnOrder;
        this.orderText = orderText;
        this.dismissalOrderText = dismissalOrderText;
        this.directionsText = directionsText;
        this.directionsResponseByDate = directionsResponseByDate;
        this.reasonForDecisionText = reasonForDecisionText;
        this.judgeApporveEditOptionDate = judgeApporveEditOptionDate;
        this.displayJudgeApporveEditOptionDate = displayJudgeApporveEditOptionDate;
        this.displayJudgeApporveEditOptionParty = displayJudgeApporveEditOptionParty;
        this.judgeApporveEditOptionParty = judgeApporveEditOptionParty;
    }
}
