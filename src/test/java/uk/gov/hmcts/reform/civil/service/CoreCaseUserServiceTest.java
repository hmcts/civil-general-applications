package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRole;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRoleWithOrganisation;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;
import uk.gov.hmcts.reform.civil.config.CrossAccessUserConfiguration;
import uk.gov.hmcts.reform.civil.enums.CaseRole;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    CoreCaseUserService.class
})
class CoreCaseUserServiceTest {

    private static final String CAA_USER_AUTH_TOKEN = "Bearer caa-user-xyz";
    private static final String SERVICE_AUTH_TOKEN = "Bearer service-xyz";
    private static final String CASE_ID = "1";
    private static final String USER_ID = "User1";
    private static final String USER_ID2 = "User2";
    public static final String ORG_ID = "62LYJRF";

    @MockBean
    private CrossAccessUserConfiguration userConfig;

    @MockBean
    private CaseAssignmentApi caseAccessDataStoreApi;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private CoreCaseUserService service;

    @BeforeEach
    void init() {
        clearInvocations(authTokenGenerator);
        clearInvocations(userService);
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(userService.getAccessToken(userConfig.getUserName(), userConfig.getPassword())).thenReturn(
            CAA_USER_AUTH_TOKEN);
    }

    @Nested
    class AssignCase {

        @Test
        void shouldAssignCaseToUser_WhenSameUserWithRequestedCaseRoleDoesNotExist() {
            when(caseAccessDataStoreApi.getUserRoles(CAA_USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, List.of(CASE_ID)))
                .thenReturn(CaseAssignmentUserRolesResource.builder().caseAssignmentUserRoles(List.of()).build());

            service.assignCase(CASE_ID, USER_ID, ORG_ID, CaseRole.APPLICANTSOLICITORONE);
            assertThat(service.userHasCaseRole(CASE_ID, USER_ID,
                                               CaseRole.APPLICANTSOLICITORONE
            )).isFalse();
            verify(caseAccessDataStoreApi).addCaseUserRoles(
                CAA_USER_AUTH_TOKEN,
                SERVICE_AUTH_TOKEN,
                getAddCaseAssignmentUserRolesRequest(CaseRole.APPLICANTSOLICITORONE)
            );
        }

        @Test
        void shouldNotAssignCaseToUser_WhenSameUserWithRequestedCaseRoleExist() {
            CaseAssignmentUserRole caseAssignedUserRole = CaseAssignmentUserRole.builder()
                .userId(CAA_USER_AUTH_TOKEN)
                .caseRole(CaseRole.APPLICANTSOLICITORONE.getFormattedName())
                .build();
            when(caseAccessDataStoreApi.getUserRoles(CAA_USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, List.of(CASE_ID)))
                .thenReturn(CaseAssignmentUserRolesResource.builder().caseAssignmentUserRoles(List.of(caseAssignedUserRole)).build());

            service.assignCase(CASE_ID, USER_ID, ORG_ID, CaseRole.APPLICANTSOLICITORONE);
            assertThat(service.userHasCaseRole(CASE_ID, USER_ID,
                                               CaseRole.APPLICANTSOLICITORONE
            )).isFalse();

        }

        @Test
        void shouldNotAssignCaseToUser_WhenSameUserWithRequestedCaseRoleAlreadyExist() {
            CaseAssignmentUserRole caseAssignedUserRole = CaseAssignmentUserRole.builder()
                .userId(USER_ID)
                .caseRole(CaseRole.RESPONDENTSOLICITORONE.getFormattedName())
                .build();

            when(caseAccessDataStoreApi.getUserRoles(CAA_USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, List.of(CASE_ID)))
                .thenReturn(CaseAssignmentUserRolesResource.builder().caseAssignmentUserRoles(List.of(caseAssignedUserRole))
                                .build());

            service.assignCase(CASE_ID, USER_ID, ORG_ID, CaseRole.APPLICANTSOLICITORONE);

            verify(caseAccessDataStoreApi, never()).addCaseUserRoles(
                CAA_USER_AUTH_TOKEN,
                SERVICE_AUTH_TOKEN,
                getAddCaseAssignmentUserRolesRequest(CaseRole.RESPONDENTSOLICITORONE)
            );
        }

        private CaseAssignmentUserRolesRequest getAddCaseAssignmentUserRolesRequest(CaseRole caseRole) {
            CaseAssignmentUserRoleWithOrganisation caseAssignedUserRoleWithOrganisation
                = CaseAssignmentUserRoleWithOrganisation.builder()
                .caseDataId(CASE_ID)
                .userId(USER_ID)
                .caseRole(caseRole.getFormattedName())
                .organisationId(ORG_ID)
                .build();

            return CaseAssignmentUserRolesRequest.builder()
                .caseAssignmentUserRolesWithOrganisation(List.of(caseAssignedUserRoleWithOrganisation))
                .build();
        }

    }

    @Nested
    class RemoveCaseAssignment {

        @Test
        void shouldRemoveCaseAssignmentToUser_WhenUserWithCaseRoleAlreadyExist() {
            CaseAssignmentUserRole caseAssignedUserRole = CaseAssignmentUserRole.builder()
                .userId(USER_ID)
                .caseRole(CaseRole.CREATOR.getFormattedName())
                .build();

            when(caseAccessDataStoreApi.getUserRoles(CAA_USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, List.of(CASE_ID)))
                .thenReturn(CaseAssignmentUserRolesResource.builder().caseAssignmentUserRoles(List.of(caseAssignedUserRole))
                                .build());

            service.removeCreatorRoleCaseAssignment(CASE_ID, USER_ID, ORG_ID);

            verify(caseAccessDataStoreApi).removeCaseUserRoles(
                CAA_USER_AUTH_TOKEN,
                SERVICE_AUTH_TOKEN,
                getCaseAssignmentUserRolesRequest(CaseRole.CREATOR)
            );
        }

        @Test
        void shouldNotRemoveCaseAssignmentToUser_WhenUserWithCaseRoleDoesNotExist() {
            CaseAssignmentUserRole caseAssignedUserRole
                = CaseAssignmentUserRole.builder().userId(USER_ID)
                .caseRole(CaseRole.APPLICANTSOLICITORONE.getFormattedName())
                .build();

            when(caseAccessDataStoreApi.getUserRoles(CAA_USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, List.of(CASE_ID)))
                .thenReturn(CaseAssignmentUserRolesResource.builder().caseAssignmentUserRoles(List.of(caseAssignedUserRole))
                                .build());

            service.removeCreatorRoleCaseAssignment(CASE_ID, USER_ID, ORG_ID);

            verify(caseAccessDataStoreApi, never()).removeCaseUserRoles(
                CAA_USER_AUTH_TOKEN,
                SERVICE_AUTH_TOKEN,
                getCaseAssignmentUserRolesRequest(CaseRole.CREATOR)
            );
        }

        private CaseAssignmentUserRolesRequest getCaseAssignmentUserRolesRequest(CaseRole caseRole) {
            CaseAssignmentUserRoleWithOrganisation caseAssignedUserRoleWithOrganisation
                = CaseAssignmentUserRoleWithOrganisation.builder()
                .caseDataId(CASE_ID)
                .userId(USER_ID)
                .caseRole(caseRole.getFormattedName())
                .organisationId(ORG_ID)
                .build();

            return CaseAssignmentUserRolesRequest.builder()
                .caseAssignmentUserRolesWithOrganisation(List.of(caseAssignedUserRoleWithOrganisation))
                .build();
        }
    }

    @Nested
    class UserHasCaseRole {

        @BeforeEach
        void setup() {
            when(userService.getAccessToken(userConfig.getUserName(), userConfig.getPassword())).thenReturn(
                CAA_USER_AUTH_TOKEN);
            CaseAssignmentUserRolesResource caseAssignedUserRolesResource = CaseAssignmentUserRolesResource.builder()
                .caseAssignmentUserRoles(List.of(
                    CaseAssignmentUserRole.builder()
                        .userId(USER_ID)
                        .caseRole(CaseRole.RESPONDENTSOLICITORONE.getFormattedName())
                        .build(),
                    CaseAssignmentUserRole.builder()
                        .userId(USER_ID2)
                        .caseRole(CaseRole.RESPONDENTSOLICITORTWO.getFormattedName())
                        .build()
                ))
                .build();
            when(caseAccessDataStoreApi.getUserRoles(CAA_USER_AUTH_TOKEN, SERVICE_AUTH_TOKEN, List.of(CASE_ID)))
                .thenReturn(caseAssignedUserRolesResource);
        }

        @Test
        void shouldReturnUserRoles_getUserRoles() {
            assertThat(service.getUserRoles(CASE_ID).getCaseAssignmentUserRoles()).hasSize(2);
        }

        @Test
        void shouldReturnTrue_whenCaseRoleAssignedToUser() {
            assertThat(service.userHasCaseRole(CASE_ID, USER_ID, CaseRole.RESPONDENTSOLICITORONE)).isTrue();
            assertThat(service.userHasCaseRole(CASE_ID, USER_ID2, CaseRole.RESPONDENTSOLICITORTWO)).isTrue();

            verify(caseAccessDataStoreApi, times(2)).getUserRoles(
                CAA_USER_AUTH_TOKEN,
                SERVICE_AUTH_TOKEN,
                List.of(CASE_ID)
            );
        }

        @Test
        void shouldReturnTrue_whenAnyCaseRoleAssignedToUser() {
            assertThat(service.userHasAnyCaseRole(CASE_ID, USER_ID,
                                                  CaseRole.RESPONDENTSOLICITORONE.getFormattedName()
            )).isTrue();
            assertThat(service.userHasAnyCaseRole(CASE_ID, USER_ID2,
                                                  CaseRole.RESPONDENTSOLICITORTWO.getFormattedName()
            )).isTrue();

            verify(caseAccessDataStoreApi, times(2)).getUserRoles(
                CAA_USER_AUTH_TOKEN,
                SERVICE_AUTH_TOKEN,
                List.of(CASE_ID)
            );
        }

        @Test
        void shouldReturnFalse_whenCaseRoleNotAssignedToUser() {
            assertThat(service.userHasCaseRole(CASE_ID, USER_ID, CaseRole.RESPONDENTSOLICITORTWO)).isFalse();
            assertThat(service.userHasCaseRole(CASE_ID, USER_ID2, CaseRole.RESPONDENTSOLICITORONE)).isFalse();

            verify(caseAccessDataStoreApi, times(2)).getUserRoles(
                CAA_USER_AUTH_TOKEN,
                SERVICE_AUTH_TOKEN,
                List.of(CASE_ID)
            );
        }

        @Test
        void shouldReturnFalse_whenAnyCaseRoleNotAssignedToUser() {
            assertThat(service.userHasAnyCaseRole(CASE_ID, USER_ID,
                                                  CaseRole.RESPONDENTSOLICITORTWO.getFormattedName()
            )).isFalse();
            assertThat(service.userHasAnyCaseRole(CASE_ID, USER_ID2,
                                                  CaseRole.RESPONDENTSOLICITORONE.getFormattedName()
            )).isFalse();

            verify(caseAccessDataStoreApi, times(2)).getUserRoles(
                CAA_USER_AUTH_TOKEN,
                SERVICE_AUTH_TOKEN,
                List.of(CASE_ID)
            );
        }
    }
}
