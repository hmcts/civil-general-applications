package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;

@Data
@Builder(toBuilder = true)
public class GARespondentResponse implements MappableObject {

    private GAHearingDetails gaHearingDetails;
    private final YesOrNo hasRespondentAgreed;

    @JsonCreator
    GARespondentResponse(@JsonProperty("gaHearingDetails") GAHearingDetails gaHearingDetails,
                         @JsonProperty("hasRespondentAgreed")
                             YesOrNo hasRespondentAgreed) {
        this.gaHearingDetails = gaHearingDetails;
        this.hasRespondentAgreed = hasRespondentAgreed;
    }
}
