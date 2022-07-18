package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    public GASolicitorDetailsGAspec checkIfOrgIDMatch (GASolicitorDetailsGAspec generalAppSolicitor,
                                                       CaseData civilCaseData, CaseData gaCaseData) {

        String civilClaimApplicantOrgId = civilCaseData.getApplicant1OrganisationPolicy()
            .getOrganisation().getOrganisationID();
        String civilClaimRespondent1OrgId = civilCaseData.getRespondent1OrganisationPolicy()
            .getOrganisation().getOrganisationID();

        // civil claim applicant
        boolean isGASolicitorEmailMatchWithCivilApplnSol = generalAppSolicitor.getEmail()
            .equals(civilCaseData.getApplicantSolicitor1UserDetails().getEmail());

        if (civilClaimApplicantOrgId.equals(generalAppSolicitor.getOrganisationIdentifier())
            && isGASolicitorEmailMatchWithCivilApplnSol) {
            // Update GA Solicitor Email ID as same as Civil Claim claimant Solicitor Email
            log.info("Update GA Solicitor Email ID as same as Civil Claim claimant Solicitor Email");
            return updateSolDetails(civilCaseData.getApplicantSolicitor1UserDetails().getEmail(), generalAppSolicitor);

        }

        // civil claim defendant 1
        boolean isGASolicitorEmailMatchWithCivilRespondent1Sol = generalAppSolicitor.getEmail()
            .equals(civilCaseData.getRespondentSolicitor1EmailAddress());
        if (civilClaimRespondent1OrgId.equals(generalAppSolicitor.getOrganisationIdentifier())
            && isGASolicitorEmailMatchWithCivilRespondent1Sol) {
            log.info("Update GA Solicitor Email ID as same as Civil Claim Respondent Solicitor one Email");
            return updateSolDetails(civilCaseData.getRespondentSolicitor1EmailAddress(), generalAppSolicitor);
        }

        if (YES.equals(gaCaseData.getIsMultiParty())) {

            boolean isGASolicitorEmailMatchWithCivilRespondent2Sol = generalAppSolicitor.getEmail()
                .equals(civilCaseData.getRespondentSolicitor1EmailAddress());
            String civilClaimRespondent2OrgId = civilCaseData.getRespondent2OrganisationPolicy()
                .getOrganisation().getOrganisationID();
            // civil claim defendant 2
            if (civilClaimRespondent2OrgId.equals(generalAppSolicitor.getOrganisationIdentifier())
                && isGASolicitorEmailMatchWithCivilRespondent2Sol) {
                log.info("Update GA Solicitor Email ID as same as Civil Claim Respondent Solicitor Two Email");
                return updateSolDetails(civilCaseData.getRespondentSolicitor2EmailAddress(), generalAppSolicitor);
            }
        }

        return generalAppSolicitor;

    }

    public CaseData validateSolicitorEmail(CaseData civilCaseData, CaseData gaCaseData,
                                           CaseData.CaseDataBuilder caseDataBuilder) {

        // GA Applicant solicitor
        caseDataBuilder.generalAppApplnSolicitor(checkIfOrgIDMatch(gaCaseData.getGeneralAppApplnSolicitor(),
                                                                   civilCaseData, gaCaseData));

        // GA Respondent solicitor
        List<Element<GASolicitorDetailsGAspec>> generalAppRespondentSolicitors = newArrayList();

        gaCaseData.getGeneralAppRespondentSolicitors().forEach(rs -> {
            generalAppRespondentSolicitors
                .add(element(checkIfOrgIDMatch(rs.getValue(), civilCaseData, gaCaseData)));
        });

        caseDataBuilder.generalAppRespondentSolicitors(generalAppRespondentSolicitors.isEmpty()
                                                           ? gaCaseData.getGeneralAppRespondentSolicitors()
                                                           : generalAppRespondentSolicitors);

        return caseDataBuilder.build();
    }
}
