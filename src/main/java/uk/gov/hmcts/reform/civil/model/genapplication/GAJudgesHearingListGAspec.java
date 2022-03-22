package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

@Setter
@Data
@Builder(toBuilder = true)
public class GAJudgesHearingListGAspec {

    private String karthickTesting;
    private String judgeHearingRecitalText;
    private String judgeHearingDirectionText;
    private String judgeHearingListText;
    private String judgeHearingCourtLocationText;
    private String judgeHearingTimeEstimateText;
    private String judgeHearingSupportReqText;

    @JsonCreator
    GAJudgesHearingListGAspec(@JsonProperty("karthickTesting") String karthickTesting,
        @JsonProperty("judgeHearingRecitalText") String judgeHearingRecitalText,
                          @JsonProperty("judgeHearingDirectionText") String judgeHearingDirectionText,
                          @JsonProperty("judgeHearingListText") String judgeHearingListText,
                          @JsonProperty("judgeHearingCourtLocationText") String judgeHearingCourtLocationText,
                          @JsonProperty("judgeHearingTimeEstimateText") String judgeHearingTimeEstimateText,
                          @JsonProperty("judgeHearingSupportReqText") String judgeHearingSupportReqText) {
        this.karthickTesting = karthickTesting;
        this.judgeHearingRecitalText = judgeHearingRecitalText;
        this.judgeHearingDirectionText = judgeHearingDirectionText;
        this.judgeHearingListText = judgeHearingListText;
        this.judgeHearingCourtLocationText = judgeHearingCourtLocationText;
        this.judgeHearingTimeEstimateText = judgeHearingTimeEstimateText;
        this.judgeHearingSupportReqText = judgeHearingSupportReqText;
    }
}
