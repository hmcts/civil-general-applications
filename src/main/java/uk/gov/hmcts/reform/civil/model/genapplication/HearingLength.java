package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

@Setter
@Data
@Builder(toBuilder = true)
public class HearingLength {

    private final int days;
    private final int hours;
    private final int minutes;

    @JsonCreator
    HearingLength(@JsonProperty("days") int days,
                  @JsonProperty("hours") int hours,
                  @JsonProperty("minutes") int minutes
    ) {

        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
    }
}
