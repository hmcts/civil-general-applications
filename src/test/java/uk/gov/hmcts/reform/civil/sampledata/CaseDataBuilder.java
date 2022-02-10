package uk.gov.hmcts.reform.civil.sampledata;

import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.time.LocalDate.now;

public class CaseDataBuilder {

    public static final String LEGACY_CASE_REFERENCE = "000DC001";
    public static final Long CASE_ID = 1594901956117591L;
    public static final LocalDateTime SUBMITTED_DATE_TIME = LocalDateTime.now();
    public static final LocalDateTime RESPONSE_DEADLINE = SUBMITTED_DATE_TIME.toLocalDate().plusDays(14)
        .atTime(23, 59, 59);
    public static final LocalDateTime APPLICANT_RESPONSE_DEADLINE = SUBMITTED_DATE_TIME.plusDays(120);
    public static final LocalDate CLAIM_ISSUED_DATE = now();
    public static final LocalDateTime DEADLINE = LocalDate.now().atStartOfDay().plusDays(14);
    public static final LocalDate PAST_DATE = now().minusDays(1);
    public static final LocalDateTime NOTIFICATION_DEADLINE = LocalDate.now().atStartOfDay().plusDays(1);
    public static final BigDecimal FAST_TRACK_CLAIM_AMOUNT = BigDecimal.valueOf(10000);

    // Create Claim
    protected Long ccdCaseReference;

    protected String respondentSolicitor1EmailAddress;

    protected String legacyCaseReference;

    protected GAInformOtherParty gaInformOtherParty;

    protected GAUrgencyRequirement gaUrgencyRequirement;

    protected GARespondentOrderAgreement gaRespondentOrderAgreement;

    protected CaseState ccdState;

    // Claimant Response
    protected BusinessProcess businessProcess;

    private GeneralAppParentCaseLink generalAppParentCaseLink;

    protected List<Element<GeneralApplication>> generalApplications;

    public CaseDataBuilder legacyCaseReference(String legacyCaseReference) {
        this.legacyCaseReference = legacyCaseReference;
        return this;
    }

    public CaseDataBuilder generalApplications(List<Element<GeneralApplication>> generalApplications) {
        this.generalApplications = generalApplications;
        return this;
    }

    public CaseDataBuilder respondentSolicitor1EmailAddress(String respondentSolicitor1EmailAddress) {
        this.respondentSolicitor1EmailAddress = respondentSolicitor1EmailAddress;
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

    public CaseDataBuilder atStateClaimDraft() {

        return this;
    }

    public static CaseDataBuilder builder() {
        return new CaseDataBuilder();
    }

    public CaseData build() {
        return CaseData.builder()
            .businessProcess(businessProcess)
            .ccdCaseReference(ccdCaseReference)
            .respondentSolicitor1EmailAddress(respondentSolicitor1EmailAddress)
            .legacyCaseReference(legacyCaseReference)
            .generalApplications(generalApplications)
            .generalAppInformOtherParty(gaInformOtherParty)
            .generalAppUrgencyRequirement(gaUrgencyRequirement)
            .generalAppRespondentAgreement(gaRespondentOrderAgreement)
            .generalAppParentCaseLink(generalAppParentCaseLink)
            .build();
    }

}
