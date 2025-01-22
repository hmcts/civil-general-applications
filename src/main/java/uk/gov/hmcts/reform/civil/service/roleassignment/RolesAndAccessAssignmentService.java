package uk.gov.hmcts.reform.civil.service.roleassignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.config.SystemUpdateUserConfiguration;
import uk.gov.hmcts.reform.civil.ras.model.GrantType;
import uk.gov.hmcts.reform.civil.ras.model.RoleAssignment;
import uk.gov.hmcts.reform.civil.ras.model.RoleAssignmentRequest;
import uk.gov.hmcts.reform.civil.ras.model.RoleAssignmentResponse;
import uk.gov.hmcts.reform.civil.ras.model.RoleAssignmentServiceResponse;
import uk.gov.hmcts.reform.civil.ras.model.RoleCategory;
import uk.gov.hmcts.reform.civil.ras.model.RoleRequest;
import uk.gov.hmcts.reform.civil.ras.model.RoleType;
import uk.gov.hmcts.reform.civil.service.UserService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RolesAndAccessAssignmentService {

    public static final List<String> ROLE_TYPE = List.of("CASE");
    public static final List<String> ROLE_NAMES = List.of("allocated-judge", "lead-judge", "allocated-legal-adviser",
        "allocated-admin-caseworker", "allocated-ctsc-caseworker", "allocated-nbc-caseworker");
    private final RoleAssignmentsService roleAssignmentService;
    private final UserService userService;
    private final SystemUpdateUserConfiguration systemUserConfig;

    public void copyAllocatedRolesFromRolesAndAccess(String mainCaseId, String gaCaseId) {
        try {
            getCaseRoles(mainCaseId, gaCaseId, getSystemUserToken());
        } catch (Exception e) {
            log.error("Could not automatically copy and assign roles from Roles And Access", e);
        }
    }

    private void getCaseRoles(String mainCaseId, String gaCaseId, String bearerToken) {
        log.info("GET Assigned roles for {}, user id{}", bearerToken, userService.getUserInfo(bearerToken).getUid());
        RoleAssignmentServiceResponse roleAssignmentResponse = roleAssignmentService.queryRoleAssignmentsByCaseIdAndRole(mainCaseId, ROLE_TYPE, ROLE_NAMES, bearerToken);
        Optional.ofNullable(roleAssignmentResponse.getRoleAssignmentResponse())
            .ifPresentOrElse(response -> {
                if (response.isEmpty()) {
                    log.info("Role assignment list empty for case ID {}", mainCaseId);
                } else {
                    log.info("GET ROLES case id roleAssignmentResponse:  {}", response);
                    Map<String, List<RoleAssignmentResponse>> roleAssignmentsByActorId = response.stream()
                        .collect(Collectors.groupingBy(RoleAssignmentResponse::getActorId));

                    roleAssignmentsByActorId.forEach((actorId, actorRoles) ->
                                                         actorRoles.forEach(role -> assignRoles(gaCaseId, role, bearerToken))
                    );
                }
            }, () -> log.info("No role assignment response found for case ID {}", mainCaseId));
    }

    private void assignRoles(String gaCaseId, RoleAssignmentResponse roleToAssign, String bearerToken) {
        //TODO we will use system user to make assignment, currently unavailable, so temporary using hardcoded ID
        String systemUserId = userService.getUserInfo(bearerToken).getUid();

        roleAssignmentService.assignUserRoles(
            systemUserId,
            bearerToken,
            RoleAssignmentRequest.builder()
                .roleRequest(RoleRequest.builder()
                                 .assignerId(systemUserId)
                                 .replaceExisting(false)
                                 .build())
                .requestedRoles(buildRoleAssignments(gaCaseId, Collections.singletonList(roleToAssign.getActorId()), roleToAssign)).build());
        log.info("Assigned roles successfully");
    }

    private static RoleAssignment buildRoleAssignment(String gaCaseId, String userId, RoleAssignmentResponse roleToAssign) {

        Map<String, Object> roleAssignmentAttributes = new HashMap<>();
        roleAssignmentAttributes.put("caseId", gaCaseId);
        roleAssignmentAttributes.put("caseType", "GENERALAPPLICATION");
        roleAssignmentAttributes.put("jurisdiction", "CIVIL");
        if (roleToAssign.getAttributes().getContractType() != null) {
            roleAssignmentAttributes.put("contractType", roleToAssign.getAttributes().getContractType());
        }
        if (roleToAssign.getAttributes().getRegion() != null) {
            roleAssignmentAttributes.put("region", roleToAssign.getAttributes().getRegion());
        }
        if (roleToAssign.getAttributes().getPrimaryLocation() != null) {
            roleAssignmentAttributes.put("primaryLocation", roleToAssign.getAttributes().getPrimaryLocation());
        }

        return RoleAssignment.builder()
            .actorId(userId)
            .actorIdType(roleToAssign.getActorIdType())
            .grantType(GrantType.valueOf(roleToAssign.getGrantType()))
            .roleCategory(RoleCategory.valueOf(roleToAssign.getRoleCategory()))
            .roleType(RoleType.valueOf(roleToAssign.getRoleType()))
            .classification(roleToAssign.getClassification())
            .roleName(roleToAssign.getRoleName())
            .beginTime(roleToAssign.getBeginTime())
            .endTime(roleToAssign.getEndTime())
            .readOnly(false)
            .attributes(roleAssignmentAttributes)
            .build();
    }

    private static List<RoleAssignment> buildRoleAssignments(String gaCaseId, List<String> userIds, RoleAssignmentResponse roleToAssign) {
        return userIds.stream()
            .map(user -> buildRoleAssignment(gaCaseId, user, roleToAssign))
            .collect(Collectors.toList());
    }

    private String getSystemUserToken() {
        return userService.getAccessToken(systemUserConfig.getUserName(), systemUserConfig.getPassword());
    }

}
