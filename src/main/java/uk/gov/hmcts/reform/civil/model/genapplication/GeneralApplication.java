package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.ccd.model.SolicitorDetails;
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
    private final GAPbaDetails generalAppPBADetails;
    private final String generalAppDetailsOfOrder;
    private final String generalAppReasonsOfOrder;
    private final GAInformOtherParty generalAppInformOtherParty;
    private final GAUrgencyRequirement generalAppUrgencyRequirement;
    private final GAStatementOfTruth generalAppStatementOfTruth;
    private final GAHearingDetails generalAppHearingDetails;
    private final GASolicitorDetailsGAspec generalAppApplnSolicitor;
    private final List<Element<GASolicitorDetailsGAspec>> generalAppRespondentSolicitors;
    private final List<Element<Document>> generalAppEvidenceDocument;
    private final LocalDateTime generalAppDateDeadline;
    private final YesOrNo isMultiParty;
    private final YesOrNo parentClaimantIsApplicant;
    private CaseLink caseLink;
    private GeneralAppParentCaseLink generalAppParentCaseLink;
    private final IdamUserDetails civilServiceUserRoles;
    private LocalDateTime generalAppSubmittedDateGAspec;
    private final List<Element<SolicitorDetails>> applicantSolicitors;
    private final List<Element<SolicitorDetails>> defendantSolicitors;
    private final List<Element<GARespondentResponse>> respondentsResponses;
    private final String applicantPartyName;
    private final String claimant1PartyName;
    private final String claimant2PartyName;
    private final String defendant1PartyName;
    private final String defendant2PartyName;

    @JsonCreator
    GeneralApplication(@JsonProperty("generalAppType") GAApplicationType generalAppType,
                       @JsonProperty("generalAppRespondentAgreement")
                           GARespondentOrderAgreement generalAppRespondentAgreement,
                       @JsonProperty("businessProcess") BusinessProcess businessProcess,
                       @JsonProperty("generalAppPBADetails") GAPbaDetails generalAppPBADetails,
                       @JsonProperty("generalAppDetailsOfOrder") String generalAppDetailsOfOrder,
                       @JsonProperty("generalAppReasonsOfOrder") String generalAppReasonsOfOrder,
                       @JsonProperty("generalAppInformOtherParty") GAInformOtherParty generalAppInformOtherParty,
                       @JsonProperty("generalAppUrgencyRequirement") GAUrgencyRequirement generalAppUrgencyRequirement,
                       @JsonProperty("generalAppStatementOfTruth") GAStatementOfTruth generalAppStatementOfTruth,
                       @JsonProperty("generalAppHearingDetails") GAHearingDetails generalAppHearingDetails,
                       @JsonProperty("generalAppApplnSolicitor") GASolicitorDetailsGAspec generalAppApplnSolicitor,
                       @JsonProperty("generalAppRespondentSolicitors") List<Element<GASolicitorDetailsGAspec>>
                           generalAppRespondentSolicitors,
                       @JsonProperty("generalAppEvidenceDocument") List<Element<Document>> generalAppEvidenceDocument,
                       @JsonProperty("generalAppDateDeadline") LocalDateTime generalAppDateDeadline,
                       @JsonProperty("isMultiParty") YesOrNo isMultiParty,
                       @JsonProperty("parentClaimantIsApplicant") YesOrNo parentClaimantIsApplicant,
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
                       @JsonProperty("defendant2PartyName") String defendant2PartyName) {
        this.generalAppType = generalAppType;
        this.generalAppRespondentAgreement = generalAppRespondentAgreement;
        this.businessProcess = businessProcess;
        this.generalAppPBADetails = generalAppPBADetails;
        this.generalAppDetailsOfOrder = generalAppDetailsOfOrder;
        this.generalAppReasonsOfOrder = generalAppReasonsOfOrder;
        this.generalAppInformOtherParty = generalAppInformOtherParty;
        this.generalAppUrgencyRequirement = generalAppUrgencyRequirement;
        this.generalAppStatementOfTruth = generalAppStatementOfTruth;
        this.generalAppHearingDetails = generalAppHearingDetails;
        this.generalAppApplnSolicitor = generalAppApplnSolicitor;
        this.generalAppRespondentSolicitors = generalAppRespondentSolicitors;
        this.generalAppEvidenceDocument = generalAppEvidenceDocument;
        this.generalAppDateDeadline = generalAppDateDeadline;
        this.isMultiParty = isMultiParty;
        this.parentClaimantIsApplicant = parentClaimantIsApplicant;
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
    }

    @JsonIgnore
    public void addCaseLink(CaseLink caseLink) {
        this.caseLink = caseLink;
    }
}
