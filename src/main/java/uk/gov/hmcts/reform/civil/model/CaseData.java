package uk.gov.hmcts.reform.civil.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.ccd.model.SolicitorDetails;
import uk.gov.hmcts.reform.civil.enums.CaseCategory;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.FinalOrderSelection;
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
import uk.gov.hmcts.reform.civil.enums.dq.OrderOnCourts;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.genapplication.FreeFormOrderValues;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GACaseLocation;
import uk.gov.hmcts.reform.civil.model.genapplication.GACaseManagementCategory;
import uk.gov.hmcts.reform.civil.model.genapplication.GADetailsRespondentSol;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDateGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingNoticeApplication;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingNoticeDetail;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.model.genapplication.GAMakeApplicationAvailableCheck;
import uk.gov.hmcts.reform.civil.model.genapplication.GAOrderCourtOwnInitiativeGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAOrderWithoutNoticeGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAReferToJudgeGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAReferToLegalAdvisorGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentResponse;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAStatementOfTruth;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.FINISHED;

@Data
@Builder(toBuilder = true)
public class CaseData implements MappableObject {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private final Long ccdCaseReference;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private final CaseState ccdState;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private final LocalDateTime createdDate;
    private final String detailsOfClaim;
    private final YesOrNo addApplicant2;
    private final GAApplicationType generalAppType;
    private final GARespondentOrderAgreement generalAppRespondentAgreement;
    private final GAPbaDetails generalAppPBADetails;
    private final String generalAppDetailsOfOrder;
    private final String generalAppReasonsOfOrder;
    private final String legacyCaseReference;
    private final LocalDateTime notificationDeadline;
    private final LocalDate submittedOn;
    private final LocalDateTime generalAppNotificationDeadlineDate;
    private final GAInformOtherParty generalAppInformOtherParty;
    private final GAUrgencyRequirement generalAppUrgencyRequirement;
    private final GAStatementOfTruth generalAppStatementOfTruth;
    private final GAHearingDetails generalAppHearingDetails;
    private final GASolicitorDetailsGAspec generalAppApplnSolicitor;
    private final List<Element<GASolicitorDetailsGAspec>> generalAppRespondentSolicitors;
    private final GAHearingDetails hearingDetailsResp;
    private final GARespondentRepresentative generalAppRespondent1Representative;
    private final YesOrNo isMultiParty;
    private final YesOrNo parentClaimantIsApplicant;
    private final CaseLink caseLink;
    private GeneralAppParentCaseLink generalAppParentCaseLink;
    private final IdamUserDetails applicantSolicitor1UserDetails;
    private final IdamUserDetails civilServiceUserRoles;
    private final List<Element<Document>> generalAppEvidenceDocument;
    private final List<Element<GeneralApplication>> generalApplications;
    private final List<Element<GeneralApplicationsDetails>> claimantGaAppDetails;
    private final List<Element<GeneralApplicationsDetails>> gaDetailsMasterCollection;
    private final List<Element<GADetailsRespondentSol>> respondentSolGaAppDetails;
    private final List<Element<GADetailsRespondentSol>> respondentSolTwoGaAppDetails;
    private final GAJudicialDecision judicialDecision;
    private final List<Element<SolicitorDetails>> applicantSolicitors;
    private final List<Element<SolicitorDetails>> defendantSolicitors;
    private final List<Element<GARespondentResponse>> respondentsResponses;
    private final YesOrNo applicationIsCloaked;
    private final YesOrNo applicationIsUncloakedOnce;
    private final GAJudicialMakeAnOrder judicialDecisionMakeOrder;
    private final Document judicialMakeOrderDocPreview;
    private final Document judicialListHearingDocPreview;
    private final Document judicialWrittenRepDocPreview;
    private final Document judicialRequestMoreInfoDocPreview;
    private final GAJudicialRequestMoreInfo judicialDecisionRequestMoreInfo;
    private final GAJudicialWrittenRepresentations judicialDecisionMakeAnOrderForWrittenRepresentations;
    private final String judgeRecitalText;
    private final String directionInRelationToHearingText;
    private final GAJudgesHearingListGAspec judicialListForHearing;
    private final String applicantPartyName;
    private final String claimant1PartyName;
    private final String claimant2PartyName;
    private final String defendant1PartyName;
    private final String defendant2PartyName;
    private final GACaseLocation caseManagementLocation;
    private final YesOrNo isCcmccLocation;
    private final GACaseManagementCategory caseManagementCategory;
    private final String judicialGeneralHearingOrderRecital;
    private final String judicialGOHearingDirections;
    private final String judicialHearingGeneralOrderHearingText;
    private final String judicialGeneralOrderHearingEstimationTimeText;
    private final String judicialHearingGOHearingReqText;
    private final String judicialSequentialDateText;
    private final String judicialApplicanSequentialDateText;
    private final String judicialConcurrentDateText;
    private final List<Element<Document>> generalAppWrittenRepUpload;
    private final List<Element<Document>> gaWrittenRepDocList;
    private final List<Element<Document>> generalAppDirOrderUpload;
    private final List<Element<Document>> gaDirectionDocList;
    private final List<Element<Document>> generalAppAddlnInfoUpload;
    private final List<Element<Document>> gaAddlnInfoList;
    private final String gaRespondentDetails;
    private final LocalDate issueDate;
    private final String generalAppSuperClaimType;
    private final GAMakeApplicationAvailableCheck makeAppVisibleToRespondents;
    private final String respondentSolicitor1EmailAddress;
    private final String respondentSolicitor2EmailAddress;
    private final OrganisationPolicy applicant1OrganisationPolicy;
    private final OrganisationPolicy respondent1OrganisationPolicy;
    private final OrganisationPolicy respondent2OrganisationPolicy;
    private final YesOrNo respondent2SameLegalRepresentative;
    private final GAReferToJudgeGAspec referToJudge;
    private final GAReferToLegalAdvisorGAspec referToLegalAdvisor;
    private final LocalDateTime applicationClosedDate;
    private final LocalDateTime applicationTakenOfflineDate;
    private final String locationName;
    private final GAByCourtsInitiativeGAspec judicialByCourtsInitiativeListForHearing;
    private final GAByCourtsInitiativeGAspec judicialByCourtsInitiativeForWrittenRep;
    private final YesOrNo showRequestInfoPreviewDoc;
    private GAHearingNoticeApplication gaHearingNoticeApplication;
    private GAHearingNoticeDetail gaHearingNoticeDetail;
    private String gaHearingNoticeInformation;
    private final String migrationId;
    private final String caseNameHmctsInternal;
    private final FinalOrderSelection finalOrderSelection;
    private final String freeFormRecitalText;
    private final String freeFormRecordedText;
    private final String freeFormOrderedText;
    private final OrderOnCourts orderOnCourtsList;
    private final FreeFormOrderValues orderOnCourtInitiative;
    private final FreeFormOrderValues orderWithoutNotice;
    private final Document gaFinalOrderDocPreview;

    @JsonProperty("CaseAccessCategory")
    private final CaseCategory caseAccessCategory;
    private final YesOrNo generalAppVaryJudgementType;
    private final Document generalAppN245FormUpload;
    private final GAHearingDateGAspec generalAppHearingDate;

    //PDF Documents
    @Builder.Default
    private final List<Element<CaseDocument>> generalOrderDocument = new ArrayList<>();
    private final List<Element<CaseDocument>> generalOrderDocStaff;
    private final List<Element<CaseDocument>> generalOrderDocClaimant;
    private final List<Element<CaseDocument>> generalOrderDocRespondentSol;
    private final List<Element<CaseDocument>> generalOrderDocRespondentSolTwo;
    @Builder.Default
    private final List<Element<CaseDocument>> dismissalOrderDocument = new ArrayList<>();
    private final List<Element<CaseDocument>> dismissalOrderDocStaff;
    private final List<Element<CaseDocument>> dismissalOrderDocClaimant;
    private final List<Element<CaseDocument>> dismissalOrderDocRespondentSol;
    private final List<Element<CaseDocument>> dismissalOrderDocRespondentSolTwo;
    @Builder.Default
    private final List<Element<CaseDocument>> directionOrderDocument = new ArrayList<>();
    private final List<Element<CaseDocument>> directionOrderDocStaff;
    private final List<Element<CaseDocument>> directionOrderDocClaimant;
    private final List<Element<CaseDocument>> directionOrderDocRespondentSol;
    private final List<Element<CaseDocument>> directionOrderDocRespondentSolTwo;
    @Builder.Default
    private final List<Element<CaseDocument>> requestForInformationDocument = new ArrayList<>();
    @Builder.Default
    private final List<Element<CaseDocument>> hearingOrderDocument = new ArrayList<>();
    @Builder.Default
    private final List<Element<CaseDocument>> hearingNoticeDocument = new ArrayList<>();
    private final List<Element<CaseDocument>> hearingNoticeDocStaff;
    private final List<Element<CaseDocument>> hearingNoticeDocClaimant;
    private final List<Element<CaseDocument>> hearingNoticeDocRespondentSol;
    private final List<Element<CaseDocument>> hearingNoticeDocRespondentSolTwo;
    @Builder.Default
    private final List<Element<CaseDocument>> writtenRepSequentialDocument = new ArrayList<>();
    @Builder.Default
    private final List<Element<CaseDocument>> writtenRepConcurrentDocument = new ArrayList<>();
    private final BusinessProcess businessProcess;
    private final String respondent1OrganisationIDCopy;
    private final GAOrderCourtOwnInitiativeGAspec orderCourtOwnInitiativeListForHearing;
    private final GAOrderWithoutNoticeGAspec orderWithoutNoticeListForHearing;
    private final GAOrderCourtOwnInitiativeGAspec orderCourtOwnInitiativeForWrittenRep;
    private final GAOrderWithoutNoticeGAspec orderWithoutNoticeForWrittenRep;

    public boolean hasNoOngoingBusinessProcess() {
        return businessProcess == null
            || businessProcess.getStatus() == null
            || businessProcess.getStatus() == FINISHED;
    }

}
