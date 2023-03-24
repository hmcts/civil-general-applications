package uk.gov.hmcts.reform.civil.model.genapplication.finalorder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingDuration;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.genapplication.HearingLength;

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
                                       @JsonProperty("lengthOfNewHearing") GAHearingDuration lengthOfNewHearing,
                                       @JsonProperty("caseHearingLengthElement") HearingLength caseHearingLengthElement,
                                       @JsonProperty("alternativeHearingLocation")
                                       DynamicList alternativeHearingLocation,
                                       @JsonProperty("hearingMethods") GAJudicialHearingType hearingMethods,
                                       @JsonProperty("hearingNotesText") String hearingNotesText) {
        this.listFromDate = listFromDate;
        this.listToDate = listToDate;
        this.lengthOfNewHearing = lengthOfNewHearing;
        this.caseHearingLengthElement = caseHearingLengthElement;
        this.alternativeHearingLocation = alternativeHearingLocation;
        this.hearingMethods = hearingMethods;
        this.hearingNotesText = hearingNotesText;
    }
}
