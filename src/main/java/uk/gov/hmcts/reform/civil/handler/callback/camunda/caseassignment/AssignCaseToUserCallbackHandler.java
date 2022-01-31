package uk.gov.hmcts.reform.civil.handler.callback.camunda.caseassignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackException;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.CaseRole;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.IdamUserDetails;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
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
    private final CoreCaseDataService coreCaseDataService;
    private final ObjectMapper objectMapper;

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

    private Boolean checkGAExitsWithSolicitorDetails(Element<GeneralApplication> generalApplication) {
        return generalApplication.getValue().getApplicantSolicitor1UserDetails() != null
            && generalApplication.getValue().getApplicant1OrganisationPolicy() != null
            && generalApplication.getValue().getRespondent1OrganisationPolicy() != null
            && generalApplication.getValue().getRespondentSolicitor1EmailAddress() != null;
    }

    private void setSolicitorEmailID(Element<GeneralApplication> generalApplication, IdamUserDetails userDetails) {
        generalApplication.getValue().toBuilder()
            .applicantSolicitor1UserDetails(IdamUserDetails.builder()
                                                .email(userDetails.getEmail()).build()).build();
    }

    private CaseDataContent caseDataContent(StartEventResponse startEventResponse,
                                            List<Element<GeneralApplication>> generalApplications) {
        Map<String, Object> data = startEventResponse.getCaseDetails().getData();
        data.put("generalApplications", generalApplications);

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder().id(startEventResponse.getEventId()).build())
            .data(data)
            .build();
    }

    private CallbackResponse assignSolicitorCaseRole(CallbackParams callbackParams) {
        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        String caseId = caseData.getCcdCaseReference().toString();

        if (caseData.getGeneralApplications() != null) {
            List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
            for (Element<GeneralApplication> generalApplication : generalApplications) {
                if (checkGAExitsWithSolicitorDetails(generalApplication)) {
                    IdamUserDetails userDetails = generalApplication.getValue().getApplicantSolicitor1UserDetails();
                    String submitterId = userDetails.getId();
                    String organisationId = generalApplication.getValue()
                            .getApplicant1OrganisationPolicy().getOrganisation().getOrganisationID();

                    coreCaseUserService.assignCase(caseId, submitterId, organisationId, CaseRole.APPLICANTSOLICITORONE);
                    coreCaseUserService.removeCreatorRoleCaseAssignment(caseId, submitterId, organisationId);

                    setSolicitorEmailID(generalApplication, userDetails);
                }
            }
            return updateCaseDate(callbackParams, generalApplications);
        }

        throw new CallbackException(String.format("AssignCaseToUserCallbackHandler::assignSolicitorCaseRole "
                                                      + "NullPointer Exception : %s", caseId));

    }

    private CallbackResponse updateCaseDate(CallbackParams callbackParams,
                                            List<Element<GeneralApplication>> generalApplications) {
        Map<String, Object> output = callbackParams.getRequest().getCaseDetails().getData();
        output.put("generalApplications", generalApplications);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(output)
            .build();
    }
}
