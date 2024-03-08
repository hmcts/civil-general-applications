package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAccessDataStoreApi;
import uk.gov.hmcts.reform.ccd.model.AddCaseAssignedUserRolesRequest;
import uk.gov.hmcts.reform.ccd.model.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.reform.ccd.model.CaseAssignedUserRolesRequest;
import uk.gov.hmcts.reform.ccd.model.CaseAssignedUserRolesResource;
import uk.gov.hmcts.reform.civil.config.CrossAccessUserConfiguration;
import uk.gov.hmcts.reform.civil.enums.CaseRole;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.prd.model.Organisation;

import java.util.List;

import static uk.gov.hmcts.reform.civil.enums.CaseRole.CREATOR;

@Service
@RequiredArgsConstructor
public class CoreCaseUserService {

    Logger log = LoggerFactory.getLogger(CoreCaseUserService.class);

    private final CaseAccessDataStoreApi caseAccessDataStoreApi;
    private final IdamClient idamClient;
    private final CrossAccessUserConfiguration crossAccessUserConfiguration;
    private final UserService userService;
    private final OrganisationService organisationService;
    private final AuthTokenGenerator authTokenGenerator;

    public void assignCase(String caseId, String userId, String organisationId, CaseRole caseRole) {
        String caaAccessToken = getCaaAccessToken();

        if (!userHasCaseRole(caseId, caaAccessToken, caseRole)) {
            assignUserToCaseForRole(caseId, userId, organisationId, caseRole, caaAccessToken);
        } else {
            log.info("Case already have the user with {} role", caseRole.getFormattedName());
        }
    }

    public void assignCaseToUser(CaseRole caseRole, String caseId, String userId,  String organisationId) {

        String caaAccessToken = getCaaAccessToken();
        if (!userHasAnyCaseRole(caseId, userId, caseRole.getFormattedName())) {
            assignUserToChildCaseForRole(caseId, userId, organisationId,  caseRole.getFormattedName(), caaAccessToken);
        }

    }

    private String getOrganisationId(String authorisation) {
        String id = null;
        try {
            id = organisationService.findOrganisation(authorisation)
                .map(Organisation::getOrganisationIdentifier).orElse(null);
        } catch (Exception e) {
            log.error("Error getting organisation id", e);
        }
        return id;
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
        CaseAssignedUserRolesResource userRoles = caseAccessDataStoreApi.getUserRoles(
            getCaaAccessToken(),
            authTokenGenerator.generate(),
            List.of(caseId)
        );

        return userRoles.getCaseAssignedUserRoles().stream()
            .filter(c -> c.getUserId().equals(userId))
            .anyMatch(c -> c.getCaseRole().equals(caseRole.getFormattedName()));
    }

    public boolean userHasAnyCaseRole(String caseId, String userId,    String caseRole) {
        CaseAssignedUserRolesResource userRoles = caseAccessDataStoreApi.getUserRoles(
            getCaaAccessToken(),
            authTokenGenerator.generate(),
            List.of(caseId)
        );

        return userRoles.getCaseAssignedUserRoles().stream()
            .filter(c -> c.getUserId().equals(userId))
            .anyMatch(c -> c.getCaseRole().equals(caseRole));
    }

    private String getCaaAccessToken() {
        return idamClient.getAccessToken(
            crossAccessUserConfiguration.getUserName(),
            crossAccessUserConfiguration.getPassword()
        );
    }

    public void assignUserToChildCaseForRole(String caseId, String userId, String organisationId,
                                        String caseRole, String caaAccessToken) {
        CaseAssignedUserRoleWithOrganisation caseAssignedUserRoleWithOrganisation
            = CaseAssignedUserRoleWithOrganisation.builder()
            .caseDataId(caseId)
            .userId(userId)
            .caseRole(caseRole)
            .organisationId(organisationId)
            .build();

        caseAccessDataStoreApi.addCaseUserRoles(
            caaAccessToken,
            authTokenGenerator.generate(),
            AddCaseAssignedUserRolesRequest.builder()
                .caseAssignedUserRoles(List.of(caseAssignedUserRoleWithOrganisation))
                .build()
        );
    }

    public void assignUserToCaseForRole(String caseId, String userId, String organisationId,
                                         CaseRole caseRole, String caaAccessToken) {
        CaseAssignedUserRoleWithOrganisation caseAssignedUserRoleWithOrganisation
            = CaseAssignedUserRoleWithOrganisation.builder()
            .caseDataId(caseId)
            .userId(userId)
            .caseRole(caseRole.getFormattedName())
            .organisationId(organisationId)
            .build();

        caseAccessDataStoreApi.addCaseUserRoles(
            caaAccessToken,
            authTokenGenerator.generate(),
            AddCaseAssignedUserRolesRequest.builder()
                .caseAssignedUserRoles(List.of(caseAssignedUserRoleWithOrganisation))
                .build()
        );
    }

    public CaseAssignedUserRolesResource getUserRoles(String caseId) {

        return caseAccessDataStoreApi.getUserRoles(
            getCaaAccessToken(),
            authTokenGenerator.generate(),
            List.of(caseId)
        );

    }

    private void removeCreatorAccess(String caseId, String userId, String organisationId, String caaAccessToken) {
        CaseAssignedUserRoleWithOrganisation caseAssignedUserRoleWithOrganisation
            = CaseAssignedUserRoleWithOrganisation.builder()
            .caseDataId(caseId)
            .userId(userId)
            .caseRole(CREATOR.getFormattedName())
            .organisationId(organisationId)
            .build();

        caseAccessDataStoreApi.removeCaseUserRoles(
            caaAccessToken,
            authTokenGenerator.generate(),
            CaseAssignedUserRolesRequest.builder()
                .caseAssignedUserRoles(List.of(caseAssignedUserRoleWithOrganisation))
                .build()
        );
    }

    public boolean userWithCaseRoleExistsOnCase(String caseId, String accessToken, CaseRole caseRole) {
        CaseAssignedUserRolesResource userRoles = caseAccessDataStoreApi.getUserRoles(
            accessToken,
            authTokenGenerator.generate(),
            List.of(caseId)
        );

        return userRoles.getCaseAssignedUserRoles().stream()
            .anyMatch(c -> c.getCaseRole().equals(caseRole.getFormattedName()));
    }

}
