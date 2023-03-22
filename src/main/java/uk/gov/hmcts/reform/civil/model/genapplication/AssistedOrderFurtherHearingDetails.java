package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingDuration;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;

import java.time.LocalDate;

@Setter
@Data
@Builder(toBuilder = true)
public class AssistedOrderFurtherHearingDetails {

    private LocalDate listFromDate;
    private LocalDate listToDate;
    private GAHearingDuration lengthOfNewHearing;
    private final HearingLength caseHearingLengthElement;
    private DynamicList alternativeHearingLocation;
    private GAJudicialHearingType hearingMethods;
    private String hearingNotesText;

    @JsonCreator
    AssistedOrderFurtherHearingDetails(@JsonProperty("listFromDate") LocalDate listFromDate,
                                       @JsonProperty("listToDate") LocalDate listToDate,
                                       @JsonProperty("hearingDate") GAHearingDuration lengthOfNewHearing,
                                       @JsonProperty("hearingTimeHourMinute") HearingLength caseHearingLengthElement,
                                       @JsonProperty("channel") DynamicList alternativeHearingLocation,
                                       @JsonProperty("hearingDuration") GAJudicialHearingType hearingMethods,
                                       @JsonProperty("hearingDurationOther") String hearingNotesText) {
        this.listFromDate = listFromDate;
        this.listToDate = listToDate;
        this.lengthOfNewHearing = lengthOfNewHearing;
        this.caseHearingLengthElement = caseHearingLengthElement;
        this.alternativeHearingLocation = alternativeHearingLocation;
        this.hearingMethods = hearingMethods;
        this.hearingNotesText = hearingNotesText;
    }
}
