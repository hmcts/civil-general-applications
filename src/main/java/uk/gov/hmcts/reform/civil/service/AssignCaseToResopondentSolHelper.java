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

            List<Element<GASolicitorDetailsGAspec>>  respondentSolList = caseData.getGeneralAppRespondentSolicitors().stream()
                .filter(userOrgId -> !(userOrgId.getValue().getOrganisationIdentifier()
                    .equalsIgnoreCase(caseData.getGeneralAppApplnSolicitor().getOrganisationIdentifier()))).toList();
            GASolicitorDetailsGAspec respondentSolicitor1 =
                respondentSolList.get(FIRST_SOLICITOR).getValue();
            coreCaseUserService.assignCase(caseId, respondentSolicitor1.getId(),
                                           respondentSolicitor1.getOrganisationIdentifier(), RESPONDENTSOLICITORONE);
            for (Element<GASolicitorDetailsGAspec> respSolElement : respondentSolList) {
                if ((respondentSolicitor1.getOrganisationIdentifier() != null && respondentSolicitor1.getOrganisationIdentifier()
                    .equalsIgnoreCase(respSolElement.getValue().getOrganisationIdentifier()))) {
                    coreCaseUserService
                        .assignCase(caseId, respSolElement.getValue().getId(),
                                    respSolElement.getValue().getOrganisationIdentifier(),
                                    RESPONDENTSOLICITORONE);
                } else if (caseData.getIsMultiParty().equals(YesOrNo.YES)
                    && !(respondentSolicitor1.getOrganisationIdentifier() != null && respondentSolicitor1.getOrganisationIdentifier()
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
