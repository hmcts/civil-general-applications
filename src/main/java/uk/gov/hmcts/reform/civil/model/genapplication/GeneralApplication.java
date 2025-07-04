package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.ccd.model.SolicitorDetails;
import uk.gov.hmcts.reform.civil.enums.CaseCategory;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseLink;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.IdamUserDetails;
import uk.gov.hmcts.reform.civil.model.citizenui.CertOfSC;
import uk.gov.hmcts.reform.civil.model.citizenui.HelpWithFees;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.documents.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
public class GeneralApplication implements MappableObject {

    private GAApplicationType generalAppType;
    private GARespondentOrderAgreement generalAppRespondentAgreement;
    private final YesOrNo generalAppConsentOrder;
    private final BusinessProcess businessProcess;
    private final GAPbaDetails generalAppPBADetails;
    private YesOrNo generalAppAskForCosts;
    private String generalAppDetailsOfOrder;
    private List<Element<String>> generalAppDetailsOfOrderColl;
    private String generalAppReasonsOfOrder;
    private List<Element<String>> generalAppReasonsOfOrderColl;
    private GAInformOtherParty generalAppInformOtherParty;
    private GAUrgencyRequirement generalAppUrgencyRequirement;
    private GAStatementOfTruth generalAppStatementOfTruth;
    private GAHearingDetails generalAppHearingDetails;
    private GASolicitorDetailsGAspec generalAppApplnSolicitor;
    private List<Element<GASolicitorDetailsGAspec>> generalAppRespondentSolicitors;
    private List<Element<GASolicitorDetailsGAspec>> generalAppApplicantAddlSolicitors;
    private List<Element<Document>> generalAppEvidenceDocument;
    private LocalDateTime generalAppDateDeadline;
    private YesOrNo isMultiParty;
    private YesOrNo isDocumentVisible;
    private YesOrNo parentClaimantIsApplicant;
    private String gaApplicantDisplayName;
    private CaseLink caseLink;
    private GeneralAppParentCaseLink generalAppParentCaseLink;
    private IdamUserDetails civilServiceUserRoles;
    private LocalDateTime generalAppSubmittedDateGAspec;
    private final List<Element<SolicitorDetails>> applicantSolicitors;
    private final List<Element<SolicitorDetails>> defendantSolicitors;
    private final List<Element<GARespondentResponse>> respondentsResponses;
    private String applicantPartyName;
    private String claimant1PartyName;
    private String claimant2PartyName;
    private String defendant1PartyName;
    private String defendant2PartyName;
    private final String litigiousPartyID;
    private String generalAppSuperClaimType;
    private final GACaseLocation caseManagementLocation;
    private YesOrNo isCcmccLocation;
    private GACaseManagementCategory caseManagementCategory;
    private String locationName;
    private CaseCategory caseAccessCategory;
    private YesOrNo generalAppVaryJudgementType;
    private Document generalAppN245FormUpload;
    private GAHearingDateGAspec generalAppHearingDate;

    // GA for LIP
    private final YesOrNo isGaApplicantLip;
    private final YesOrNo isGaRespondentOneLip;
    private final YesOrNo isGaRespondentTwoLip;
    private HelpWithFees generalAppHelpWithFees;
    private final CertOfSC certOfSC;
    //Case name for manage case
    private String caseNameGaInternal;
    //WA claim track description
    private final String gaWaTrackLabel;
    private final String emailPartyReference;
    //dates
    private final LocalDateTime mainCaseSubmittedDate;

    @JsonCreator
    GeneralApplication(@JsonProperty("generalAppType") GAApplicationType generalAppType,
                       @JsonProperty("generalAppRespondentAgreement")
                           GARespondentOrderAgreement generalAppRespondentAgreement,
                       @JsonProperty("generalAppConsentOrder")
                       YesOrNo generalAppConsentOrder,
                       @JsonProperty("businessProcess") BusinessProcess businessProcess,
                       @JsonProperty("generalAppPBADetails") GAPbaDetails generalAppPBADetails,
                       @JsonProperty("generalAppAskForCosts") YesOrNo generalAppAskForCosts,
                       @JsonProperty("generalAppDetailsOfOrder") String generalAppDetailsOfOrder,
                       @JsonProperty("generalAppDetailsOfOrderColl") List<Element<String>> generalAppDetailsOfOrderColl,
                       @JsonProperty("generalAppReasonsOfOrder") String generalAppReasonsOfOrder,
                       @JsonProperty("generalAppReasonsOfOrderColl") List<Element<String>> generalAppReasonsOfOrderColl,
                       @JsonProperty("generalAppInformOtherParty") GAInformOtherParty generalAppInformOtherParty,
                       @JsonProperty("generalAppUrgencyRequirement") GAUrgencyRequirement generalAppUrgencyRequirement,
                       @JsonProperty("generalAppStatementOfTruth") GAStatementOfTruth generalAppStatementOfTruth,
                       @JsonProperty("generalAppHearingDetails") GAHearingDetails generalAppHearingDetails,
                       @JsonProperty("generalAppApplnSolicitor") GASolicitorDetailsGAspec generalAppApplnSolicitor,
                       @JsonProperty("generalAppRespondentSolicitors") List<Element<GASolicitorDetailsGAspec>>
                           generalAppRespondentSolicitors,
                       @JsonProperty("generalAppApplicantAddlSolicitors") List<Element<GASolicitorDetailsGAspec>>
                           generalAppApplicantAddlSolicitors,
                       @JsonProperty("generalAppEvidenceDocument") List<Element<Document>> generalAppEvidenceDocument,
                       @JsonProperty("generalAppDateDeadline") LocalDateTime generalAppDateDeadline,
                       @JsonProperty("isMultiParty") YesOrNo isMultiParty,
                       @JsonProperty("isDocumentVisible") YesOrNo isDocumentVisible,
                       @JsonProperty("parentClaimantIsApplicant") YesOrNo parentClaimantIsApplicant,
                       @JsonProperty("gaApplicantDisplayName") String gaApplicantDisplayName,
                       @JsonProperty("caseLink") CaseLink caseLink,
                       @JsonProperty("generalAppParentCaseLink") GeneralAppParentCaseLink generalAppParentCaseLink,
                       @JsonProperty("civilServiceUserRoles") IdamUserDetails civilServiceUserRoles,
                       @JsonProperty("generalAppSubmittedDateGAspec") LocalDateTime generalAppSubmittedDateGAspec,
                       @JsonProperty("applicantSolicitors") List<Element<SolicitorDetails>> applicantSolicitors,
                       @JsonProperty("defendantSolicitors") List<Element<SolicitorDetails>> defendantSolicitors,
                       @JsonProperty("respondentsResponses") List<Element<GARespondentResponse>> respondentsResponses,
                       @JsonProperty("applicantPartyName") String applicantPartyName,
                       @JsonProperty("claimant1PartyName") String claimant1PartyName,
                       @JsonProperty("claimant2PartyName") String claimant2PartyName,
                       @JsonProperty("defendant1PartyName") String defendant1PartyName,
                       @JsonProperty("defendant2PartyName") String defendant2PartyName,
                       @JsonProperty("litigiousPartyID") String litigiousPartyID,
                       @JsonProperty("generalAppSuperClaimType") String generalAppSuperClaimType,
                       @JsonProperty("caseManagementLocation") GACaseLocation caseManagementLocation,
                       @JsonProperty("isCcmccLocation") YesOrNo isCcmccLocation,
                       @JsonProperty("caseManagementCategory") GACaseManagementCategory caseManagementCategory,
                       @JsonProperty("locationName") String locationName,
                       @JsonProperty("CaseAccessCategory") CaseCategory caseAccessCategory,
                       @JsonProperty("generalAppVaryJudgementType") YesOrNo generalAppVaryJudgementType,
                       @JsonProperty("generalAppN245FormUpload") Document generalAppN245FormUpload,
                       @JsonProperty("generalAppHearingDate") GAHearingDateGAspec generalAppHearingDate,
                       @JsonProperty("isGaApplicantLip") YesOrNo isGaApplicantLip,
                       @JsonProperty("isGaRespondentOneLip") YesOrNo isGaRespondentOneLip,
                       @JsonProperty("isGaRespondentTwoLip") YesOrNo isGaRespondentTwoLip,
                       @JsonProperty("generalAppHelpWithFees") HelpWithFees generalAppHelpWithFees,
                       @JsonProperty("certOfSC") CertOfSC certOfSC,
                       @JsonProperty("caseNameGaInternal") String caseNameGaInternal,
                       @JsonProperty("gaWaTrackLabel") String gaWaTrackLabel,
                       @JsonProperty("emailPartyReference") String emailPartyReference,
                       @JsonProperty("mainCaseSubmittedDate") LocalDateTime mainCaseSubmittedDate) {
        this.generalAppType = generalAppType;
        this.generalAppRespondentAgreement = generalAppRespondentAgreement;
        this.generalAppConsentOrder = generalAppConsentOrder;
        this.businessProcess = businessProcess;
        this.generalAppPBADetails = generalAppPBADetails;
        this.generalAppAskForCosts = generalAppAskForCosts;
        this.generalAppDetailsOfOrder = generalAppDetailsOfOrder;
        this.generalAppDetailsOfOrderColl = generalAppDetailsOfOrderColl;
        this.generalAppReasonsOfOrder = generalAppReasonsOfOrder;
        this.generalAppReasonsOfOrderColl = generalAppReasonsOfOrderColl;
        this.generalAppInformOtherParty = generalAppInformOtherParty;
        this.generalAppUrgencyRequirement = generalAppUrgencyRequirement;
        this.generalAppStatementOfTruth = generalAppStatementOfTruth;
        this.generalAppHearingDetails = generalAppHearingDetails;
        this.generalAppApplnSolicitor = generalAppApplnSolicitor;
        this.generalAppRespondentSolicitors = generalAppRespondentSolicitors;
        this.generalAppApplicantAddlSolicitors = generalAppApplicantAddlSolicitors;
        this.generalAppEvidenceDocument = generalAppEvidenceDocument;
        this.generalAppDateDeadline = generalAppDateDeadline;
        this.isMultiParty = isMultiParty;
        this.isDocumentVisible = isDocumentVisible;
        this.parentClaimantIsApplicant = parentClaimantIsApplicant;
        this.gaApplicantDisplayName = gaApplicantDisplayName;
        this.caseLink = caseLink;
        this.generalAppParentCaseLink = generalAppParentCaseLink;
        this.civilServiceUserRoles = civilServiceUserRoles;
        this.generalAppSubmittedDateGAspec = generalAppSubmittedDateGAspec;
        this.applicantSolicitors = applicantSolicitors;
        this.defendantSolicitors = defendantSolicitors;
        this.respondentsResponses = respondentsResponses;
        this.applicantPartyName = applicantPartyName;
        this.claimant1PartyName = claimant1PartyName;
        this.claimant2PartyName = claimant2PartyName;
        this.defendant1PartyName = defendant1PartyName;
        this.defendant2PartyName = defendant2PartyName;
        this.litigiousPartyID = litigiousPartyID;
        this.generalAppSuperClaimType = generalAppSuperClaimType;
        this.caseManagementLocation = caseManagementLocation;
        this.isCcmccLocation = isCcmccLocation;
        this.caseManagementCategory = caseManagementCategory;
        this.locationName = locationName;
        this.caseAccessCategory = caseAccessCategory;
        this.generalAppVaryJudgementType = generalAppVaryJudgementType;
        this.generalAppN245FormUpload = generalAppN245FormUpload;
        this.generalAppHearingDate = generalAppHearingDate;
        this.isGaApplicantLip = isGaApplicantLip;
        this.isGaRespondentOneLip = isGaRespondentOneLip;
        this.isGaRespondentTwoLip = isGaRespondentTwoLip;
        this.generalAppHelpWithFees = generalAppHelpWithFees;
        this.certOfSC = certOfSC;
        this.caseNameGaInternal = caseNameGaInternal;
        this.gaWaTrackLabel = gaWaTrackLabel;
        this.emailPartyReference = emailPartyReference;
        this.mainCaseSubmittedDate = mainCaseSubmittedDate;
    }

    @JsonIgnore
    public void addCaseLink(CaseLink caseLink) {
        this.caseLink = caseLink;
    }
}
