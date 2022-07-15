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

    public CaseData validateSolicitorEmail(CaseData civilCaseData, CaseData gaCaseData, CaseData.CaseDataBuilder caseDataBuilder) {

        if (gaCaseData.getIsMultiParty() != YES) {
            if (gaCaseData.getParentClaimantIsApplicant() == YES) {
                // Check if the Civil Case Defendant Email is changed
                boolean isRespondentSolEmailChanged = gaCaseData.getGeneralAppRespondentSolicitors()
                    .stream().anyMatch(rs ->
                                           rs.getValue().getEmail()
                                               .equals(civilCaseData.getRespondentSolicitor1EmailAddress()));
                if (! isRespondentSolEmailChanged) {
                    List<Element<GASolicitorDetailsGAspec>> generalAppRespondentSolicitors = newArrayList();

                    log.info("Update GA Respondent Solicitor Email ID as same as Civil Claim Defendant Email");
                    // Update GA Respondent Solicitor Email ID as same as Civil Claim Defendant Email
                    generalAppRespondentSolicitors
                        .add(element(updateRespondentSolEmailAddress(civilCaseData
                                                                     .getRespondentSolicitor1EmailAddress(),
                                                                     gaCaseData.getGeneralAppRespondentSolicitors())));

                    caseDataBuilder.generalAppRespondentSolicitors(generalAppRespondentSolicitors);
                }

                // Check if the Civil Case Claimant Email is changed
                boolean isApplicantSolEmailChanged = gaCaseData.getGeneralAppApplnSolicitor().getEmail()
                    .equals(civilCaseData.getApplicantSolicitor1UserDetails().getEmail());

                if (! isApplicantSolEmailChanged) {

                    // Update GA Applicant Solicitor Email ID as same as Civil Claim claimant Solicitor Email
                    log.info("Update GA Applicant Solicitor Email ID as same as Civil Claim claimant Solicitor Email");
                    caseDataBuilder.generalAppApplnSolicitor(updateApplicantSolEmailAddress(
                        civilCaseData.getApplicantSolicitor1UserDetails().getEmail(),
                        gaCaseData.getGeneralAppApplnSolicitor()));

                }

                return caseDataBuilder.build();

            } else {
                // Check if the Civil Claim Claimant email is changed
                // Case : Civil Claim Claimant is the GA Respondent
                boolean isRespondentSolEmailChanged = gaCaseData.getGeneralAppRespondentSolicitors()
                    .stream().anyMatch(rs ->
                                           rs.getValue().getEmail()
                                               .equals(civilCaseData.getApplicantSolicitor1UserDetails().getEmail()));

                if (! isRespondentSolEmailChanged) {
                    List<Element<GASolicitorDetailsGAspec>> generalAppRespondentSolicitors = newArrayList();

                    // Update GA Respondent Solicitor Email ID as same as Civil Claim Claimant Solicitor Email
                    generalAppRespondentSolicitors
                        .add(element(
                            updateRespondentSolEmailAddress(civilCaseData
                                                                .getApplicantSolicitor1UserDetails().getEmail(),
                                                            gaCaseData.getGeneralAppRespondentSolicitors())));

                    caseDataBuilder.generalAppRespondentSolicitors(generalAppRespondentSolicitors);
                }

                // Check if the Civil Case defendant Email is changed
                boolean isApplicantSolEmailChanged = gaCaseData.getGeneralAppApplnSolicitor().getEmail()
                    .equals(civilCaseData.getRespondentSolicitor1EmailAddress());

                if (! isApplicantSolEmailChanged) {

                    // Update GA Applicant Solicitor email as same as Civil claim defendant solicitor email id
                    caseDataBuilder
                        .generalAppApplnSolicitor(updateApplicantSolEmailAddress(
                            civilCaseData.getRespondentSolicitor1EmailAddress(),
                            gaCaseData.getGeneralAppApplnSolicitor()));
                }
            }
        }
        return caseDataBuilder.build();
    }

    private GASolicitorDetailsGAspec updateRespondentSolEmailAddress(String updateEmail,
                                                                 List<Element<GASolicitorDetailsGAspec>>
                                                                     generalAppRespondentSolicitor) {
        GASolicitorDetailsGAspec.GASolicitorDetailsGAspecBuilder
            gaSolicitorDetailsGAspecBuilder = GASolicitorDetailsGAspec.builder();

        generalAppRespondentSolicitor.stream().forEach((rs) -> gaSolicitorDetailsGAspecBuilder.id(rs.getValue().getId())
            .forename(rs.getValue().getForename())
            .surname(rs.getValue().getSurname())
            .organisationIdentifier(rs.getValue().getOrganisationIdentifier())
            .civilClaimRole(rs.getValue().getCivilClaimRole())
            .email(updateEmail));

        return gaSolicitorDetailsGAspecBuilder.build();

    }

    private GASolicitorDetailsGAspec updateApplicantSolEmailAddress(String updateEmail, GASolicitorDetailsGAspec generalAppApplnSolicitor) {
        GASolicitorDetailsGAspec.GASolicitorDetailsGAspecBuilder
            gaSolicitorDetailsGAspecBuilder = GASolicitorDetailsGAspec.builder();

        gaSolicitorDetailsGAspecBuilder.id(generalAppApplnSolicitor.getId())
            .forename(generalAppApplnSolicitor.getForename())
            .surname(generalAppApplnSolicitor.getSurname())
            .organisationIdentifier(generalAppApplnSolicitor.getOrganisationIdentifier())
            .civilClaimRole(generalAppApplnSolicitor.getCivilClaimRole())
            .email(updateEmail);

        return gaSolicitorDetailsGAspecBuilder.build();

    }
}
