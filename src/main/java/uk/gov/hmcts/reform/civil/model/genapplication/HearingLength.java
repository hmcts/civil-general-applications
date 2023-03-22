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

    private final String days;
    private final String hours;
    private final String minutes;

    @JsonCreator
    HearingLength(@JsonProperty("days") String days,
                  @JsonProperty("hours") String hours,
                  @JsonProperty("minutes") String minutes
    ) {

        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
    }
}
