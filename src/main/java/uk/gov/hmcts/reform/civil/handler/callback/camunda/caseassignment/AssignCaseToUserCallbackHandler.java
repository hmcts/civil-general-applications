package uk.gov.hmcts.reform.civil.handler.callback.camunda.caseassignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.model.CaseAssignedUserRole;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.CaseRole;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.service.AssignCaseToResopondentSolHelper;
import uk.gov.hmcts.reform.civil.service.CoreCaseUserService;
import uk.gov.hmcts.reform.civil.service.OrganisationService;
import uk.gov.hmcts.reform.prd.model.Organisation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.ASSIGN_GA_ROLES;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.APPLICANTSOLICITORONE;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORONE;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORTWO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignCaseToUserCallbackHandler extends CallbackHandler {

    private final AssignCaseToResopondentSolHelper assignCaseToResopondentSolHelper;

    private final ObjectMapper mapper;

    private final OrganisationService organisationService;
    private static final List<CaseEvent> EVENTS = List.of(ASSIGN_GA_ROLES);
    public static final String TASK_ID = "AssigningOfRoles";

    private final CoreCaseUserService coreCaseUserService;
    private final CaseDetailsConverter caseDetailsConverter;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::assignSolicitorCaseRole
        );
    }

    @Override
    public String camundaActivityId() {
        return TASK_ID;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse assignSolicitorCaseRole(CallbackParams callbackParams) {

        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        String caseId = caseData.getCcdCaseReference().toString();
        String parentCaseId = caseData.getGeneralAppParentCaseLink().getCaseReference();

        List<String> errors = new ArrayList<>();

        try {

            GASolicitorDetailsGAspec applicantSolicitor = caseData.getGeneralAppApplnSolicitor();

            coreCaseUserService.assignCase(caseId, applicantSolicitor.getId(),
                                           applicantSolicitor.getOrganisationIdentifier(), APPLICANTSOLICITORONE
            );

            List<CaseAssignedUserRole> assignedMainCaseUserRoles = coreCaseUserService.getUserRoles(parentCaseId).getCaseAssignedUserRoles();
            List<CaseAssignedUserRole> assignedChildCaseUserRoles = coreCaseUserService.getUserRoles(caseId).getCaseAssignedUserRoles();
            List<String> childCaseAssignedUserIds = assignedChildCaseUserRoles.stream().map(CaseAssignedUserRole::getUserId).toList();
            List<String> mainCaseAssignedUserIds = assignedMainCaseUserRoles.stream().map(CaseAssignedUserRole::getUserId).toList();
            List<String> unAssignedUserIds = getUnAssignedUserIds(mainCaseAssignedUserIds, childCaseAssignedUserIds);

            for (String id : unAssignedUserIds) {
                String orgId = organisationService.findOrganisationByUserId(id)
                    .map(Organisation::getOrganisationIdentifier).orElse(null);
                if (orgId != null && orgId.equals(applicantSolicitor.getOrganisationIdentifier())) {

                    assignRoleToChildCase(APPLICANTSOLICITORONE, caseId, id, orgId);
                    caseData.getGeneralAppRespondentSolicitors().removeIf(user -> Objects.equals(user.getValue().getId(), id));
                }

            }

            /*
             * Don't assign the case to respondent solicitors if GA is without notice
             * */
            if (ofNullable(caseData.getGeneralAppInformOtherParty()).isPresent()
                && YES.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice())) {

                assignCaseToResopondentSolHelper.assignCaseToRespondentSolicitor(caseData, caseId);

                List<CaseAssignedUserRole> assignedGaCaseUserRoles = coreCaseUserService.getUserRoles(caseId).getCaseAssignedUserRoles();
                List<String> childCaseAppAssignedUserIds = assignedGaCaseUserRoles.stream().map(CaseAssignedUserRole::getUserId).toList();
                List<String> unAssignedRespUserIds = getUnAssignedUserIds(mainCaseAssignedUserIds, childCaseAppAssignedUserIds);
                for (String id : unAssignedRespUserIds) {
                    String organisationId = organisationService.findOrganisationByUserId(id)
                        .map(Organisation::getOrganisationIdentifier).orElse(null);
                    if (organisationId != null
                        && organisationId.equalsIgnoreCase(caseData.getGeneralAppRespondentSolicitors().get(0).getValue()
                                                               .getOrganisationIdentifier())) {
                        assignRoleToChildCase(RESPONDENTSOLICITORONE, caseId, id, organisationId);
                    } else if (organisationId != null
                        && organisationId.equalsIgnoreCase(caseData.getGeneralAppRespondentSolicitors().get(1).getValue()
                                                              .getOrganisationIdentifier())) {
                        assignRoleToChildCase(RESPONDENTSOLICITORTWO, caseId, id, organisationId);
                    }
                }
            }

            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

            return AboutToStartOrSubmitCallbackResponse.builder().data(caseDataBuilder.build().toMap(mapper)).errors(
                    errors)
                .build();

        } catch (Exception e) {
            log.error(e.toString());
            throw e;
        }
    }

    private List<String> getUnAssignedUserIds(List<String> mainCaseAssignedUserIds, List<String> childCaseAssignedUserIds) {
        return mainCaseAssignedUserIds.stream().filter(mainCaseUserId -> childCaseAssignedUserIds
            .parallelStream().noneMatch(childCaseUserId -> childCaseUserId.equals(mainCaseUserId))).toList();
    }

    private void assignRoleToChildCase(CaseRole caseAssignedUserRole, String caseId, String userId, String orgId) {
        coreCaseUserService.assignCaseToUser(caseAssignedUserRole, caseId, userId, orgId);
    }

}
