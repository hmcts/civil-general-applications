package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.service.tasklisteners.GaForLipService;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.civil.utils.OrgPolicyUtils.getRespondent1SolicitorOrgId;
import static uk.gov.hmcts.reform.civil.utils.OrgPolicyUtils.getRespondent2SolicitorOrgId;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitorEmailValidation {

    private final GaForLipService gaForLipService;

    private GASolicitorDetailsGAspec updateSolDetails(String updateEmail,
                                                      GASolicitorDetailsGAspec generalAppSolicitor) {

        GASolicitorDetailsGAspec.GASolicitorDetailsGAspecBuilder gaSolicitorDetailsGAspecBuilder =
            generalAppSolicitor.toBuilder();

        gaSolicitorDetailsGAspecBuilder.email(updateEmail);

        return gaSolicitorDetailsGAspecBuilder.build();

    }

    private boolean checkIfOrgIdAndEmailAreSame(String organisationID, String civilSolEmail,
                                               GASolicitorDetailsGAspec generalAppSolicitor) {

        return organisationID.equals(generalAppSolicitor.getOrganisationIdentifier())
            && ! generalAppSolicitor.getEmail().equals(civilSolEmail);

    }

    private GASolicitorDetailsGAspec checkIfOrgIDMatch(GASolicitorDetailsGAspec generalAppSolicitor,
                                                       CaseData civilCaseData, CaseData gaCaseData) {

        // civil claim applicant
        if (civilCaseData.getApplicant1OrganisationPolicy() != null
            && checkIfOrgIdExists(civilCaseData.getApplicant1OrganisationPolicy())
            && (checkIfOrgIdAndEmailAreSame(civilCaseData.getApplicant1OrganisationPolicy()
                                                .getOrganisation().getOrganisationID(),
                                            civilCaseData.getApplicantSolicitor1UserDetails().getEmail(),
                                            generalAppSolicitor))) {

            // Update GA Solicitor Email ID as same as Civil Claim claimant Solicitor Email
            log.info("Update GA Solicitor Email ID as same as Civil Claim claimant Solicitor Email");
            return updateSolDetails(civilCaseData.getApplicantSolicitor1UserDetails()
                                            .getEmail(), generalAppSolicitor);

        }

        // civil claim defendant 1
        if (civilCaseData.getRespondent1OrganisationPolicy() != null
            && getRespondent1SolicitorOrgId(civilCaseData) != null
            && (checkIfOrgIdAndEmailAreSame(getRespondent1SolicitorOrgId(civilCaseData),
                                            civilCaseData.getRespondentSolicitor1EmailAddress(),
                                            generalAppSolicitor))) {

            log.info("Update GA Solicitor Email ID as same as Civil Claim Respondent Solicitor one Email");
            return updateSolDetails(civilCaseData.getRespondentSolicitor1EmailAddress(), generalAppSolicitor);

        }

        // civil claim defendant 2
        if (YES.equals(gaCaseData.getIsMultiParty())
            && NO.equals(civilCaseData.getRespondent2SameLegalRepresentative())
            && (getRespondent2SolicitorOrgId(civilCaseData) != null
            && (checkIfOrgIdAndEmailAreSame(getRespondent2SolicitorOrgId(civilCaseData),
                                            civilCaseData.getRespondentSolicitor2EmailAddress(),
                                            generalAppSolicitor)))) {

            log.info("Update GA Solicitor Email ID as same as Civil Claim Respondent Solicitor Two Email");
            return updateSolDetails(civilCaseData.getRespondentSolicitor2EmailAddress(), generalAppSolicitor);
        }
        return generalAppSolicitor;

    }

    private boolean checkIfOrgIdExists(OrganisationPolicy organisationPolicy) {
        return organisationPolicy.getOrganisation() != null
            && organisationPolicy.getOrganisation().getOrganisationID() != null;
    }

    public CaseData validateSolicitorEmail(CaseData civilCaseData, CaseData gaCaseData) {

        // GA Applicant solicitor
        CaseData.CaseDataBuilder caseDataBuilder = gaCaseData.toBuilder();

        caseDataBuilder.generalAppApplnSolicitor(checkIfOrgIDMatch(gaCaseData.getGeneralAppApplnSolicitor(),
                                                                   civilCaseData, gaCaseData));

        // GA Respondent solicitor
        List<Element<GASolicitorDetailsGAspec>> generalAppRespondentSolicitors = newArrayList();

        if (!gaForLipService.isGaForLip(gaCaseData)) {
            gaCaseData.getGeneralAppRespondentSolicitors().forEach(rs -> generalAppRespondentSolicitors
                .add(element(checkIfOrgIDMatch(rs.getValue(), civilCaseData, gaCaseData))));

            caseDataBuilder.generalAppRespondentSolicitors(generalAppRespondentSolicitors.isEmpty()
                                                               ? gaCaseData.getGeneralAppRespondentSolicitors()
                                                               : generalAppRespondentSolicitors);
        } else {
            /*GA for LIP*/
            caseDataBuilder.generalAppRespondentSolicitors(gaCaseData.getGeneralAppRespondentSolicitors());
        }


        return caseDataBuilder.build();
    }
}
