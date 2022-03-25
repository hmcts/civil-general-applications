package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingDuration;
import uk.gov.hmcts.reform.civil.enums.dq.SupportRequirements;

@Setter
@Data
@Builder(toBuilder = true)
public class GAJudgesHearingListGAspec {

    private YesOrNo sameHearingPrefByAppAndResp;
    private YesOrNo sameCourtLocationPrefByAppAndResp;
    private YesOrNo sameHearingTimeEstByAppAndResp;
    private YesOrNo sameHearingSupportReqByAppAndResp;

    private GAJudicialHearingType hearingPreferencesPreferredType;
    private GAHearingDuration judicialTimeEstimate;
    private SupportRequirements judicialSupportRequirement;
    private String addlnInfoCourtStaff;

    @JsonCreator
    GAJudgesHearingListGAspec(@JsonProperty("sameHearingPrefByAppAndResp") YesOrNo sameHearingPrefByAppAndResp,
                              @JsonProperty("sameCourtLocationPrefByAppAndResp")
                                  YesOrNo sameCourtLocationPrefByAppAndResp,
                              @JsonProperty("sameHearingSupportReqByAppAndResp")
                                  YesOrNo sameHearingSupportReqByAppAndResp,
                              @JsonProperty("sameHearingTimeEstByAppAndResp") YesOrNo sameHearingTimeEstByAppAndResp,
                              @JsonProperty("hearingPreferencesPreferredType")
                                  GAJudicialHearingType hearingPreferencesPreferredType,
                              @JsonProperty("judicialTimeEstimate") GAHearingDuration judicialTimeEstimate,
                              @JsonProperty("judicialSupportRequirement")
                                  SupportRequirements judicialSupportRequirement,
                              @JsonProperty("addlnInfoCourtStaff") String addlnInfoCourtStaff) {

        this.sameHearingPrefByAppAndResp = sameHearingPrefByAppAndResp;
        this.sameCourtLocationPrefByAppAndResp = sameCourtLocationPrefByAppAndResp;
        this.sameHearingTimeEstByAppAndResp = sameHearingTimeEstByAppAndResp;
        this.sameHearingSupportReqByAppAndResp = sameHearingSupportReqByAppAndResp;

        this.hearingPreferencesPreferredType = hearingPreferencesPreferredType;
        this.judicialSupportRequirement = judicialSupportRequirement;
        this.judicialTimeEstimate = judicialTimeEstimate;
        this.addlnInfoCourtStaff = addlnInfoCourtStaff;

    }
}
