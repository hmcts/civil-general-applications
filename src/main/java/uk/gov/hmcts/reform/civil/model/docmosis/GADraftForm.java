package uk.gov.hmcts.reform.civil.model.docmosis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;

@Getter
@Builder(toBuilder = true)
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
    private final Boolean isSignLanguageExists;
    private final String supportRequirementLanguageInterpreter;
    private final Boolean isLanguageInterpreterExists;
    private final String supportRequirementOther;
    private final Boolean isOtherSupportExists;
    private final String name;

    private final YesOrNo resp1HasAgreed;
    private final YesOrNo gaResp1Consent;
    private final String resp1DebtorOffer;
    private final String resp1DeclineReason;
    private final YesOrNo resp1HearingYesOrNo;
    private final String resp1Hearingdate;
    private final String resp1HearingPreferredType;
    private final String resp1ReasonForPreferredType;
    private final String resp1PreferredLocation;
    private final String resp1PreferredTelephone;
    private final String resp1PreferredEmail;
    private final YesOrNo resp1UnavailableTrialRequired;
    private final String resp1UnavailableTrialDateFrom;
    private final String resp1UnavailableTrialDateTo;
    private final YesOrNo resp1VulnerableQuestions;
    private final String resp1SupportRequirement;
    private final String resp1SignLanguage;
    private final String resp1LanguageInterpreter;
    private final String resp1Other;
    private final YesOrNo isOneVTwoApp;
    private final YesOrNo isConsentOrderApp;
    private final YesOrNo isVaryJudgmentApp;
    private final Boolean isResp1SignLanguageExists;
    private final Boolean isResp1LanguageInterpreterExists;
    private final Boolean isResp1OtherSupportExists;

    private final YesOrNo resp2HasAgreed;
    private final YesOrNo gaResp2Consent;
    private final String resp2DebtorOffer;
    private final YesOrNo resp2HearingYesOrNo;
    private final String resp2DeclineReason;
    private final String resp2Hearingdate;
    private final String resp2HearingPreferredType;
    private final String resp2ReasonForPreferredType;
    private final String resp2PreferredLocation;
    private final String resp2PreferredTelephone;
    private final String resp2PreferredEmail;
    private final YesOrNo resp2UnavailableTrialRequired;
    private final String resp2UnavailableTrialDateFrom;
    private final String resp2UnavailableTrialDateTo;
    private final YesOrNo resp2VulnerableQuestions;
    private final String resp2SupportRequirement;
    private final String resp2SignLanguage;
    private final String resp2LanguageInterpreter;
    private final String resp2Other;
    private final Boolean isResp2SignLanguageExists;
    private final Boolean isResp2LanguageInterpreterExists;
    private final Boolean isResp2OtherSupportExists;
    private final Boolean isCasePastDueDate;
}
