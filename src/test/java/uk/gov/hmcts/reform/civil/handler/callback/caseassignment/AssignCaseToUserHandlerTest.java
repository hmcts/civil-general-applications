package uk.gov.hmcts.reform.civil.handler.callback.caseassignment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.caseassignment.AssignCaseToUserCallbackHandler;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.IdamUserDetails;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.service.CoreCaseUserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.SUMMARY_JUDGEMENT;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

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

    public static final Long CASE_ID = 1594901956117591L;

    @BeforeEach
    void setup() {

        List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

        GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
            .email("test@gmail.com").organisationIdentifier("org2").build();

        GASolicitorDetailsGAspec respondent2 = GASolicitorDetailsGAspec.builder().id("id")
            .email("test@gmail.com").organisationIdentifier("org2").build();

        respondentSols.add(element(respondent1));
        respondentSols.add(element(respondent2));

        GeneralApplication.GeneralApplicationBuilder builder = GeneralApplication.builder();
        builder.generalAppType(GAApplicationType.builder()
                                   .types(singletonList(SUMMARY_JUDGEMENT))
                                   .build())
            .claimant1PartyName("Applicant1")
            .generalAppRespondentSolicitors(respondentSols)
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec
                                         .builder()
                                         .id("id")
                                         .email("TEST@gmail.com")
                                         .organisationIdentifier("Org1").build())
            .defendant1PartyName("Respondent1")
            .claimant2PartyName("Applicant2")
            .defendant2PartyName("Respondent2")
            .generalAppParentCaseLink(GeneralAppParentCaseLink.builder().caseReference("12342341").build())
            .civilServiceUserRoles(IdamUserDetails.builder()
                                       .id("f5e5cc53-e065-43dd-8cec-2ad005a6b9a9")
                                       .email("applicant@someorg.com")
                                       .build())
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .build();

        GeneralApplication caseData = builder.build();

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        params = callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);
    }

    @Test
    void shouldCallAssignCase_3Times() {

        assignCaseToUserHandler.handle(params);
        verify(coreCaseUserService, times(3)).assignCase(
            any(),
            any(),
            any(),
            any()
        );
    }

    @Test
    void shouldThrowExceptionIfSolicitorsAreNull() {

        try {
            assignCaseToUserHandler.handle(getCaseDateWithNoSolicitor());
        } catch (Exception e) {
            assertEquals("java.lang.NullPointerException", e.toString());
        }
    }

    public CallbackParams getCaseDateWithNoSolicitor() {

        GeneralApplication.GeneralApplicationBuilder builder = GeneralApplication.builder();
        builder.generalAppType(GAApplicationType.builder()
                                   .types(singletonList(SUMMARY_JUDGEMENT))
                                   .build())
            .claimant1PartyName("Applicant1")
            .defendant1PartyName("Respondent1")
            .claimant2PartyName("Applicant2")
            .defendant2PartyName("Respondent2")
            .generalAppParentCaseLink(GeneralAppParentCaseLink.builder().caseReference("12342341").build())
            .civilServiceUserRoles(IdamUserDetails.builder()
                                       .id("f5e5cc53-e065-43dd-8cec-2ad005a6b9a9")
                                       .email("applicant@someorg.com")
                                       .build())
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .build();

        GeneralApplication caseData = builder.build();

        Map<String, Object> dataMap = objectMapper.convertValue(caseData, new TypeReference<>() {
        });
        return callbackParamsOf(dataMap, CallbackType.ABOUT_TO_SUBMIT);
    }

}
