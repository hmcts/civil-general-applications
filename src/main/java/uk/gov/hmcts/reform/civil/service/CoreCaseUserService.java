package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRoleWithOrganisation;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;
import uk.gov.hmcts.reform.civil.config.CrossAccessUserConfiguration;
import uk.gov.hmcts.reform.civil.enums.CaseRole;

import java.util.List;

import static uk.gov.hmcts.reform.civil.enums.CaseRole.CREATOR;

@Service
@RequiredArgsConstructor
public class CoreCaseUserService {

    Logger log = LoggerFactory.getLogger(CoreCaseUserService.class);

    private final CaseAssignmentApi caseAssignmentApi;
    private final UserService userService;
    private final CrossAccessUserConfiguration crossAccessUserConfiguration;
    private final AuthTokenGenerator authTokenGenerator;

    public void assignCase(String caseId, String userId, String organisationId, CaseRole caseRole) {
        String caaAccessToken = getCaaAccessToken();

        if (!userHasCaseRole(caseId, caaAccessToken, caseRole)) {
            assignUserToCaseForRole(caseId, userId, organisationId, caseRole, caaAccessToken);
        } else {
            log.info("Case already have the user with {} role", caseRole.getFormattedName());
        }
    }

    public void removeCreatorRoleCaseAssignment(String caseId, String userId, String organisationId) {

        String caaAccessToken = getCaaAccessToken();

        if (userWithCaseRoleExistsOnCase(caseId, caaAccessToken, CREATOR)) {
            removeCreatorAccess(caseId, userId, organisationId, caaAccessToken);
        } else {
            log.info("User doesn't have {} role", CREATOR.getFormattedName());
        }
    }

    public boolean userHasCaseRole(String caseId, String userId, CaseRole caseRole) {
        CaseAssignmentUserRolesResource userRoles = caseAssignmentApi.getUserRoles(
            getCaaAccessToken(),
            authTokenGenerator.generate(),
            List.of(caseId)
        );

        return userRoles.getCaseAssignmentUserRoles().stream()
            .filter(c -> c.getUserId().equals(userId))
            .anyMatch(c -> c.getCaseRole().equals(caseRole.getFormattedName()));
    }

    public boolean userHasAnyCaseRole(String caseId, String userId, String caseRole) {
        CaseAssignmentUserRolesResource userRoles = caseAssignmentApi.getUserRoles(
            getCaaAccessToken(),
            authTokenGenerator.generate(),
            List.of(caseId)
        );

        return userRoles.getCaseAssignmentUserRoles().stream()
            .filter(c -> c.getUserId().equals(userId))
            .anyMatch(c -> c.getCaseRole().equals(caseRole));
    }

    private String getCaaAccessToken() {
        return userService.getAccessToken(
            crossAccessUserConfiguration.getUserName(),
            crossAccessUserConfiguration.getPassword()
        );
    }

    public void assignUserToCaseForRole(String caseId, String userId, String organisationId,
                                        CaseRole caseRole, String caaAccessToken) {
        CaseAssignmentUserRoleWithOrganisation caseAssignedUserRoleWithOrganisation
            = CaseAssignmentUserRoleWithOrganisation.builder()
            .caseDataId(caseId)
            .userId(userId)
            .caseRole(caseRole.getFormattedName())
            .organisationId(organisationId)
            .build();

        caseAssignmentApi.addCaseUserRoles(
            caaAccessToken,
            authTokenGenerator.generate(),
            CaseAssignmentUserRolesRequest.builder()
                .caseAssignmentUserRolesWithOrganisation(List.of(caseAssignedUserRoleWithOrganisation))
                .build()
        );
    }

    public CaseAssignmentUserRolesResource getUserRoles(String caseId) {

        return caseAssignmentApi.getUserRoles(
            getCaaAccessToken(),
            authTokenGenerator.generate(),
            List.of(caseId)
        );

    }

    private void removeCreatorAccess(String caseId, String userId, String organisationId, String caaAccessToken) {
        CaseAssignmentUserRoleWithOrganisation caseAssignedUserRoleWithOrganisation
            = CaseAssignmentUserRoleWithOrganisation.builder()
            .caseDataId(caseId)
            .userId(userId)
            .caseRole(CREATOR.getFormattedName())
            .organisationId(organisationId)
            .build();

        caseAssignmentApi.removeCaseUserRoles(
            caaAccessToken,
            authTokenGenerator.generate(),
            CaseAssignmentUserRolesRequest.builder()
                .caseAssignmentUserRolesWithOrganisation(List.of(caseAssignedUserRoleWithOrganisation))
                .build()
        );
    }

    public boolean userWithCaseRoleExistsOnCase(String caseId, String accessToken, CaseRole caseRole) {
        CaseAssignmentUserRolesResource userRoles = caseAssignmentApi.getUserRoles(
            accessToken,
            authTokenGenerator.generate(),
            List.of(caseId)
        );

        return userRoles.getCaseAssignmentUserRoles().stream()
            .anyMatch(c -> c.getCaseRole().equals(caseRole.getFormattedName()));
    }
}
