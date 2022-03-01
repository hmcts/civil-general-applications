package uk.gov.hmcts.reform.civil.handler.callback.caseassignment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.CaseAccessDataStoreApi;
import uk.gov.hmcts.reform.ccd.model.CaseAssignedUserRole;
import uk.gov.hmcts.reform.ccd.model.CaseAssignedUserRolesResource;
import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.config.CrossAccessUserConfiguration;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseRole;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.caseassignment.AssignCaseToUserCallbackHandler;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.IdamUserDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.service.CoreCaseUserService;
import uk.gov.hmcts.reform.prd.client.OrganisationApi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.SUMMARY_JUDGEMENT;

@SpringBootTest(classes = {
    AssignCaseToUserCallbackHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
public class AssignCaseToUserHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private AssignCaseToUserCallbackHandler assignCaseToUserHandler;

    @MockBean
    private CoreCaseUserService coreCaseUserService;

    @MockBean
    private OrganisationApi organisationApi;

    @MockBean
    private CaseAccessDataStoreApi caseAccessDataStoreApi;

    @MockBean
    private CrossAccessUserConfiguration crossAccessUserConfiguration;

    @Autowired
    private ObjectMapper objectMapper;

    private CallbackParams params;

    private GeneralApplication caseData;

    public static final Long CASE_ID = 1594901956117591L;

    public static final String STRING_CONSTANT = "This is string";

    @BeforeEach
    void setup() {

        GeneralApplication.GeneralApplicationBuilder builder = GeneralApplication.builder();
        builder.generalAppType(GAApplicationType.builder()
                                          .types(singletonList(SUMMARY_JUDGEMENT))
                                          .build())
            .generalAppParentCaseLink(GeneralAppParentCaseLink.builder().caseReference("12342341").build())
            .civilServiceUserRoles(IdamUserDetails.builder()
                                       .id("f5e5cc53-e065-43dd-8cec-2ad005a6b9a9")
                                       .email("applicant@someorg.com")
                                       .build())
            .applicantSolicitor1UserDetails(IdamUserDetails.builder()
                                                .id("f5e5cc53-e065-43dd-8cec-2ad005a6b9a9")
                                                .email("applicant@someorg.com")
                                                .build())
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .applicant1OrganisationPolicy(OrganisationPolicy.builder()
                                              .organisation(Organisation.builder().organisationID("OrgId1").build())
                                              .build())
            .respondent1OrganisationPolicy(OrganisationPolicy.builder()
                                               .organisation(Organisation.builder().organisationID("OrgId2").build())
                                               .build())

            .build();

        caseData = builder.build();

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);
    }

    public List<CaseAssignedUserRole> getCaseAssignedApplicantUserRoles() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = List.of(
            CaseAssignedUserRole.builder().caseDataId("1").userId("1")
                .caseRole(CaseRole.APPLICANTSOLICITORONE.getFormattedName()).build(),
            CaseAssignedUserRole.builder().caseDataId("1").userId("f5e5cc53-e065-43dd-8cec-2ad005a6b9a9")
                .caseRole(CaseRole.APPLICANTSOLICITORONE.getFormattedName()).build(),
            CaseAssignedUserRole.builder().caseDataId("1").userId("3")
                .caseRole(CaseRole.RESPONDENTSOLICITORONE.getFormattedName()).build(),
            CaseAssignedUserRole.builder().caseDataId("1").userId("4")
                .caseRole(CaseRole.RESPONDENTSOLICITORONE.getFormattedName()).build(),
            CaseAssignedUserRole.builder().caseDataId("1").userId("5")
                .caseRole(CaseRole.APPLICANTSOLICITORONE.getFormattedName()).build());

        return caseAssignedUserRoles;
    }

    public List<CaseAssignedUserRole> getCaseAssignedRespondentUserRoles() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = List.of(
            CaseAssignedUserRole.builder().caseDataId("1").userId("f5e5cc53-e065-43dd-8cec-2ad005a6b9a9")
                .caseRole(CaseRole.RESPONDENTSOLICITORONE.getFormattedName()).build(),
            CaseAssignedUserRole.builder().caseDataId("1").userId("3")
                .caseRole(CaseRole.RESPONDENTSOLICITORONE.getFormattedName()).build(),
            CaseAssignedUserRole.builder().caseDataId("1").userId("4")
                .caseRole(CaseRole.RESPONDENTSOLICITORONE.getFormattedName()).build(),
            CaseAssignedUserRole.builder().caseDataId("1").userId("5")
                .caseRole(CaseRole.APPLICANTSOLICITORONE.getFormattedName()).build());

        return caseAssignedUserRoles;
    }

    public List<CaseAssignedUserRole> getCaseAssignedRespondentUserRolesWithNoApplicantSolicitor() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = List.of(
            CaseAssignedUserRole.builder().caseDataId("1").userId("f5e5cc53-e065-43dd-8cec-2ad005a6b9a9")
                .caseRole(CaseRole.RESPONDENTSOLICITORONE.getFormattedName()).build(),
            CaseAssignedUserRole.builder().caseDataId("1").userId("3")
                .caseRole(CaseRole.RESPONDENTSOLICITORONE.getFormattedName()).build(),
            CaseAssignedUserRole.builder().caseDataId("1").userId("4")
                .caseRole(CaseRole.RESPONDENTSOLICITORONE.getFormattedName()).build(),
            CaseAssignedUserRole.builder().caseDataId("1").userId("5")
                .caseRole(CaseRole.RESPONDENTSOLICITORTWO.getFormattedName()).build());

        return caseAssignedUserRoles;
    }

    public List<CaseAssignedUserRole> getCaseAssignedApplicantUserRolesEmptyList() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = Collections.emptyList();
        return caseAssignedUserRoles;
    }

    @Test
    void shouldAssignCaseToApplicantSolicitorOne() {

        when(caseAccessDataStoreApi.getUserRoles(any(), any(), any()))
            .thenReturn(CaseAssignedUserRolesResource.builder()
                            .caseAssignedUserRoles(getCaseAssignedApplicantUserRoles()).build());
        when(organisationApi.findUserOrganisation(any(), any()))
            .thenReturn(uk.gov.hmcts.reform.prd.model.Organisation
                            .builder().organisationIdentifier("OrgId1").build());

        assignCaseToUserHandler.handle(params);

        verify(coreCaseUserService).assignCase(
            CASE_ID.toString(),
            "f5e5cc53-e065-43dd-8cec-2ad005a6b9a9",
            "OrgId1",
            CaseRole.APPLICANTSOLICITORONE
        );
    }

    @Test
    void shouldThrowExceptionForNoSolicitors() {

        when(caseAccessDataStoreApi.getUserRoles(any(), any(), any()))
            .thenReturn(CaseAssignedUserRolesResource.builder()
                            .caseAssignedUserRoles(getCaseAssignedApplicantUserRolesEmptyList()).build());
        when(organisationApi.findUserOrganisation(any(), any()))
            .thenReturn(uk.gov.hmcts.reform.prd.model.Organisation
                            .builder().organisationIdentifier("OrgId1").build());

        assertThrows(IllegalArgumentException.class, () ->
            assignCaseToUserHandler.handle(params));
    }

    @Test
    void shouldAssignCaseToRespondentSolicitorOne() {

        when(caseAccessDataStoreApi.getUserRoles(any(), any(), any()))
            .thenReturn(CaseAssignedUserRolesResource.builder()
                            .caseAssignedUserRoles(getCaseAssignedRespondentUserRoles()).build());
        when(organisationApi.findUserOrganisation(any(), any()))
            .thenReturn(uk.gov.hmcts.reform.prd.model.Organisation
                            .builder().organisationIdentifier("OrgId1").build());

        assignCaseToUserHandler.handle(params);

        verify(coreCaseUserService).assignCase(
            CASE_ID.toString(),
            "5",
            "OrgId1",
            CaseRole.RESPONDENTSOLICITORONE
        );
    }

    @Test
    void shouldAssignCaseTo_PC_Respondent_As_GA_Applicant() {
        when(caseAccessDataStoreApi.getUserRoles(any(), any(), any()))
            .thenReturn(CaseAssignedUserRolesResource.builder()
                            .caseAssignedUserRoles(getCaseAssignedRespondentUserRolesWithNoApplicantSolicitor())
                            .build());
        when(organisationApi.findUserOrganisation(any(), any()))
            .thenReturn(uk.gov.hmcts.reform.prd.model.Organisation
                            .builder().organisationIdentifier("OrgId1").build());

        assignCaseToUserHandler.handle(params);

        verify(coreCaseUserService).assignCase(
            CASE_ID.toString(),
            "f5e5cc53-e065-43dd-8cec-2ad005a6b9a9",
            "OrgId1",
            CaseRole.APPLICANTSOLICITORONE
        );
    }

    @Test
    void shouldNotInvokeAssignUserToCaseForRole() {

        when(caseAccessDataStoreApi.getUserRoles(any(), any(), any()))
            .thenReturn(CaseAssignedUserRolesResource.builder()
                            .caseAssignedUserRoles(getCaseAssignedApplicantUserRoles()).build());

        when(organisationApi.findUserOrganisation(any(), any()))
            .thenReturn(uk.gov.hmcts.reform.prd.model.Organisation
                            .builder().organisationIdentifier("OrgId1").build());

        when(coreCaseUserService
                 .userWithCaseRoleExistsOnCase(STRING_CONSTANT, STRING_CONSTANT, CaseRole.APPLICANTSOLICITORONE))
            .thenReturn(Boolean.TRUE);

        assignCaseToUserHandler.handle(params);

        verify(coreCaseUserService, never()).assignUserToCaseForRole(
            anyString(),
            anyString(),
            anyString(),
            any(CaseRole.class),
            anyString()
        );
    }
}
