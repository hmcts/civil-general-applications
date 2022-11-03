package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;

import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORONE;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORTWO;

@Service
@RequiredArgsConstructor
public class AssignCaseToResopondentSolHelper {

    private final CoreCaseUserService coreCaseUserService;

    private static final int FIRST_SOLICITOR = 0;
    private static final int SECOND_SOLICITOR = 1;

    public void assignCaseToRespondentSolicitor(CaseData caseData, String caseId) {

        /*
         * Assign case respondent solicitors if judge uncloak the application
         * */
        if (!CollectionUtils.isEmpty(caseData.getGeneralAppRespondentSolicitors())) {
            GASolicitorDetailsGAspec respondentSolicitor1 =
                caseData.getGeneralAppRespondentSolicitors().get(FIRST_SOLICITOR).getValue();

            coreCaseUserService
                .assignCase(caseId, respondentSolicitor1.getId(),
                            respondentSolicitor1.getOrganisationIdentifier(), RESPONDENTSOLICITORONE);

            if (caseData.getGeneralAppRespondentSolicitors().size() > 1) {

                GASolicitorDetailsGAspec respondentSolicitor2 =
                    caseData.getGeneralAppRespondentSolicitors().get(SECOND_SOLICITOR).getValue();

                coreCaseUserService
                    .assignCase(caseId, respondentSolicitor2.getId(),
                                respondentSolicitor2.getOrganisationIdentifier(),
                                RESPONDENTSOLICITORTWO);
            }
        }
    }
}
