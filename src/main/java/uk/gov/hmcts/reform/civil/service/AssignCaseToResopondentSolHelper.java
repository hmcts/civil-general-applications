package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;

import java.util.List;

import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORONE;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORTWO;

@Service
@RequiredArgsConstructor
public class AssignCaseToResopondentSolHelper {

    private final CoreCaseUserService coreCaseUserService;

    private static final int FIRST_SOLICITOR = 0;

    public void assignCaseToRespondentSolicitor(CaseData caseData, String caseId) {

        /*
         * Assign case respondent solicitors if judge uncloak the application
         * */
        if (!CollectionUtils.isEmpty(caseData.getGeneralAppRespondentSolicitors())) {
            GASolicitorDetailsGAspec respondentSolicitor1 =
                caseData.getGeneralAppRespondentSolicitors().get(FIRST_SOLICITOR).getValue();

            coreCaseUserService.assignCase(caseId, respondentSolicitor1.getId(),
                                respondentSolicitor1.getOrganisationIdentifier(), RESPONDENTSOLICITORONE);
        }

        if (caseData.getGeneralAppRespondentSolicitors().size() > 1 && caseData.getIsMultiParty().equals(YesOrNo.YES)) {
            GASolicitorDetailsGAspec respondentSolicitor1 =
                    caseData.getGeneralAppRespondentSolicitors().get(FIRST_SOLICITOR).getValue();
            List<Element<GASolicitorDetailsGAspec>> respondentSolList = caseData.getGeneralAppRespondentSolicitors();
            for (Element<GASolicitorDetailsGAspec> respSolElement : respondentSolList) {
                if (!(respondentSolicitor1.getOrganisationIdentifier() != null && respondentSolicitor1.getOrganisationIdentifier()
                    .equalsIgnoreCase(respSolElement.getValue().getOrganisationIdentifier()))) {
                    coreCaseUserService
                        .assignCase(caseId, respSolElement.getValue().getId(),
                                    respSolElement.getValue().getOrganisationIdentifier(),
                                    RESPONDENTSOLICITORTWO);
                }

            }
        }
    }
}
