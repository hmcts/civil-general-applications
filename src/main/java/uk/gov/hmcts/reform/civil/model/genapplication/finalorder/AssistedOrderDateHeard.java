package uk.gov.hmcts.reform.civil.model.genapplication.finalorder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
public class AssistedOrderDateHeard {

    private LocalDate singleDate;
    private LocalDate dateRangeFrom;
    private LocalDate dateRangeTo;


    @JsonCreator
    AssistedOrderDateHeard (@JsonProperty("singleDateHeard") LocalDate singleDate,
                            @JsonProperty("dateRangeFrom") LocalDate dateRangeFrom,
                            @JsonProperty("dateRangeTo") LocalDate dateRangeTo) {

        this.singleDate = singleDate;
        this.dateRangeFrom = dateRangeFrom;
        this.dateRangeTo = dateRangeTo;
    }
}
