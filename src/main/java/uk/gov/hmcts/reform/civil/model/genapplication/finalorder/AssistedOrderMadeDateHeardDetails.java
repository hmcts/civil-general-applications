package uk.gov.hmcts.reform.civil.model.genapplication.finalorder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Data
@Builder(toBuilder = true)
public class AssistedOrderMadeDateHeardDetails {

    private final LocalDate date;

    @JsonCreator
    AssistedOrderMadeDateHeardDetails(@JsonProperty("date") LocalDate date) {
        this.date = date;
    }
}
