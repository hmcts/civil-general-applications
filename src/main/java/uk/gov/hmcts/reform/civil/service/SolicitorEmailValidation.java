package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitorEmailValidation {

    private GASolicitorDetailsGAspec updateSolDetails(String updateEmail,
                                                      GASolicitorDetailsGAspec generalAppSolicitor) {

        GASolicitorDetailsGAspec.GASolicitorDetailsGAspecBuilder
            gaSolicitorDetailsGAspecBuilder = GASolicitorDetailsGAspec.builder();

        gaSolicitorDetailsGAspecBuilder.id(generalAppSolicitor.getId())
            .forename(generalAppSolicitor.getForename())
            .surname(generalAppSolicitor.getSurname())
            .organisationIdentifier(generalAppSolicitor.getOrganisationIdentifier())
            .email(updateEmail);

        return gaSolicitorDetailsGAspecBuilder.build();

    }

    public GASolicitorDetailsGAspec checkIfOrgIDMatch(GASolicitorDetailsGAspec generalAppSolicitor,
                                                       CaseData civilCaseData, CaseData gaCaseData) {

        // civil claim applicant

        if (civilCaseData.getApplicant1OrganisationPolicy() != null
            && checkIfOrgIdExists(civilCaseData.getApplicant1OrganisationPolicy())) {

            String civilClaimApplicantOrgId = civilCaseData.getApplicant1OrganisationPolicy()
                .getOrganisation().getOrganisationID();

            boolean isGASolicitorEmailMatchWithCivilApplnSol = generalAppSolicitor.getEmail()
                .equals(civilCaseData.getApplicantSolicitor1UserDetails().getEmail());

            if (civilClaimApplicantOrgId.equals(generalAppSolicitor.getOrganisationIdentifier())
                && ! isGASolicitorEmailMatchWithCivilApplnSol) {
                // Update GA Solicitor Email ID as same as Civil Claim claimant Solicitor Email
                log.info("Update GA Solicitor Email ID as same as Civil Claim claimant Solicitor Email");
                return updateSolDetails(civilCaseData.getApplicantSolicitor1UserDetails()
                                            .getEmail(), generalAppSolicitor);

            }

        }

        // civil claim defendant 1

        if (civilCaseData.getRespondent1OrganisationPolicy() != null
            && checkIfOrgIdExists(civilCaseData.getRespondent1OrganisationPolicy())) {

            String civilClaimRespondent1OrgId = civilCaseData.getRespondent1OrganisationPolicy()
                .getOrganisation().getOrganisationID();
            boolean isGASolicitorEmailMatchWithCivilRespondent1Sol = generalAppSolicitor.getEmail()
                .equals(civilCaseData.getRespondentSolicitor1EmailAddress());
            if (civilClaimRespondent1OrgId.equals(generalAppSolicitor.getOrganisationIdentifier())
                && ! isGASolicitorEmailMatchWithCivilRespondent1Sol) {
                log.info("Update GA Solicitor Email ID as same as Civil Claim Respondent Solicitor one Email");
                return updateSolDetails(civilCaseData.getRespondentSolicitor1EmailAddress(), generalAppSolicitor);
            }

        }

        if (YES.equals(gaCaseData.getIsMultiParty())) {

            // civil claim defendant 2

            if (civilCaseData.getRespondent2OrganisationPolicy() != null
                && checkIfOrgIdExists(civilCaseData.getRespondent2OrganisationPolicy())) {

                boolean isGASolicitorEmailMatchWithCivilRespondent2Sol = generalAppSolicitor.getEmail()
                    .equals(civilCaseData.getRespondentSolicitor2EmailAddress());
                String civilClaimRespondent2OrgId = civilCaseData.getRespondent2OrganisationPolicy()
                    .getOrganisation().getOrganisationID();
                if (civilClaimRespondent2OrgId.equals(generalAppSolicitor.getOrganisationIdentifier())
                    && ! isGASolicitorEmailMatchWithCivilRespondent2Sol) {
                    log.info("Update GA Solicitor Email ID as same as Civil Claim Respondent Solicitor Two Email");
                    return updateSolDetails(civilCaseData.getRespondentSolicitor2EmailAddress(), generalAppSolicitor);
                }

            }
        }

        return generalAppSolicitor;

    }

    public boolean checkIfOrgIdExists(OrganisationPolicy organisationPolicy) {
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

        gaCaseData.getGeneralAppRespondentSolicitors().forEach(rs -> generalAppRespondentSolicitors
            .add(element(checkIfOrgIDMatch(rs.getValue(), civilCaseData, gaCaseData))));

        caseDataBuilder.generalAppRespondentSolicitors(generalAppRespondentSolicitors.isEmpty()
                                                           ? gaCaseData.getGeneralAppRespondentSolicitors()
                                                           : generalAppRespondentSolicitors);

        return caseDataBuilder.build();
    }
}
