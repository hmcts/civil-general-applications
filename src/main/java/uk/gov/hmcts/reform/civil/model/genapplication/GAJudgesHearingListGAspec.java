package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingDuration;
import uk.gov.hmcts.reform.civil.enums.dq.SupportRequirements;

import java.util.List;

@Setter
@Data
@Builder(toBuilder = true)
public class GAJudgesHearingListGAspec {

    private GAJudicialHearingType hearingPreferencesPreferredType;
    private GAHearingDuration judicialTimeEstimate;
    private List<SupportRequirements> judicialSupportRequirement;
    private String judgeSignLanguage;
    private String judgeLanguageInterpreter;
    private String judgeOtherSupport;
    private String addlnInfoCourtStaff;

    private String judgeHearingTimeEstimateText1;
    private String judgeHearingCourtLocationText1;
    private String hearingPreferencesPreferredTypeLabel1;
    private String judgeHearingSupportReqText1;

    @JsonCreator
    GAJudgesHearingListGAspec(@JsonProperty("hearingPreferencesPreferredType")
                                  GAJudicialHearingType hearingPreferencesPreferredType,
                              @JsonProperty("judicialTimeEstimate") GAHearingDuration judicialTimeEstimate,
                              @JsonProperty("judicialSupportRequirement")
                                  List<SupportRequirements> judicialSupportRequirement,
                              @JsonProperty("judgeSignLanguage") String judgeSignLanguage,
                              @JsonProperty("judgeLanguageInterpreter") String judgeLanguageInterpreter,
                              @JsonProperty("judgeOtherSupport") String judgeOtherSupport,
                              @JsonProperty("addlnInfoCourtStaff") String addlnInfoCourtStaff,
                              @JsonProperty("judgeHearingTimeEstimateText1") String judgeHearingTimeEstimateText1,
                              @JsonProperty("judgeHearingCourtLocationText1") String judgeHearingCourtLocationText1,
                              @JsonProperty("hearingPreferencesPreferredTypeLabel1")
                                  String hearingPreferencesPreferredTypeLabel1,
                              @JsonProperty("judgeHearingSupportReqText1") String judgeHearingSupportReqText1) {

        this.hearingPreferencesPreferredType = hearingPreferencesPreferredType;
        this.judicialSupportRequirement = judicialSupportRequirement;
        this.judicialTimeEstimate = judicialTimeEstimate;
        this.addlnInfoCourtStaff = addlnInfoCourtStaff;
        this.judgeHearingTimeEstimateText1 = judgeHearingTimeEstimateText1;
        this.judgeHearingCourtLocationText1 = judgeHearingCourtLocationText1;
        this.hearingPreferencesPreferredTypeLabel1 = hearingPreferencesPreferredTypeLabel1;
        this.judgeHearingSupportReqText1 = judgeHearingSupportReqText1;

    }
}
