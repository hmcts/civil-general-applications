package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Data
@Builder(toBuilder = true)
public class AssistedOrderDateHeardDetails {

    private final LocalDate date;

    @JsonCreator
    AssistedOrderDateHeardDetails(@JsonProperty("date") LocalDate date) {
        this.date = date;
    }
}
