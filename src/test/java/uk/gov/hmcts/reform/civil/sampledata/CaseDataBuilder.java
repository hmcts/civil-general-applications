package uk.gov.hmcts.reform.civil.sampledata;

import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.PaymentStatus;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.FinalOrderSelection;
import uk.gov.hmcts.reform.civil.enums.dq.FinalOrderShowToggle;
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingDuration;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.enums.dq.OrderOnCourts;
import uk.gov.hmcts.reform.civil.enums.hearing.HearingApplicationDetails;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.CaseLink;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.IdamUserDetails;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.FreeFormOrderValues;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApproveConsentOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GACaseLocation;
import uk.gov.hmcts.reform.civil.model.genapplication.GADetailsRespondentSol;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDateGAspec;
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
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_ADD_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_APPLICATION_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.LISTING_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PENDING_APPLICATION_ISSUED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.FREE_FORM_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.LIST_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_AN_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.ADJOURN_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.EXTEND_TIME;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

public class CaseDataBuilder {

    public static final String LEGACY_CASE_REFERENCE = "000DC001";
    public static final Long CASE_ID = 1594901956117591L;
    public static final LocalDateTime SUBMITTED_DATE_TIME = LocalDateTime.now();
    public static final LocalDateTime RESPONSE_DEADLINE = SUBMITTED_DATE_TIME.toLocalDate().plusDays(14)
        .atTime(23, 59, 59);
    public static final LocalDateTime APPLICANT_RESPONSE_DEADLINE = SUBMITTED_DATE_TIME.plusDays(120);
    public static final LocalDate APPLICATION_SUBMITTED_DATE = now();
    public static final LocalDateTime DEADLINE = LocalDate.now().atStartOfDay().plusDays(14);
    public static final LocalDate PAST_DATE = now().minusDays(1);
    public static final LocalDateTime NOTIFICATION_DEADLINE = LocalDate.now().atStartOfDay().plusDays(1);
    public static final BigDecimal FAST_TRACK_CLAIM_AMOUNT = BigDecimal.valueOf(10000);
    public static final String CUSTOMER_REFERENCE = "12345";

    private static final String JUDGES_DECISION = "MAKE_DECISION";

    private static final String HEARING_SCHEDULED = "HEARING_SCHEDULED_GA";
    private static final Fee FEE108 = Fee.builder().calculatedAmountInPence(
        BigDecimal.valueOf(10800)).code("FEE0443").version("1").build();
    private static final Fee FEE14 = Fee.builder().calculatedAmountInPence(
        BigDecimal.valueOf(1400)).code("FEE0458").version("2").build();
    private static final Fee FEE275 = Fee.builder().calculatedAmountInPence(
        BigDecimal.valueOf(27500)).code("FEE0442").version("1").build();
    public static final String STRING_CONSTANT = "this is a string";
    private static final String DUMMY_EMAIL = "hmcts.civil@gmail.com";

    private static final String JUDICIAL_REQUEST_MORE_INFO_RECITAL_TEXT = "<Title> <Name> \n"
        + "Upon reviewing the application made and upon considering the information "
        + "provided by the parties, the court requests more information from the applicant.";

    // Create Claim
    protected Long ccdCaseReference;
    protected String legacyCaseReference;
    protected LocalDateTime generalAppDeadlineNotificationDate;
    protected GAInformOtherParty gaInformOtherParty;
    protected GAUrgencyRequirement gaUrgencyRequirement;
    protected GARespondentOrderAgreement gaRespondentOrderAgreement;
    protected String respondentSolicitor1EmailAddress;
    protected String respondentSolicitor2EmailAddress;
    protected GAPbaDetails gaPbaDetails;
    protected OrganisationPolicy applicant1OrganisationPolicy;
    protected IdamUserDetails applicantSolicitor1UserDetails;
    protected OrganisationPolicy respondent1OrganisationPolicy;
    protected YesOrNo respondent2SameLegalRepresentative;
    protected OrganisationPolicy respondent2OrganisationPolicy;
    protected GAJudicialRequestMoreInfo judicialDecisionRequestMoreInfo;
    protected CaseState ccdState;
    // Claimant Response
    protected BusinessProcess businessProcess;
    protected List<Element<GeneralApplication>> generalApplications;
    protected List<Element<GeneralApplicationsDetails>> claimantGaAppDetails;
    protected List<Element<GADetailsRespondentSol>> respondentSolGaAppDetails;
    protected List<Element<GADetailsRespondentSol>> respondentSolTwoGaAppDetails;
    protected List<Element<GeneralApplicationsDetails>> gaDetailsMasterCollection;

    protected GASolicitorDetailsGAspec generalAppApplnSolicitor;
    private YesOrNo isMultiParty;
    protected YesOrNo addApplicant2;
    protected List<Element<GASolicitorDetailsGAspec>> generalAppRespondentSolicitors;
    protected GAMakeApplicationAvailableCheck makeAppVisibleToRespondents;
    //General Application
    protected LocalDate submittedOn;
    private GeneralAppParentCaseLink generalAppParentCaseLink;
    private YesOrNo parentClaimantIsApplicant;
    private static final Long CASE_REFERENCE = 111111L;
    protected GAJudicialMakeAnOrder judicialMakeAnOrder;
    protected GAApplicationType generalAppType;
    protected GAApproveConsentOrder  approveConsentOrder;

    public CaseDataBuilder legacyCaseReference(String legacyCaseReference) {
        this.legacyCaseReference = legacyCaseReference;
        return this;
    }

    public CaseDataBuilder generalAppDeadlineNotificationDate(LocalDateTime generalAppDeadlineNotificationDate) {
        this.generalAppDeadlineNotificationDate = generalAppDeadlineNotificationDate;
        return this;
    }

    public CaseDataBuilder generalApplications(List<Element<GeneralApplication>> generalApplications) {
        this.generalApplications = generalApplications;
        return this;
    }

    public CaseDataBuilder generalAppApplnSolicitor(GASolicitorDetailsGAspec generalAppApplnSolicitor) {
        this.generalAppApplnSolicitor = generalAppApplnSolicitor;
        return this;
    }

    public CaseDataBuilder isMultiParty(YesOrNo isMultiParty) {
        this.isMultiParty = isMultiParty;
        return this;
    }

    public CaseDataBuilder addApplicant2(YesOrNo addApplicant2) {
        this.addApplicant2 = addApplicant2;
        return this;
    }

    public CaseDataBuilder applicantSolicitor1UserDetails(IdamUserDetails applicantSolicitor1UserDetails) {
        this.applicantSolicitor1UserDetails = applicantSolicitor1UserDetails;
        return this;
    }

    public CaseDataBuilder generalAppRespondentSolicitors(List<Element<GASolicitorDetailsGAspec>>
                                                              generalAppRespondentSolicitors) {
        this.generalAppRespondentSolicitors = generalAppRespondentSolicitors;
        return this;
    }

    public CaseDataBuilder makeAppVisibleToRespondents(GAMakeApplicationAvailableCheck makeAppVisibleToRespondents) {
        this.makeAppVisibleToRespondents = makeAppVisibleToRespondents;
        return this;
    }

    public CaseDataBuilder ccdState(CaseState ccdState) {
        this.ccdState = ccdState;
        return this;
    }

    public CaseDataBuilder generalApplicationsDetails(List<Element<GeneralApplicationsDetails>>
                                                          generalApplicationsDetails) {
        this.claimantGaAppDetails = generalApplicationsDetails;
        return this;
    }

    public CaseDataBuilder gaDetailsRespondentSol(List<Element<GADetailsRespondentSol>>
                                                      gaDetailsRespondentSol) {
        this.respondentSolGaAppDetails = gaDetailsRespondentSol;
        return this;
    }

    public CaseDataBuilder gaDetailsRespondentSolTwo(List<Element<GADetailsRespondentSol>>
                                                      gaDetailsRespondentSolTwo) {
        this.respondentSolTwoGaAppDetails = gaDetailsRespondentSolTwo;
        return this;
    }

    public CaseDataBuilder gaDetailsMasterCollection(List<Element<GeneralApplicationsDetails>>
                                                         gaDetailsMasterCollection) {
        this.gaDetailsMasterCollection = gaDetailsMasterCollection;
        return this;
    }

    public CaseDataBuilder businessProcess(BusinessProcess businessProcess) {
        this.businessProcess = businessProcess;
        return this;
    }

    public CaseDataBuilder generalAppParentCaseLink(GeneralAppParentCaseLink generalAppParentCaseLink) {
        this.generalAppParentCaseLink = generalAppParentCaseLink;
        return this;
    }

    public CaseDataBuilder parentClaimantIsApplicant(YesOrNo parentClaimantIsApplicant) {
        this.parentClaimantIsApplicant = parentClaimantIsApplicant;
        return this;
    }

    public CaseDataBuilder ccdCaseReference(Long ccdCaseReference) {
        this.ccdCaseReference = ccdCaseReference;
        return this;
    }

    public CaseDataBuilder gaInformOtherParty(GAInformOtherParty gaInformOtherParty) {
        this.gaInformOtherParty = gaInformOtherParty;
        return this;
    }

    public CaseDataBuilder gaUrgencyRequirement(GAUrgencyRequirement gaUrgencyRequirement) {
        this.gaUrgencyRequirement = gaUrgencyRequirement;
        return this;
    }

    public CaseDataBuilder gaRespondentOrderAgreement(GARespondentOrderAgreement gaRespondentOrderAgreement) {
        this.gaRespondentOrderAgreement = gaRespondentOrderAgreement;
        return this;
    }

    public CaseDataBuilder respondentSolicitor1EmailAddress(String email) {
        this.respondentSolicitor1EmailAddress = email;
        return this;
    }

    public CaseDataBuilder respondentSolicitor2EmailAddress(String email) {
        this.respondentSolicitor2EmailAddress = email;
        return this;
    }

    public CaseDataBuilder gaPbaDetails(GAPbaDetails gaPbaDetails) {
        this.gaPbaDetails = gaPbaDetails;
        return this;
    }

    public CaseDataBuilder applicant1OrganisationPolicy(OrganisationPolicy applicant1OrganisationPolicy) {
        this.applicant1OrganisationPolicy = applicant1OrganisationPolicy;
        return this;
    }

    public CaseDataBuilder respondent1OrganisationPolicy(OrganisationPolicy respondent1OrganisationPolicy) {
        this.respondent1OrganisationPolicy = respondent1OrganisationPolicy;
        return this;
    }

    public CaseDataBuilder respondent2SameLegalRepresentative(YesOrNo respondent2SameLegalRepresentative) {
        this.respondent2SameLegalRepresentative = respondent2SameLegalRepresentative;
        return this;
    }

    public CaseDataBuilder respondent2OrganisationPolicy(OrganisationPolicy respondent2OrganisationPolicy) {
        this.respondent2OrganisationPolicy = respondent2OrganisationPolicy;
        return this;
    }

    public CaseDataBuilder judicialDecisionMakeOrder(GAJudicialMakeAnOrder judicialMakeAnOrder) {
        this.judicialMakeAnOrder = judicialMakeAnOrder;
        return this;
    }

    public CaseDataBuilder generalAppType(GAApplicationType generalAppType) {
        this.generalAppType = generalAppType;
        return this;
    }

    public CaseDataBuilder approveConsentOrder(GAApproveConsentOrder approveConsentOrder) {
        this.approveConsentOrder = approveConsentOrder;
        return this;
    }

    public static CaseDataBuilder builder() {
        return new CaseDataBuilder();
    }

    public CaseDataBuilder judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo judicialDecisionRequestMoreInfo) {
        this.judicialDecisionRequestMoreInfo = judicialDecisionRequestMoreInfo;
        return this;
    }

    public CaseDataBuilder atStateClaimDraft() {

        return this;
    }

    public CaseData build() {
        return CaseData.builder()
            .businessProcess(businessProcess)
            .ccdState(ccdState)
            .isMultiParty(isMultiParty)
            .addApplicant2(addApplicant2)
            .respondentSolTwoGaAppDetails(respondentSolTwoGaAppDetails)
            .gaDetailsMasterCollection(gaDetailsMasterCollection)
            .applicantSolicitor1UserDetails(applicantSolicitor1UserDetails)
            .applicant1OrganisationPolicy(applicant1OrganisationPolicy)
            .respondentSolicitor1EmailAddress(respondentSolicitor1EmailAddress)
            .respondentSolicitor2EmailAddress(respondentSolicitor2EmailAddress)
            .respondent1OrganisationPolicy(respondent1OrganisationPolicy)
            .respondent2OrganisationPolicy(respondent2OrganisationPolicy)
            .generalAppApplnSolicitor(generalAppApplnSolicitor)
            .judicialDecisionRequestMoreInfo(judicialDecisionRequestMoreInfo)
            .generalAppRespondentSolicitors(generalAppRespondentSolicitors)
            .ccdCaseReference(ccdCaseReference)
            .respondent2SameLegalRepresentative(respondent2SameLegalRepresentative)
            .legacyCaseReference(legacyCaseReference)
            .generalApplications(generalApplications)
            .generalAppInformOtherParty(gaInformOtherParty)
            .generalAppUrgencyRequirement(gaUrgencyRequirement)
            .generalAppRespondentAgreement(gaRespondentOrderAgreement)
            .generalAppParentCaseLink(generalAppParentCaseLink)
            .claimantGaAppDetails(claimantGaAppDetails)
            .respondentSolGaAppDetails(respondentSolGaAppDetails)
            .generalAppPBADetails(gaPbaDetails)
            .applicant1OrganisationPolicy(applicant1OrganisationPolicy)
            .generalAppNotificationDeadlineDate(generalAppDeadlineNotificationDate)
            .parentClaimantIsApplicant(parentClaimantIsApplicant)
            .makeAppVisibleToRespondents(makeAppVisibleToRespondents)
            .judicialDecisionMakeOrder(judicialMakeAnOrder)
            .generalAppType(generalAppType)
            .approveConsentOrder(approveConsentOrder)
            .build();
    }

    public CaseData buildMakePaymentsCaseData() {
        uk.gov.hmcts.reform.ccd.model.Organisation orgId = uk.gov.hmcts.reform.ccd.model.Organisation.builder()
            .organisationID("OrgId").build();

        return build().toBuilder()
            .ccdCaseReference(1644495739087775L)
            .ccdCaseReference(1644495739087775L)
            .legacyCaseReference("000DC001")
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .fee(
                        Fee.builder()
                            .code("FE203")
                            .calculatedAmountInPence(BigDecimal.valueOf(27500))
                            .version("1")
                            .build())
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .generalAppSuperClaimType("UNSPEC_CLAIM")
            .applicant1OrganisationPolicy(OrganisationPolicy.builder().organisation(orgId).build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().organisationIdentifier("OrgId").build())
            .build();
    }

    public CaseData buildPaymentFailureCaseData() {
        uk.gov.hmcts.reform.ccd.model.Organisation orgId = uk.gov.hmcts.reform.ccd.model.Organisation.builder()
            .organisationID("OrgId").build();

        return build().toBuilder()
            .ccdCaseReference(1644495739087775L)
            .ccdCaseReference(1644495739087775L)
            .legacyCaseReference("000DC001")
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YES).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                               .hasAgreed(YES).build())
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .paymentDetails(PaymentDetails.builder()
                                        .status(PaymentStatus.FAILED)
                                        .reference("RC-1658-4258-2679-9795")
                                        .customerReference(CUSTOMER_REFERENCE)
                                        .build())
                    .fee(
                        Fee.builder()
                            .code("FE203")
                            .calculatedAmountInPence(BigDecimal.valueOf(27500))
                            .version("1")
                            .build())
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .applicant1OrganisationPolicy(OrganisationPolicy.builder().organisation(orgId).build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().organisationIdentifier("OrgId").build())
            .build();
    }

    public CaseData withNoticeCaseData() {
        uk.gov.hmcts.reform.ccd.model.Organisation orgId = uk.gov.hmcts.reform.ccd.model.Organisation.builder()
            .organisationID("OrgId").build();

        return build().toBuilder()
            .ccdCaseReference(1644495739087775L)
            .ccdCaseReference(1644495739087775L)
            .legacyCaseReference("000DC001")
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YES).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                               .hasAgreed(YES).build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .paymentSuccessfulDate(LocalDateTime.of(LocalDate.of(2020, 01, 01),
                                                            LocalTime.of(12, 00, 00)))
                    .paymentDetails(PaymentDetails.builder()
                                        .status(PaymentStatus.SUCCESS)
                                        .reference("RC-1658-4258-2679-9795")
                                        .customerReference(CUSTOMER_REFERENCE)
                                        .build())
                    .fee(
                        Fee.builder()
                            .code("FE203")
                            .calculatedAmountInPence(BigDecimal.valueOf(27500))
                            .version("1")
                            .build())
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .applicant1OrganisationPolicy(OrganisationPolicy.builder().organisation(orgId).build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().organisationIdentifier("OrgId").build())
            .build();
    }

    public CaseData buildPaymentSuccessfulCaseData() {
        uk.gov.hmcts.reform.ccd.model.Organisation orgId = uk.gov.hmcts.reform.ccd.model.Organisation.builder()
            .organisationID("OrgId").build();

        return build().toBuilder()
            .ccdCaseReference(1644495739087775L)
            .ccdCaseReference(1644495739087775L)
            .legacyCaseReference("000DC001")
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .paymentSuccessfulDate(LocalDateTime.of(LocalDate.of(2020, 01, 01),
                                                            LocalTime.of(12, 00, 00)))
                    .paymentDetails(PaymentDetails.builder()
                                        .status(PaymentStatus.SUCCESS)
                                        .reference("RC-1658-4258-2679-9795")
                                        .customerReference(CUSTOMER_REFERENCE)
                                        .build())
                    .fee(
                        Fee.builder()
                            .code("FE203")
                            .calculatedAmountInPence(BigDecimal.valueOf(27500))
                            .version("1")
                            .build())
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .applicant1OrganisationPolicy(OrganisationPolicy.builder().organisation(orgId).build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().organisationIdentifier("OrgId").build())
            .build();
    }

    public CaseData buildCaseDateBaseOnGeneralApplication(GeneralApplication application) {
        return CaseData.builder()
            .ccdState(PENDING_APPLICATION_ISSUED)
            .generalAppType(application.getGeneralAppType())
            .caseLink(application.getCaseLink())
            .generalAppRespondentAgreement(application.getGeneralAppRespondentAgreement())
            .generalAppInformOtherParty(application.getGeneralAppInformOtherParty())
            .generalAppPBADetails(application.getGeneralAppPBADetails())
            .generalAppDetailsOfOrder(application.getGeneralAppDetailsOfOrder())
            .generalAppReasonsOfOrder(application.getGeneralAppReasonsOfOrder())
            .generalAppNotificationDeadlineDate(application.getGeneralAppDateDeadline())
            .generalAppUrgencyRequirement(application.getGeneralAppUrgencyRequirement())
            .generalAppStatementOfTruth(application.getGeneralAppStatementOfTruth())
            .generalAppHearingDetails(application.getGeneralAppHearingDetails())
            .generalAppEvidenceDocument(application.getGeneralAppEvidenceDocument())
            .isMultiParty(application.getIsMultiParty())
            .parentClaimantIsApplicant(application.getParentClaimantIsApplicant())
            .generalAppParentCaseLink(application.getGeneralAppParentCaseLink())
            .generalAppRespondentSolicitors(application.getGeneralAppRespondentSolicitors())
            .isCcmccLocation(application.getIsCcmccLocation())
            .caseManagementLocation(application.getCaseManagementLocation())
            .build();
    }

    public CaseData buildCaseDateBaseOnGeneralApplicationByState(GeneralApplication application, CaseState state) {
        return CaseData.builder()
            .ccdState(state)
            .generalAppType(application.getGeneralAppType())
            .caseLink(application.getCaseLink())
            .generalAppRespondentAgreement(application.getGeneralAppRespondentAgreement())
            .generalAppInformOtherParty(application.getGeneralAppInformOtherParty())
            .generalAppPBADetails(application.getGeneralAppPBADetails())
            .generalAppDetailsOfOrder(application.getGeneralAppDetailsOfOrder())
            .generalAppReasonsOfOrder(application.getGeneralAppReasonsOfOrder())
            .generalAppNotificationDeadlineDate(application.getGeneralAppDateDeadline())
            .generalAppUrgencyRequirement(application.getGeneralAppUrgencyRequirement())
            .generalAppStatementOfTruth(application.getGeneralAppStatementOfTruth())
            .generalAppHearingDetails(application.getGeneralAppHearingDetails())
            .generalAppEvidenceDocument(application.getGeneralAppEvidenceDocument())
            .isMultiParty(application.getIsMultiParty())
            .parentClaimantIsApplicant(application.getParentClaimantIsApplicant())
            .generalAppParentCaseLink(application.getGeneralAppParentCaseLink())
            .generalAppRespondentSolicitors(application.getGeneralAppRespondentSolicitors())
            .isCcmccLocation(application.getIsCcmccLocation())
            .caseManagementLocation(application.getCaseManagementLocation())
            .build();
    }

    public CaseData buildCaseDateBaseOnGaForCollection(GeneralApplication application) {
        return CaseData.builder()
            .ccdState(AWAITING_APPLICATION_PAYMENT)
            .generalAppType(application.getGeneralAppType())
            .caseLink(application.getCaseLink())
            .generalAppRespondentAgreement(application.getGeneralAppRespondentAgreement())
            .generalAppInformOtherParty(application.getGeneralAppInformOtherParty())
            .generalAppPBADetails(application.getGeneralAppPBADetails())
            .generalAppDetailsOfOrder(application.getGeneralAppDetailsOfOrder())
            .generalAppReasonsOfOrder(application.getGeneralAppReasonsOfOrder())
            .generalAppNotificationDeadlineDate(application.getGeneralAppDateDeadline())
            .generalAppUrgencyRequirement(application.getGeneralAppUrgencyRequirement())
            .generalAppStatementOfTruth(application.getGeneralAppStatementOfTruth())
            .generalAppHearingDetails(application.getGeneralAppHearingDetails())
            .generalAppEvidenceDocument(application.getGeneralAppEvidenceDocument())
            .isMultiParty(application.getIsMultiParty())
            .parentClaimantIsApplicant(application.getParentClaimantIsApplicant())
            .generalAppParentCaseLink(application.getGeneralAppParentCaseLink())
            .generalAppRespondentSolicitors(application.getGeneralAppRespondentSolicitors())
            .isCcmccLocation(application.getIsCcmccLocation())
            .caseManagementLocation(application.getCaseManagementLocation())
            .build();
    }

    public CaseData buildFeeValidationCaseData(Fee fee, boolean isConsented, boolean isWithNotice) {

        uk.gov.hmcts.reform.ccd.model.Organisation orgId = uk.gov.hmcts.reform.ccd.model.Organisation.builder()
            .organisationID("OrgId").build();

        GAInformOtherParty gaInformOtherParty = null;
        if (!isConsented) {
            gaInformOtherParty = GAInformOtherParty.builder().isWithNotice(isWithNotice ? YES : NO)
                                                .reasonsForWithoutNotice(isWithNotice ? null : STRING_CONSTANT)
                                                .build();
        }
        return CaseData.builder()
            .ccdCaseReference(1644495739087775L)
            .ccdCaseReference(1644495739087775L)
            .legacyCaseReference("000DC001")
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                               .hasAgreed(isConsented ? YES : NO).build())
            .generalAppInformOtherParty(gaInformOtherParty)
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .fee(fee)
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .applicant1OrganisationPolicy(OrganisationPolicy.builder().organisation(orgId).build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().organisationIdentifier("OrgId").build())
            .build();
    }

    public CaseData.CaseDataBuilder consentOrderApplication() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .createdDate(SUBMITTED_DATE_TIME)
            .locationName("County Court")
            .caseManagementLocation(GACaseLocation.builder()
                                        .siteName("County Court")
                                        .baseLocation("2")
                                        .region("4").build())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .approveConsentOrder(GAApproveConsentOrder.builder().consentOrderDescription("testing purpose")
                                     .build())
            .judicialDecision(GAJudicialDecision.builder().decision(MAKE_AN_ORDER).build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .orderText("Test Order")
                                           .orderCourtOwnInitiative("abcd")
                                           .orderCourtOwnInitiativeDate(now())
                                           .judicialByCourtsInitiative(GAByCourtsInitiativeGAspec.OPTION_1)
                                           .reasonForDecisionText("Test Reason")
                                           .makeAnOrder(GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT)
                                           .judgeRecitalText("Test Judge's recital")
                                           .build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder finalOrderFreeForm() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .judgeTitle("John Doe")
            .caseManagementLocation(GACaseLocation.builder().siteName("testing")
                                           .address("london court")
                                        .baseLocation("2")
                                           .postcode("BA 117").build())
            .freeFormRecitalText("abcd")
            .freeFormOrderedText("abcd")
            .orderOnCourtsList(OrderOnCourts.ORDER_ON_COURT_INITIATIVE)
            .orderOnCourtInitiative(FreeFormOrderValues.builder()
                                        .onInitiativeSelectionTextArea("abcd")
                                        .onInitiativeSelectionDate(now()).build())
            .createdDate(SUBMITTED_DATE_TIME)
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder generalOrderApplication() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .judgeTitle("John Doe")
            .caseManagementLocation(GACaseLocation.builder().siteName("testing")
                                        .address("london court")
                                        .baseLocation("2")
                                        .postcode("BA 117").build())
            .isMultiParty(NO)
            .createdDate(SUBMITTED_DATE_TIME)
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(MAKE_AN_ORDER).build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .orderText("Test Order")
                                           .orderCourtOwnInitiative("abcd")
                                           .orderCourtOwnInitiativeDate(now())
                                           .judicialByCourtsInitiative(GAByCourtsInitiativeGAspec.OPTION_1)
                                           .reasonForDecisionText("Test Reason")
                                           .makeAnOrder(GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT)
                                           .showJudgeRecitalText(List.of(FinalOrderShowToggle.SHOW))
                                           .judgeRecitalText("Test Judge's recital")
                                           .build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder generalOrderFreeFormApplication() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .judgeTitle("John Doe")
            .caseManagementLocation(GACaseLocation.builder().siteName("testing")
                                        .address("london court")
                                        .postcode("BA 117").build())
            .isMultiParty(NO)
            .createdDate(SUBMITTED_DATE_TIME)
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(FREE_FORM_ORDER).build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .orderText("Test Order")
                                           .orderCourtOwnInitiative("abcd")
                                           .orderCourtOwnInitiativeDate(now())
                                           .judicialByCourtsInitiative(GAByCourtsInitiativeGAspec.OPTION_1)
                                           .reasonForDecisionText("Test Reason")
                                           .makeAnOrder(GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT)
                                           .showJudgeRecitalText(List.of(FinalOrderShowToggle.SHOW))
                                           .judgeRecitalText("Test Judge's recital")
                                           .build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder judgeFinalOrderApplication() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .judgeTitle("John Doe")
            .caseManagementLocation(GACaseLocation.builder().siteName("testing")
                                        .address("london court")
                                        .postcode("BA 117").build())
            .isMultiParty(NO)
            .createdDate(SUBMITTED_DATE_TIME)
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(MAKE_AN_ORDER).build())
            .finalOrderSelection(FinalOrderSelection.ASSISTED_ORDER)
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .orderText("Test Order")
                                           .orderCourtOwnInitiative("abcd")
                                           .orderCourtOwnInitiativeDate(now())
                                           .judicialByCourtsInitiative(GAByCourtsInitiativeGAspec.OPTION_1)
                                           .reasonForDecisionText("Test Reason")
                                           .makeAnOrder(GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT)
                                           .showJudgeRecitalText(List.of(FinalOrderShowToggle.SHOW))
                                           .judgeRecitalText("Test Judge's recital")
                                           .build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder directionOrderApplication() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .judgeTitle("John Doe")
            .caseManagementLocation(GACaseLocation.builder().siteName("testing")
                                        .address("london court")
                                        .baseLocation("1")
                                        .postcode("BA 117").build())
            .createdDate(LocalDateTime.now())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(MAKE_AN_ORDER).build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .directionsText("Test Direction")
                                           .judicialByCourtsInitiative(GAByCourtsInitiativeGAspec.OPTION_1)
                                           .orderCourtOwnInitiative("abcd")
                                           .orderCourtOwnInitiativeDate(LocalDate.now())
                                           .reasonForDecisionText("Test Reason")
                                           .makeAnOrder(GIVE_DIRECTIONS_WITHOUT_HEARING)
                                           .directionsResponseByDate(LocalDate.now())
                                           .showJudgeRecitalText(List.of(FinalOrderShowToggle.SHOW))
                                           .judgeRecitalText("Test Judge's recital")
                                           .build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder dismissalOrderApplication() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .judgeTitle("John Doe")
            .caseManagementLocation(GACaseLocation.builder().siteName("testing")
                                        .address("london court")
                                        .baseLocation("1")
                                        .postcode("BA 117").build())
            .createdDate(LocalDateTime.now())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(MAKE_AN_ORDER).build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .dismissalOrderText("Test Dismissal")
                                           .reasonForDecisionText("Test Reason")
                                           .orderCourtOwnInitiative("abcd")
                                           .orderCourtOwnInitiativeDate(LocalDate.now())
                                           .judicialByCourtsInitiative(GAByCourtsInitiativeGAspec.OPTION_1)
                                           .makeAnOrder(DISMISS_THE_APPLICATION)
                                           .build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder hearingOrderApplication(YesOrNo isAgreed, YesOrNo isWithNotice) {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .judgeTitle("John Doe")
            .judicialByCourtsInitiativeListForHearing(GAByCourtsInitiativeGAspec.OPTION_1)
            .orderCourtOwnInitiativeListForHearing(GAOrderCourtOwnInitiativeGAspec
                                                       .builder()
                                                       .orderCourtOwnInitiative("abcd")
                                                       .orderCourtOwnInitiativeDate(LocalDate.now()).build())
            .defendant1PartyName("Test Defendant1 Name")
            .locationName("Nottingham County Court and Family Court (and Crown)")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .createdDate(LocalDateTime.now())
            .caseManagementLocation(GACaseLocation.builder().siteName("testing")
                                        .address("london court")
                                        .baseLocation("1")
                                        .postcode("BA 117").build())
            .judicialGeneralHearingOrderRecital("Test Judge's recital")
            .judicialGOHearingDirections("Test hearing direction")
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(isAgreed).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isWithNotice).build())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(LIST_FOR_A_HEARING).build())
            .judicialHearingGOHearingReqText("test")
            .judicialListForHearing(GAJudgesHearingListGAspec.builder()
                                        .hearingPreferencesPreferredType(GAJudicialHearingType.VIDEO)
                                        .judicialTimeEstimate(GAHearingDuration.MINUTES_15)
                                        .build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder hearingScheduledApplication(YesOrNo isCloak) {
        List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

        GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).organisationIdentifier("2").build();

        GASolicitorDetailsGAspec respondent2 = GASolicitorDetailsGAspec.builder().id("id")
            .email(DUMMY_EMAIL).organisationIdentifier("3").build();
        GeneralApplicationsDetails generalApplicationsDetails = GeneralApplicationsDetails.builder()
                .caseState(LISTING_FOR_A_HEARING.getDisplayedValue())
                .caseLink(CaseLink.builder()
                        .caseReference(String.valueOf(CASE_ID)).build())
                .build();
        GADetailsRespondentSol gaDetailsRespondentSol = GADetailsRespondentSol.builder()
                .caseState(LISTING_FOR_A_HEARING.getDisplayedValue())
                .caseLink(CaseLink.builder()
                        .caseReference(String.valueOf(CASE_ID)).build())
                .build();
        respondentSols.add(element(respondent1));
        respondentSols.add(element(respondent2));
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .judgeTitle("John Doe")
            .generalAppParentCaseLink(
                GeneralAppParentCaseLink
                    .builder()
                    .caseReference(CASE_REFERENCE.toString())
                    .build())
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY)
                                 .camundaEvent(HEARING_SCHEDULED).build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .fee(FEE108)
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .createdDate(LocalDateTime.now())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .generalAppApplnSolicitor(
            GASolicitorDetailsGAspec.builder().email(DUMMY_EMAIL).build())
            .applicant1OrganisationPolicy(OrganisationPolicy.builder()
                                              .organisation(Organisation.builder().organisationID("1").build())
                                              .build())
            .respondent1OrganisationPolicy(OrganisationPolicy.builder()
                                               .organisation(Organisation.builder().organisationID("2").build())
                                               .build())
            .respondent2OrganisationPolicy(OrganisationPolicy.builder()
                                               .organisation(Organisation.builder().organisationID("3").build())
                                               .build())
            .judicialDecision(GAJudicialDecision.builder().decision(LIST_FOR_A_HEARING).build())
            .caseManagementLocation(GACaseLocation.builder().siteName("testing")
                                        .address("london court")
                                        .baseLocation("1")
                                        .postcode("BA 117").build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(NO).build())
            .businessProcess(BusinessProcess.builder().camundaEvent(HEARING_SCHEDULED).build())
            .applicationIsCloaked(isCloak)
            .generalAppRespondentSolicitors(respondentSols)
            .gaDetailsMasterCollection(wrapElements(GeneralApplicationsDetails.builder()
                                                        .caseState(LISTING_FOR_A_HEARING.getDisplayedValue())
                                                        .caseLink(CaseLink.builder()
                                                                      .caseReference(String.valueOf(CASE_ID)).build())
                                                        .build()))
            .claimantGaAppDetails(
                wrapElements(generalApplicationsDetails
                ))
            .respondentSolGaAppDetails(wrapElements(gaDetailsRespondentSol))
            .respondentSolTwoGaAppDetails(wrapElements(gaDetailsRespondentSol))
            .gaHearingNoticeDetail(GAHearingNoticeDetail.builder()
                .channel(GAJudicialHearingType.IN_PERSON)
                .hearingDuration(GAHearingDuration.HOUR_1)
                .hearingTimeHourMinute("1530")
                .hearingDate(LocalDate.now().plusDays(10))
                .hearingLocation(getLocationDynamicList()).build())
            .gaHearingNoticeApplication(GAHearingNoticeApplication.builder()
                    .hearingNoticeApplicationDate(LocalDate.now())
                    .hearingNoticeApplicationDetail(HearingApplicationDetails.CLAIMANT_AND_DEFENDANT)
                    .hearingNoticeApplicationType("type").build())
            .gaHearingNoticeInformation("testing");
    }

    public CaseData.CaseDataBuilder writtenRepresentationSequentialApplication() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .judgeTitle("John Doe")
            .caseManagementLocation(GACaseLocation.builder().siteName("testing")
                                        .address("london court")
                                        .baseLocation("1")
                                        .postcode("BA 117").build())
            .judicialByCourtsInitiativeForWrittenRep(GAByCourtsInitiativeGAspec.OPTION_1)
            .orderCourtOwnInitiativeForWrittenRep(
                GAOrderCourtOwnInitiativeGAspec.builder()
                    .orderCourtOwnInitiative("abcd")
                    .orderCourtOwnInitiativeDate(LocalDate.now()).build())
            .judgeRecitalText("Test Judge's recital")
            .directionInRelationToHearingText("Test written order")
            .createdDate(LocalDateTime.now())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS).build())
            .judicialDecisionMakeAnOrderForWrittenRepresentations(
                GAJudicialWrittenRepresentations.builder()
                    .writtenOption(SEQUENTIAL_REPRESENTATIONS)
                    .writtenSequentailRepresentationsBy(now())
                    .sequentialApplicantMustRespondWithin(now()
                                                              .plusDays(5)).build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder approveApplication() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .caseManagementLocation(GACaseLocation.builder().siteName("testing")
                                        .address("london court")
                                        .postcode("BA 117").build())
            .judicialByCourtsInitiativeForWrittenRep(GAByCourtsInitiativeGAspec.OPTION_1)
            .orderCourtOwnInitiativeForWrittenRep(
                GAOrderCourtOwnInitiativeGAspec.builder()
                    .orderCourtOwnInitiative("abcd")
                    .orderCourtOwnInitiativeDate(LocalDate.now()).build())
            .judgeRecitalText("Test Judge's recital")
            .directionInRelationToHearingText("Test written order")
            .createdDate(LocalDateTime.now())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(MAKE_AN_ORDER).build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().makeAnOrder(GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT).build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder writtenRepresentationConcurrentApplication() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .judgeTitle("John Doe")
            .caseManagementLocation(GACaseLocation.builder().siteName("testing")
                                        .address("london court")
                                        .baseLocation("2")
                                        .postcode("BA 117").build())
            .judicialByCourtsInitiativeForWrittenRep(GAByCourtsInitiativeGAspec.OPTION_1)
            .orderCourtOwnInitiativeForWrittenRep(
                GAOrderCourtOwnInitiativeGAspec.builder()
                    .orderCourtOwnInitiative("abcd")
                    .orderCourtOwnInitiativeDate(LocalDate.now()).build())
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .createdDate(LocalDateTime.now())
            .judgeRecitalText("Test Judge's recital")
            .directionInRelationToHearingText("Test written order")
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS).build())
            .judicialDecisionMakeAnOrderForWrittenRepresentations(
                GAJudicialWrittenRepresentations.builder()
                    .writtenOption(CONCURRENT_REPRESENTATIONS)
                    .writtenConcurrentRepresentationsBy(LocalDate.now())
                    .build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder requestForInformationApplication() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .locationName("Nottingham County Court and Family Court (and Crown)")
            .caseManagementLocation(GACaseLocation.builder().siteName("testing")
                                        .address("london court")
                                        .baseLocation("2")
                                        .postcode("BA 117").build())
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .fee(FEE275)
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .createdDate(LocalDateTime.now())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(REQUEST_MORE_INFO).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YES).build())
            .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                 .judgeRecitalText(JUDICIAL_REQUEST_MORE_INFO_RECITAL_TEXT)
                                                 .requestMoreInfoOption(REQUEST_MORE_INFORMATION)
                                                 .judgeRequestMoreInfoByDate(now())
                                                 .judgeRequestMoreInfoText("test").build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder judicialDecisionWithUncloakRequestForInformationApplication(
        GAJudgeRequestMoreInfoOption requestMoreInfoOption, YesOrNo isWithNotice, YesOrNo isCloak) {

        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY)
                                 .camundaEvent(JUDGES_DECISION).build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .fee(FEE108)
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .createdDate(LocalDateTime.now())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(REQUEST_MORE_INFO).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(YesOrNo.NO).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(isWithNotice).build())
            .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                 .requestMoreInfoOption(requestMoreInfoOption)
                                                 .judgeRequestMoreInfoByDate(LocalDate.now())
                                                 .judgeRequestMoreInfoText("test").build())
            .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
            .applicationIsCloaked(isCloak)
            .isMultiParty(NO)
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder judicialOrderMadeWithUncloakApplication(YesOrNo isCloak) {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .generalAppParentCaseLink(
                GeneralAppParentCaseLink
                    .builder()
                    .caseReference(CASE_REFERENCE.toString())
                    .build())
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY)
                                 .camundaEvent(JUDGES_DECISION).build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .fee(FEE108)
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .createdDate(LocalDateTime.now())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(MAKE_AN_ORDER).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(NO).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(NO).build())
            .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
            .applicationIsCloaked(isCloak)
            .submittedOn(APPLICATION_SUBMITTED_DATE)
            .gaDetailsMasterCollection(wrapElements(GeneralApplicationsDetails.builder()
                                                        .caseState(APPLICATION_ADD_PAYMENT.getDisplayedValue())
                                                        .caseLink(CaseLink.builder()
                                                                      .caseReference(String.valueOf(CASE_ID)).build())
                                                        .build()))
            .claimantGaAppDetails(wrapElements(GeneralApplicationsDetails.builder()
                                                   .caseState(APPLICATION_ADD_PAYMENT.getDisplayedValue())
                                                   .caseLink(CaseLink.builder()
                                                                 .caseReference(String.valueOf(CASE_ID)).build())
                                                   .build()));
    }

    public CaseData.CaseDataBuilder adjournOrVacateHearingApplication(
            YesOrNo isRespondentAgreed, LocalDate gaHearingDate) {
        GAHearingDateGAspec generalAppHearingDate = GAHearingDateGAspec.builder()
            .hearingScheduledDate(gaHearingDate)
            .build();
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY)
                                 .build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .fee(FEE108)
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .createdDate(LocalDateTime.now())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(ADJOURN_HEARING))
                                .build())
            .generalAppHearingDate(generalAppHearingDate)
            .generalAppRespondentAgreement(GARespondentOrderAgreement
                                               .builder().hasAgreed(isRespondentAgreed).build())
            .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder varyApplication(List<GeneralApplicationTypes> types) {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY)
                                 .build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .fee(FEE14)
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .createdDate(LocalDateTime.now())
            .generalAppType(GAApplicationType.builder()
                                .types(types)
                                .build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement
                                               .builder().hasAgreed(NO).build())
            .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder getMainCaseDataWithDetails(
                                               boolean withGADetails,
                                               boolean withGADetailsResp,
                                               boolean withGADetailsResp2,
                                               boolean withGADetailsMaster) {

        CaseData.CaseDataBuilder caseDataBuilder = build().toBuilder();
        caseDataBuilder.ccdCaseReference(1L);
        GeneralApplicationsDetails generalApplicationsDetails = GeneralApplicationsDetails.builder()
                .caseState(LISTING_FOR_A_HEARING.getDisplayedValue())
                .caseLink(CaseLink.builder()
                        .caseReference(String.valueOf(CASE_ID)).build())
                .build();

        if (withGADetails) {
            caseDataBuilder.claimantGaAppDetails(
                    wrapElements(generalApplicationsDetails
                    ));
        }

        if (withGADetailsMaster) {
            caseDataBuilder.gaDetailsMasterCollection(
                    wrapElements(generalApplicationsDetails
                    ));
        }

        GADetailsRespondentSol gaDetailsRespondentSol = GADetailsRespondentSol.builder()
                .caseState(LISTING_FOR_A_HEARING.getDisplayedValue())
                .caseLink(CaseLink.builder()
                        .caseReference(String.valueOf(CASE_ID)).build())
                .build();
        if (withGADetailsResp) {
            caseDataBuilder.respondentSolGaAppDetails(wrapElements(gaDetailsRespondentSol));
        }

        if (withGADetailsResp2) {
            caseDataBuilder.respondentSolTwoGaAppDetails(wrapElements(gaDetailsRespondentSol));
        }
        return caseDataBuilder;
    }

    public DynamicList getLocationDynamicList() {
        DynamicListElement location1 = DynamicListElement.builder()
                .code(String.valueOf(UUID.randomUUID())).label("ABCD - RG0 0AL").build();
        DynamicListElement location2 = DynamicListElement.builder()
                .code(String.valueOf(UUID.randomUUID())).label("PQRS - GU0 0EE").build();
        DynamicListElement location3 = DynamicListElement.builder()
                .code(String.valueOf(UUID.randomUUID())).label("WXYZ - EW0 0HE").build();
        DynamicListElement location4 = DynamicListElement.builder()
                .code(String.valueOf(UUID.randomUUID())).label("LMNO - NE0 0BH").build();

        return DynamicList.builder()
                .listItems(List.of(location1, location2, location3, location4))
                .value(location1).build();
    }
}
