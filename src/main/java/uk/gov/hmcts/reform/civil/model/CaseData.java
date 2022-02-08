package uk.gov.hmcts.reform.civil.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAEvidence;
import uk.gov.hmcts.reform.civil.model.genapplication.GAHearingDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.model.genapplication.GAStatementOfTruth;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;

import java.time.LocalDateTime;
import java.util.List;

import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.FINISHED;

@Data
@Builder(toBuilder = true)
public class CaseData implements MappableObject {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private final Long ccdCaseReference;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private final CaseState ccdState;
    private final String detailsOfClaim;
    private final YesOrNo addApplicant2;
    private final GAApplicationType generalAppType;
    private final GARespondentOrderAgreement generalAppRespondentAgreement;
    private final GAPbaDetails generalAppPBADetails;
    private final String generalAppDetailsOfOrder;
    private final String generalAppReasonsOfOrder;
    private final String legacyCaseReference;
    private final String respondentSolicitor1EmailAddress;
    private final LocalDateTime notificationDeadline;
    private final GAInformOtherParty generalAppInformOtherParty;
    private final GAUrgencyRequirement generalAppUrgencyRequirement;
    private final GAStatementOfTruth generalAppStatementOfTruth;
    private final GAHearingDetails generalAppHearingDetails;
    private final GAEvidence generalAppUploadEvidences;
    private final YesOrNo isMultiParty;
    private final CaseLink caseLink;
    private GeneralAppParentCaseLink generalAppParentCaseLink;
    private final IdamUserDetails applicantSolicitor1UserDetails;
    private final OrganisationPolicy applicant1OrganisationPolicy;
    private final OrganisationPolicy respondent1OrganisationPolicy;
    private final String respondentSolicitor1EmailAddress;
    private final List<Element<GeneralApplication>> generalApplications;

    private final BusinessProcess businessProcess;

    public boolean hasNoOngoingBusinessProcess() {
        return businessProcess == null
            || businessProcess.getStatus() == null
            || businessProcess.getStatus() == FINISHED;
    }

}
