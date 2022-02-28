package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseLink;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.IdamUserDetails;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.documents.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
public class GeneralApplication implements MappableObject {

    private final GAApplicationType generalAppType;
    private final GARespondentOrderAgreement generalAppRespondentAgreement;
    private final BusinessProcess businessProcess;
    private final IdamUserDetails applicantSolicitor1UserDetails;
    private final OrganisationPolicy applicant1OrganisationPolicy;
    private final OrganisationPolicy respondent1OrganisationPolicy;
    private final String respondentSolicitor1EmailAddress;
    private final GAPbaDetails generalAppPBADetails;
    private final String generalAppDetailsOfOrder;
    private final String generalAppReasonsOfOrder;
    private final GAInformOtherParty generalAppInformOtherParty;
    private final GAUrgencyRequirement generalAppUrgencyRequirement;
    private final GAStatementOfTruth generalAppStatementOfTruth;
    private final GAHearingDetails generalAppHearingDetails;
    private final List<Element<Document>> generalAppEvidenceDocument;
    private final String generalAppDeadlineNotification;
    private final YesOrNo isMultiParty;
    private final YesOrNo isPCClaimantMakingApplication;
    private CaseLink caseLink;
    private GeneralAppParentCaseLink generalAppParentCaseLink;
    private LocalDateTime generalAppSubmittedDateGAspec;

    @JsonCreator
    GeneralApplication(@JsonProperty("generalAppType") GAApplicationType generalAppType,
                       @JsonProperty("generalAppRespondentAgreement")
                           GARespondentOrderAgreement generalAppRespondentAgreement,
                       @JsonProperty("businessProcess") BusinessProcess businessProcess,
                       @JsonProperty("applicantSolicitor1UserDetails") IdamUserDetails applicantSolicitor1UserDetails,
                       @JsonProperty("applicant1OrganisationPolicy") OrganisationPolicy applicant1OrganisationPolicy,
                       @JsonProperty("respondent1OrganisationPolicy") OrganisationPolicy respondent1OrganisationPolicy,
                       @JsonProperty("respondentSolicitor1EmailAddress") String respondentSolicitor1EmailAddress,
                       @JsonProperty("generalAppPBADetails") GAPbaDetails generalAppPBADetails,
                       @JsonProperty("generalAppDetailsOfOrder") String generalAppDetailsOfOrder,
                       @JsonProperty("generalAppReasonsOfOrder") String generalAppReasonsOfOrder,
                       @JsonProperty("generalAppInformOtherParty") GAInformOtherParty generalAppInformOtherParty,
                       @JsonProperty("generalAppUrgencyRequirement") GAUrgencyRequirement generalAppUrgencyRequirement,
                       @JsonProperty("generalAppStatementOfTruth") GAStatementOfTruth generalAppStatementOfTruth,
                       @JsonProperty("generalAppHearingDetails") GAHearingDetails generalAppHearingDetails,
                       @JsonProperty("generalAppEvidenceDocument") List<Element<Document>> generalAppEvidenceDocument,
                       @JsonProperty("generalAppDeadlineNotification") String generalAppDeadlineNotification,
                       @JsonProperty("isMultiParty") YesOrNo isMultiParty,
                       @JsonProperty("isPCClaimantMakingApplication") YesOrNo isPCClaimantMakingApplication,
                       @JsonProperty("caseLink") CaseLink caseLink,
                       @JsonProperty("generalAppParentCaseLink") GeneralAppParentCaseLink generalAppParentCaseLink,
                       @JsonProperty("generalAppSubmittedDateGAspec") LocalDateTime generalAppSubmittedDateGAspec) {
        this.generalAppType = generalAppType;
        this.generalAppRespondentAgreement = generalAppRespondentAgreement;
        this.businessProcess = businessProcess;
        this.applicantSolicitor1UserDetails = applicantSolicitor1UserDetails;
        this.applicant1OrganisationPolicy = applicant1OrganisationPolicy;
        this.respondent1OrganisationPolicy = respondent1OrganisationPolicy;
        this.respondentSolicitor1EmailAddress = respondentSolicitor1EmailAddress;
        this.generalAppPBADetails = generalAppPBADetails;
        this.generalAppDetailsOfOrder = generalAppDetailsOfOrder;
        this.generalAppReasonsOfOrder = generalAppReasonsOfOrder;
        this.generalAppInformOtherParty = generalAppInformOtherParty;
        this.generalAppUrgencyRequirement = generalAppUrgencyRequirement;
        this.generalAppStatementOfTruth = generalAppStatementOfTruth;
        this.generalAppHearingDetails = generalAppHearingDetails;
        this.generalAppEvidenceDocument = generalAppEvidenceDocument;
        this.generalAppDeadlineNotification = generalAppDeadlineNotification;
        this.isMultiParty = isMultiParty;
        this.isPCClaimantMakingApplication = isPCClaimantMakingApplication;
        this.caseLink = caseLink;
        this.generalAppParentCaseLink = generalAppParentCaseLink;
        this.generalAppSubmittedDateGAspec = generalAppSubmittedDateGAspec;
    }

    @JsonIgnore
    public void addCaseLink(CaseLink caseLink) {
        this.caseLink = caseLink;
    }
}
