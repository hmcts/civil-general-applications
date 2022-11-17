package uk.gov.hmcts.reform.civil.sampledata;

import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
import uk.gov.hmcts.reform.civil.enums.PaymentStatus;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingDuration;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption;
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
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GADetailsRespondentSol;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDateGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.model.genapplication.GAMakeApplicationAvailableCheck;
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
import java.util.List;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_ADD_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.LIST_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_AN_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.CONCURRENT_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.ADJOURN_VACATE_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.EXTEND_TIME;
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
    private static final Fee FEE108 = Fee.builder().calculatedAmountInPence(
        BigDecimal.valueOf(10800)).code("FEE0443").version("1").build();
    private static final Fee FEE275 = Fee.builder().calculatedAmountInPence(
        BigDecimal.valueOf(27500)).code("FEE0442").version("1").build();
    public static final String STRING_CONSTANT = "this is a string";

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
    protected List<Element<GeneralApplicationsDetails>> generalApplicationsDetails;
    protected List<Element<GADetailsRespondentSol>> gaDetailsRespondentSol;
    protected List<Element<GADetailsRespondentSol>> gaDetailsRespondentSolTwo;
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
        this.generalApplicationsDetails = generalApplicationsDetails;
        return this;
    }

    public CaseDataBuilder gaDetailsRespondentSol(List<Element<GADetailsRespondentSol>>
                                                      gaDetailsRespondentSol) {
        this.gaDetailsRespondentSol = gaDetailsRespondentSol;
        return this;
    }

    public CaseDataBuilder gaDetailsRespondentSolTwo(List<Element<GADetailsRespondentSol>>
                                                      gaDetailsRespondentSolTwo) {
        this.gaDetailsRespondentSolTwo = gaDetailsRespondentSolTwo;
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
            .gaDetailsRespondentSolTwo(gaDetailsRespondentSolTwo)
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
            .generalApplicationsDetails(generalApplicationsDetails)
            .gaDetailsRespondentSol(gaDetailsRespondentSol)
            .generalAppPBADetails(gaPbaDetails)
            .applicant1OrganisationPolicy(applicant1OrganisationPolicy)
            .generalAppNotificationDeadlineDate(generalAppDeadlineNotificationDate)
            .parentClaimantIsApplicant(parentClaimantIsApplicant)
            .makeAppVisibleToRespondents(makeAppVisibleToRespondents)
            .judicialDecisionMakeOrder(judicialMakeAnOrder)
            .generalAppType(generalAppType)
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
                    .applicantsPbaAccounts(
                        DynamicList.builder()
                            .listItems(asList(
                                DynamicListElement.builder().label("PBA0088192").build(),
                                DynamicListElement.builder().label("PBA0078095").build()
                            ))
                            .value(
                                DynamicListElement.dynamicElement("PBA0078095"))
                            .build())
                    .fee(
                        Fee.builder()
                            .code("FE203")
                            .calculatedAmountInPence(BigDecimal.valueOf(27500))
                            .version("1")
                            .build())
                    .pbaReference(CUSTOMER_REFERENCE)
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
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .paymentDetails(PaymentDetails.builder()
                                        .status(PaymentStatus.FAILED)
                                        .reference("RC-1658-4258-2679-9795")
                                        .customerReference(CUSTOMER_REFERENCE)
                                        .build())
                    .applicantsPbaAccounts(
                        DynamicList.builder()
                            .listItems(asList(
                                DynamicListElement.builder().label("PBA0088192").build(),
                                DynamicListElement.builder().label("PBA0078095").build()
                            ))
                            .value(
                                DynamicListElement.dynamicElement("PBA0078095"))
                            .build())
                    .fee(
                        Fee.builder()
                            .code("FE203")
                            .calculatedAmountInPence(BigDecimal.valueOf(27500))
                            .version("1")
                            .build())
                    .pbaReference(CUSTOMER_REFERENCE)
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
                    .applicantsPbaAccounts(
                        DynamicList.builder()
                            .listItems(asList(
                                DynamicListElement.builder().label("PBA0088192").build(),
                                DynamicListElement.builder().label("PBA0078095").build()
                            ))
                            .value(
                                DynamicListElement.dynamicElement("PBA0078095"))
                            .build())
                    .fee(
                        Fee.builder()
                            .code("FE203")
                            .calculatedAmountInPence(BigDecimal.valueOf(27500))
                            .version("1")
                            .build())
                    .pbaReference(CUSTOMER_REFERENCE)
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .applicant1OrganisationPolicy(OrganisationPolicy.builder().organisation(orgId).build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().organisationIdentifier("OrgId").build())
            .build();
    }

    public CaseData buildAdditionalPaymentsReferenceCaseData() {
        uk.gov.hmcts.reform.ccd.model.Organisation orgId = uk.gov.hmcts.reform.ccd.model.Organisation.builder()
            .organisationID("OrgId").build();

        return build().toBuilder()
            .ccdCaseReference(1644495739087775L)
            .ccdCaseReference(1644495739087775L)
            .legacyCaseReference("000DC001")
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .applicantsPbaAccounts(
                        DynamicList.builder()
                            .listItems(asList(
                                DynamicListElement.builder().label("PBA0088192").build(),
                                DynamicListElement.builder().label("PBA0078095").build()
                            ))
                            .value(
                                DynamicListElement.dynamicElement("PBA0078095"))
                            .build())
                    .fee(
                        Fee.builder()
                            .code("FEE0444")
                            .calculatedAmountInPence(BigDecimal.valueOf(16700))
                            .version("1")
                            .build())
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .applicant1OrganisationPolicy(OrganisationPolicy.builder().organisation(orgId).build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().organisationIdentifier("OrgId").build())
            .build();
    }

    public CaseData buildCaseDateBaseOnGeneralApplication(GeneralApplication application) {
        return CaseData.builder()
            .generalAppType(application.getGeneralAppType())
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
                    .applicantsPbaAccounts(
                        DynamicList.builder()
                            .listItems(asList(
                                DynamicListElement.builder().label("PBA0088192").build(),
                                DynamicListElement.builder().label("PBA0078095").build()
                            ))
                            .value(DynamicListElement.dynamicElement("PBA0078095"))
                            .build())
                    .fee(fee)
                    .pbaReference(CUSTOMER_REFERENCE)
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .applicant1OrganisationPolicy(OrganisationPolicy.builder().organisation(orgId).build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().organisationIdentifier("OrgId").build())
            .build();
    }

    public CaseData.CaseDataBuilder generalOrderApplication() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .createdDate(SUBMITTED_DATE_TIME)
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(MAKE_AN_ORDER).build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .orderText("Test Order")
                                           .reasonForDecisionText("Test Reason")
                                           .makeAnOrder(GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT)
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
            .createdDate(LocalDateTime.now())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(MAKE_AN_ORDER).build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .directionsText("Test Direction")
                                           .reasonForDecisionText("Test Reason")
                                           .makeAnOrder(GIVE_DIRECTIONS_WITHOUT_HEARING)
                                           .directionsResponseByDate(LocalDate.now())
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
            .createdDate(LocalDateTime.now())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(MAKE_AN_ORDER).build())
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .dismissalOrderText("Test Dismissal")
                                           .reasonForDecisionText("Test Reason")
                                           .makeAnOrder(DISMISS_THE_APPLICATION)
                                           .build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder hearingOrderApplication(YesOrNo isAgreed, YesOrNo isWithNotice) {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .createdDate(LocalDateTime.now())
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

    public CaseData.CaseDataBuilder writtenRepresentationSequentialApplication() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
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
                    .writtenSequentailRepresentationsBy(LocalDate.now())
                    .sequentialApplicantMustRespondWithin(LocalDate.now()
                                                              .plusDays(5)).build())
            .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

    public CaseData.CaseDataBuilder writtenRepresentationConcurrentApplication() {
        return CaseData.builder()
            .ccdCaseReference(CASE_ID)
            .claimant1PartyName("Test Claimant1 Name")
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
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
            .claimant2PartyName("Test Claimant2 Name")
            .defendant1PartyName("Test Defendant1 Name")
            .defendant2PartyName("Test Defendant2 Name")
            .applicantPartyName("Test Applicant Name")
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .applicantsPbaAccounts(
                        DynamicList.builder()
                            .listItems(asList(
                                DynamicListElement.builder().label("PBA0088192").build(),
                                DynamicListElement.builder().label("PBA0078095").build()
                            ))
                            .value(DynamicListElement.dynamicElement("PBA0078095"))
                            .build())
                    .fee(FEE275)
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .createdDate(LocalDateTime.now())
            .generalAppType(GAApplicationType.builder()
                                .types(singletonList(EXTEND_TIME))
                                .build())
            .judicialDecision(GAJudicialDecision.builder().decision(REQUEST_MORE_INFO).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder().hasAgreed(YesOrNo.NO).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YesOrNo.YES).build())
            .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                 .judgeRecitalText(JUDICIAL_REQUEST_MORE_INFO_RECITAL_TEXT)
                                                 .requestMoreInfoOption(REQUEST_MORE_INFORMATION)
                                                 .judgeRequestMoreInfoByDate(LocalDate.now())
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
                    .applicantsPbaAccounts(
                        DynamicList.builder()
                            .listItems(asList(
                                DynamicListElement.builder().label("PBA0088192").build(),
                                DynamicListElement.builder().label("PBA0078095").build()
                            ))
                            .value(DynamicListElement.dynamicElement("PBA0078095"))
                            .build())
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
                    .applicantsPbaAccounts(
                        DynamicList.builder()
                            .listItems(asList(
                                DynamicListElement.builder().label("PBA0088192").build(),
                                DynamicListElement.builder().label("PBA0078095").build()
                            ))
                            .value(DynamicListElement.dynamicElement("PBA0078095"))
                            .build())
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
            .generalApplicationsDetails(wrapElements(GeneralApplicationsDetails.builder()
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
                                .applicantsPbaAccounts(
                                        DynamicList.builder()
                                                .listItems(asList(
                                                        DynamicListElement.builder().label("PBA0088192").build(),
                                                        DynamicListElement.builder().label("PBA0078095").build()
                                                ))
                                                .value(DynamicListElement.dynamicElement("PBA0078095"))
                                                .build())
                                .fee(FEE108)
                                .serviceReqReference(CUSTOMER_REFERENCE).build())
                .createdDate(LocalDateTime.now())
                .generalAppType(GAApplicationType.builder()
                        .types(singletonList(ADJOURN_VACATE_HEARING))
                        .build())
                .generalAppHearingDate(generalAppHearingDate)
                .generalAppRespondentAgreement(GARespondentOrderAgreement
                        .builder().hasAgreed(isRespondentAgreed).build())
                .businessProcess(BusinessProcess.builder().camundaEvent(JUDGES_DECISION).build())
                .submittedOn(APPLICATION_SUBMITTED_DATE);
    }

}
