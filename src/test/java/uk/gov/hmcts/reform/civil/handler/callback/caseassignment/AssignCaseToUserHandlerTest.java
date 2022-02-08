package uk.gov.hmcts.reform.civil.handler.callback.caseassignment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.CaseRole;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.caseassignment.AssignCaseToUserCallbackHandler;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.IdamUserDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.service.CoreCaseUserService;

import java.util.Map;

import static java.util.Collections.singletonList;
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

    @Test
    void shouldAssignCaseToApplicantSolicitorOne() {

        assignCaseToUserHandler.handle(params);

        verify(coreCaseUserService).assignCase(
            CASE_ID.toString(),
            caseData.getApplicantSolicitor1UserDetails().getId(),
            "OrgId1",
            CaseRole.APPLICANTSOLICITORONE
        );
    }

    @Test
    void shouldNotInvokeAssignUserToCaseForRole() {

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
