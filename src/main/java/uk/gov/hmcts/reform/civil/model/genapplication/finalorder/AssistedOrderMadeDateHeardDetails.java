package uk.gov.hmcts.reform.civil.model.genapplication.finalorder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.dq.LengthOfHearing;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.genapplication.HearingLength;

import java.time.LocalDate;

@Setter
@Data
@Builder(toBuilder = true)
public class AssistedOrderMadeDateHeardDetails {

    private AssistedOrderDateHeard singleDateSelection;
    private AssistedOrderDateHeard dateRangeSelection;
    @JsonCreator
    AssistedOrderMadeDateHeardDetails(@JsonProperty("singleDateSelection") AssistedOrderDateHeard singleDateSelection,
                                       @JsonProperty("dateRangeSelection") AssistedOrderDateHeard dateRangeSelection) {

        this.singleDateSelection = singleDateSelection;
        this.dateRangeSelection = dateRangeSelection;
    }
}
