package uk.gov.hmcts.reform.civil.model.docmosis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;

@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class GADraftForm implements MappableObject {

    private final String claimNumber;
    private final String claimantName;
    private final String defendantName;
    private final String claimantReference;
    private final String defendantReference;
    private final String date;
    private final String applicantPartyName;
    private final YesOrNo hasAgreed;
    private final YesOrNo isWithNotice;
    private final String reasonsForWithoutNotice;
    private final YesOrNo generalAppUrgency;
    private final String urgentAppConsiderationDate;
    private final String reasonsForUrgency;
    private final String generalAppType;
    private final String generalAppDetailsOfOrder;
    private final String generalAppReasonsOfOrder;
    private final YesOrNo hearingYesorNo;
    private final String hearingDate;
    private final String hearingPreferencesPreferredType;
    private final String reasonForPreferredHearingType;
    private final String hearingPreferredLocation;
    private final String hearingDetailsTelephoneNumber;
    private final String hearingDetailsEmailId;
    private final YesOrNo unavailableTrialRequiredYesOrNo;
    private final String unavailableTrialDateFrom;
    private final String unavailableTrialDateTo;
    private final YesOrNo vulnerabilityQuestionsYesOrNo;
    private final String supportRequirement;
    private final String supportRequirementSignLanguage;
    private final String supportRequirementLanguageInterpreter;
    private final String supportRequirementOther;
    private final String name;
    private final boolean isWithNoticeApp;


}
