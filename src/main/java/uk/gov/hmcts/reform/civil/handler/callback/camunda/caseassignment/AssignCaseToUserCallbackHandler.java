package uk.gov.hmcts.reform.civil.handler.callback.camunda.caseassignment;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAccessDataStoreApi;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.model.CaseAssignedUserRole;
import uk.gov.hmcts.reform.ccd.model.CaseAssignedUserRolesResource;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.config.CrossAccessUserConfiguration;
import uk.gov.hmcts.reform.civil.enums.CaseRole;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.IdamUserDetails;
import uk.gov.hmcts.reform.civil.service.CoreCaseUserService;
import uk.gov.hmcts.reform.civil.service.UserService;
import uk.gov.hmcts.reform.prd.client.OrganisationApi;
import uk.gov.hmcts.reform.prd.model.Organisation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.ASSIGN_GA_ROLES;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignCaseToUserCallbackHandler extends CallbackHandler {

    private final OrganisationApi organisationApi;
    private final CaseAccessDataStoreApi caseAccessDataStoreApi;
    private final UserService userService;
    private final CrossAccessUserConfiguration crossAccessUserConfiguration;
    private final AuthTokenGenerator authTokenGenerator;
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

        String parentCaseId = caseData.getGeneralAppParentCaseLink().getCaseReference();

        CaseAssignedUserRolesResource userRoles = caseAccessDataStoreApi.getUserRoles(
            getCaaAccessToken(),
            authTokenGenerator.generate(),
            List.of(parentCaseId)
        );

        IdamUserDetails userDetails = caseData.getCivilServiceUserRoles();
        String submitterId = userDetails.getId();
        Optional<Organisation> org = findOrganisation(callbackParams.getParams().get(BEARER_TOKEN).toString());

        List<CaseAssignedUserRole> applicantSolicitors = userRoles.getCaseAssignedUserRoles().stream()
            .filter(CA -> CA.getCaseRole().contentEquals(CaseRole.APPLICANTSOLICITORONE.getFormattedName())
                || CA.getCaseRole().contentEquals(CaseRole.APPLICANTSOLICITORTWO.getFormattedName()))
            .collect(Collectors.toList());

        List<CaseAssignedUserRole> respondentSolicitors = userRoles.getCaseAssignedUserRoles().stream()
            .filter(CA -> CA.getCaseRole().contentEquals(CaseRole.RESPONDENTSOLICITORONE.getFormattedName())
                || CA.getCaseRole().contentEquals(CaseRole.RESPONDENTSOLICITORTWO.getFormattedName()))
            .collect(Collectors.toList());

        if (applicantSolicitors.isEmpty() && respondentSolicitors.isEmpty()) {
            throw new IllegalArgumentException("Applicant and Respondent Solicitors should not be Null");
        }

        if (org.isPresent()) {
            String organisationId = org.get().getOrganisationIdentifier();

            if (!applicantSolicitors.isEmpty() && applicantSolicitors.stream().anyMatch(AS -> AS.getUserId().equals(
                submitterId))) {

                applicantSolicitors.stream().forEach((AS) -> {
                    coreCaseUserService
                        .assignCase(caseId, AS.getUserId(), organisationId, CaseRole.APPLICANTSOLICITORONE);
                });

                respondentSolicitors.stream().forEach((AS) -> {
                    coreCaseUserService
                        .assignCase(caseId, AS.getUserId(), organisationId, CaseRole.RESPONDENTSOLICITORONE);
                });
            } else if (!respondentSolicitors.isEmpty() && respondentSolicitors.stream()
                .anyMatch(AS -> AS.getUserId().equals(submitterId))) {

                applicantSolicitors.stream().forEach((AS) -> {
                    coreCaseUserService
                        .assignCase(caseId, AS.getUserId(), organisationId, CaseRole.RESPONDENTSOLICITORONE);
                });

                respondentSolicitors.stream().forEach((AS) -> {
                    coreCaseUserService
                        .assignCase(caseId, AS.getUserId(), organisationId, CaseRole.APPLICANTSOLICITORONE);
                });

            }
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .build();

    }

    private String getCaaAccessToken() {
        return userService.getAccessToken(
            crossAccessUserConfiguration.getUserName(),
            crossAccessUserConfiguration.getPassword()
        );
    }

    public Optional<Organisation> findOrganisation(String authToken) {
        try {
            return ofNullable(organisationApi.findUserOrganisation(authToken, authTokenGenerator.generate()));

        } catch (FeignException.NotFound | FeignException.Forbidden ex) {
            log.error("User not registered in MO", ex);
            return Optional.empty();
        }
    }
}
