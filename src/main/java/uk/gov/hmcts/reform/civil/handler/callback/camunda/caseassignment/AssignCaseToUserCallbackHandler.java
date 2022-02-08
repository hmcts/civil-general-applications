package uk.gov.hmcts.reform.civil.handler.callback.camunda.caseassignment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.CaseRole;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.IdamUserDetails;
import uk.gov.hmcts.reform.civil.service.CoreCaseUserService;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.ASSIGN_GA_ROLES;

@Service
@RequiredArgsConstructor
public class AssignCaseToUserCallbackHandler extends CallbackHandler {

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
    public String camundaActivityId(CallbackParams callbackParams) {
        return TASK_ID;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private boolean applicationSolicitorDetailsExist(CaseData caseData) {
        return caseData.getApplicantSolicitor1UserDetails() != null
            && caseData.getApplicant1OrganisationPolicy() != null;
    }

    private CallbackResponse assignSolicitorCaseRole(CallbackParams callbackParams) {
        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        String caseId = caseData.getCcdCaseReference().toString();

        if (applicationSolicitorDetailsExist(caseData)) {
            IdamUserDetails userDetails = caseData.getApplicantSolicitor1UserDetails();
            String submitterId = userDetails.getId();
            String organisationId = caseData
                    .getApplicant1OrganisationPolicy().getOrganisation().getOrganisationID();
            coreCaseUserService.assignCase(caseId, submitterId, organisationId, CaseRole.APPLICANTSOLICITORONE);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .build();
    }
}
