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
    private LocalDate judgeApproveEditOptionDate;
    private YesOrNo displayjudgeApproveEditOptionDate;
    private YesOrNo displayjudgeApproveEditOptionDoc;
    private GAJudgeOrderClaimantOrDefenseFixedList judgeApproveEditOptionDoc;

    @JsonCreator
    GAJudicialMakeAnOrder(@JsonProperty("judgeRecitalText") String judgeRecitalText,
                          @JsonProperty("makeAnOrder") GAJudgeMakeAnOrderOption makeAnOrder,
                          @JsonProperty("orderText") String orderText,
                          @JsonProperty("dismissalOrderText") String dismissalOrderText,
                          @JsonProperty("directionsText") String directionsText,
                          @JsonProperty("directionsResponseByDate") LocalDate directionsResponseByDate,
                          @JsonProperty("reasonForDecisionText") String reasonForDecisionText,
                          @JsonProperty("judgeApproveEditOptionDate") LocalDate judgeApproveEditOptionDate,
                          @JsonProperty("displayjudgeApproveEditOptionDate") YesOrNo displayjudgeApproveEditOptionDate,
                          @JsonProperty("displayjudgeApproveEditOptionDoc")
                              YesOrNo displayjudgeApproveEditOptionDoc,
                          @JsonProperty("judgeApproveEditOptionDoc")
                              GAJudgeOrderClaimantOrDefenseFixedList judgeApproveEditOptionDoc) {
        this.judgeRecitalText = judgeRecitalText;
        this.makeAnOrder = makeAnOrder;
        this.orderText = orderText;
        this.dismissalOrderText = dismissalOrderText;
        this.directionsText = directionsText;
        this.directionsResponseByDate = directionsResponseByDate;
        this.reasonForDecisionText = reasonForDecisionText;
        this.judgeApproveEditOptionDate = judgeApproveEditOptionDate;
        this.displayjudgeApproveEditOptionDate = displayjudgeApproveEditOptionDate;
        this.displayjudgeApproveEditOptionDoc = displayjudgeApproveEditOptionDoc;
        this.judgeApproveEditOptionDoc = judgeApproveEditOptionDoc;
    }
}
