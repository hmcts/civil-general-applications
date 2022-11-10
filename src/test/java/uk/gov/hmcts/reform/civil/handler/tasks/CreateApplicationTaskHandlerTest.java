package uk.gov.hmcts.reform.civil.handler.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Lists;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.ccd.model.Organisation;
import uk.gov.hmcts.reform.ccd.model.OrganisationPolicy;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GADetailsRespondentSol;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.time.LocalDate.EPOCH;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CREATE_GENERAL_APPLICATION_CASE;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.SUMMARY_JUDGEMENT;
import static uk.gov.hmcts.reform.civil.handler.tasks.BaseExternalTaskHandler.FLOW_FLAGS;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@SpringBootTest(classes = {
    CreateApplicationTaskHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    StateFlowEngine.class
})
@ExtendWith(SpringExtension.class)
public class CreateApplicationTaskHandlerTest {

    private static final String STRING_CONSTANT = "this is a string";
    private static final LocalDate APP_DATE_EPOCH = EPOCH;
    private static final String PROCESS_INSTANCE_ID = "1";
    private static final String CASE_ID = "1";
    private static final String GA_ID = "2";
    private static final String GENERAL_APPLICATIONS = "generalApplications";
    private static final String GENERAL_APPLICATIONS_DETAILS = "generalApplicationsDetails";
    private static final String GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL = "gaDetailsRespondentSol";
    private static final LocalDateTime DUMMY_DATE = LocalDateTime.parse("2022-02-22T15:59:59");

    List<Element<GeneralApplicationsDetails>> generalApplicationsDetailsList = Lists.newArrayList();
    List<Element<GeneralApplicationsDetails>>  gaDetailsMasterCollection = Lists.newArrayList();
    List<Element<GADetailsRespondentSol>> gaDetailsRespondentSolList = Lists.newArrayList();
    List<Element<GADetailsRespondentSol>> gaDetailsRespondentSolTwoList = Lists.newArrayList();
    List<Element<GeneralApplication>> generalApplications = Lists.newArrayList();
    CaseDataContent caseDataContent = CaseDataContent.builder().build();

    @Mock
    private ExternalTask mockTask;

    @Mock
    private ExternalTaskService externalTaskService;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private CreateApplicationTaskHandler createApplicationTaskHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void init() {
        when(mockTask.getTopicName()).thenReturn("test");
        when(mockTask.getWorkerId()).thenReturn("worker");
        when(mockTask.getActivityId()).thenReturn("activityId");

        Map<String, Object> variables = Map.of(
            "caseId", CASE_ID,
            "caseEvent", CREATE_GENERAL_APPLICATION_CASE.name()
        );

        when(mockTask.getAllVariables()).thenReturn(variables);
    }

    @Nested
    class CreateGeneralApplication {
        /*
         * GA without notice application
         * */

        @Test
        void shouldAddApplicantSolListForWithoutNoticeAppln() {
            GeneralApplication generalApplication =
                getGeneralApplication("applicant", YES, NO);
            CaseData data = buildData(generalApplication);

            assertThat(data.getGaDetailsRespondentSol().size()).isEqualTo(1);
            assertThat(data.getGeneralApplicationsDetails().size()).isEqualTo(2);
            assertThat(data.getGaDetailsRespondentSolTwo().size()).isEqualTo(0);
            assertThat(data.getGaDetailsMasterCollection().size()).isEqualTo(1);
        }

        @Test
        void shouldAddRespondentOneSolListForWithoutNoticeAppln() {
            GeneralApplication generalApplication =
                getGeneralApplication("respondent1", NO, NO);
            CaseData data = buildData(generalApplication);

            assertThat(data.getGaDetailsRespondentSol().size()).isEqualTo(2);
            assertThat(data.getGeneralApplicationsDetails().size()).isEqualTo(1);
            assertThat(data.getGaDetailsRespondentSolTwo().size()).isEqualTo(0);
            assertThat(data.getGaDetailsMasterCollection().size()).isEqualTo(1);

        }

        @Test
        void shouldAddRespondentTwoSolListForWithoutNoticeAppln() {
            GeneralApplication generalApplication =
                getGeneralApplication("respondent2", NO, NO);
            CaseData data = buildData(generalApplication);

            assertThat(data.getGaDetailsRespondentSol().size()).isEqualTo(1);
            assertThat(data.getGaDetailsRespondentSolTwo().size()).isEqualTo(1);
            assertThat(data.getGeneralApplicationsDetails().size()).isEqualTo(1);
            assertThat(data.getGaDetailsMasterCollection().size()).isEqualTo(1);

        }

        /*
         * GA with notice application
         * */

        @Test
        void shouldAddApplicantSolListForWithNoticeAppln() {
            GeneralApplication generalApplication =
                getGeneralApplication("applicant", YES, YES);
            CaseData data = buildData(generalApplication);

            assertThat(data.getGaDetailsRespondentSol().size()).isEqualTo(2);
            assertThat(data.getGeneralApplicationsDetails().size()).isEqualTo(2);
            assertThat(data.getGaDetailsRespondentSolTwo().size()).isEqualTo(1);
            assertThat(data.getGaDetailsMasterCollection().size()).isEqualTo(1);

        }

        @Test
        void shouldAddRespondentOneSolListForWithNoticeAppln() {
            GeneralApplication generalApplication =
                getGeneralApplication("respondent1", NO, YES);
            CaseData data = buildData(generalApplication);

            assertThat(data.getGaDetailsRespondentSol().size()).isEqualTo(2);
            assertThat(data.getGeneralApplicationsDetails().size()).isEqualTo(2);
            assertThat(data.getGaDetailsRespondentSolTwo().size()).isEqualTo(1);
            assertThat(data.getGaDetailsMasterCollection().size()).isEqualTo(1);

        }

        @Test
        void shouldAddRespondentTwoSolListForWithNoticeAppln() {
            GeneralApplication generalApplication =
                getGeneralApplication("respondent2", NO, YES);
            CaseData data = buildData(generalApplication);

            assertThat(data.getGaDetailsRespondentSol().size()).isEqualTo(2);
            assertThat(data.getGaDetailsRespondentSolTwo().size()).isEqualTo(1);
            assertThat(data.getGeneralApplicationsDetails().size()).isEqualTo(2);
            assertThat(data.getGaDetailsMasterCollection().size()).isEqualTo(1);

        }

        private GeneralApplication getGeneralApplication(String organisationIdentifier,
                                                         YesOrNo parentClaimantIsApplicant,
                                                         YesOrNo isWithoutNotice) {
            GeneralApplication.GeneralApplicationBuilder builder = GeneralApplication.builder();

            builder.generalAppType(GAApplicationType.builder()
                                       .types(singletonList(SUMMARY_JUDGEMENT))
                                       .build());

            return builder
                .parentClaimantIsApplicant(parentClaimantIsApplicant)
                .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder()
                                              .organisationIdentifier(organisationIdentifier).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder()
                                                .isWithNotice(isWithoutNotice)
                                                .reasonsForWithoutNotice(STRING_CONSTANT)
                                                .build())
                .generalAppDateDeadline(DUMMY_DATE)
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder()
                                                  .generalAppUrgency(YES)
                                                  .reasonsForUrgency(STRING_CONSTANT)
                                                  .urgentAppConsiderationDate(APP_DATE_EPOCH)
                                                  .build())
                .isMultiParty(NO)
                .businessProcess(BusinessProcess.builder()
                                     .status(STARTED)
                                     .processInstanceId(PROCESS_INSTANCE_ID)
                                     .camundaEvent(CREATE_GENERAL_APPLICATION_CASE.name())
                                     .build())
                .build();
        }
    }

    @Nested
    class CreateGeneralApplicationCCDEvent {

        @Test
        void shouldTriggerCCDEvent() {
            GeneralApplication generalApplication = getGeneralApplication();
            CaseData data = buildData(generalApplication);

            assertThat(data.getGaDetailsRespondentSol().size()).isEqualTo(2);
            assertThat(data.getGeneralApplicationsDetails().size()).isEqualTo(2);
        }

        @Test
        void shouldNotTriggerCCDEvent() {

            CaseData caseData = new CaseDataBuilder().atStateClaimDraft()
                .businessProcess(BusinessProcess.builder().status(STARTED)
                                     .processInstanceId(PROCESS_INSTANCE_ID).build()).build();

            VariableMap variables = Variables.createVariables();
            variables.putValue(BaseExternalTaskHandler.FLOW_STATE, "MAIN.DRAFT");
            variables.putValue(FLOW_FLAGS, Map.of());
            variables.putValue("generalApplicationCaseId", GA_ID);

            CaseDetails caseDetails = CaseDetailsBuilder.builder().data(caseData).build();
            StartEventResponse startEventResponse = StartEventResponse.builder().caseDetails(caseDetails).build();
            CaseDataContent caseDataContent = CaseDataContent.builder().build();

            when(caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails()))
                .thenReturn(caseData);

            when(coreCaseDataService.startUpdate(anyString(), any(CaseEvent.class)))
                .thenReturn(startEventResponse);

            when(coreCaseDataService.caseDataContentFromStartEventResponse(
                any(StartEventResponse.class),
                anyMap()
            )).thenReturn(caseDataContent);

            createApplicationTaskHandler.execute(mockTask, externalTaskService);

            verify(coreCaseDataService, times(1)).startUpdate(CASE_ID, CREATE_GENERAL_APPLICATION_CASE);
            verify(coreCaseDataService, never()).createGeneralAppCase(anyMap());
            verify(coreCaseDataService, times(1)).submitUpdate(CASE_ID, caseDataContent);
        }

        @Test
        void shouldTriggerCCDEventWhenAdditionalFieldAdded() {
            GeneralApplication generalApplication = getGeneralApplication();

            CaseData caseData = new CaseDataBuilder().atStateClaimDraft()
                .generalApplications(getGeneralApplications(generalApplication))
                .build();

            VariableMap variables = Variables.createVariables();
            variables.putValue(BaseExternalTaskHandler.FLOW_STATE, "MAIN.DRAFT");
            variables.putValue(FLOW_FLAGS, Map.of());
            variables.putValue("generalApplicationCaseId", GA_ID);

            CaseDetails caseDetails = CaseDetailsBuilder.builder().data(caseData).build();
            StartEventResponse startEventResponse = StartEventResponse.builder().caseDetails(caseDetails).build();
            CaseDataContent caseDataContent = CaseDataContent.builder().build();
            when(coreCaseDataService.startUpdate(CASE_ID, CREATE_GENERAL_APPLICATION_CASE))
                .thenReturn(startEventResponse);

            when(caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails()))
                .thenReturn(caseData);

            when(coreCaseDataService.caseDataContentFromStartEventResponse(
                any(StartEventResponse.class),
                anyMap()
            )).thenReturn(caseDataContent);

            when(coreCaseDataService.submitUpdate(CASE_ID, caseDataContent)).thenReturn(caseData);
            Map<String, Object> map = generalApplication.toMap(objectMapper);
            map.put(
                "generalAppNotificationDeadlineDate",
                generalApplication
                    .getGeneralAppDateDeadline()
            );

            when(coreCaseDataService.createGeneralAppCase(map)).thenReturn(caseData);

            when(coreCaseDataService.submitUpdate(CASE_ID, caseDataContent)).thenReturn(caseData);

            createApplicationTaskHandler.execute(mockTask, externalTaskService);

            verify(coreCaseDataService).createGeneralAppCase(map);

        }

        private GeneralApplication getGeneralApplication() {
            GeneralApplication.GeneralApplicationBuilder builder = GeneralApplication.builder();

            builder.generalAppType(GAApplicationType.builder()
                                       .types(singletonList(SUMMARY_JUDGEMENT))
                                       .build());

            return builder
                .generalAppInformOtherParty(GAInformOtherParty.builder()
                                                .isWithNotice(YES)
                                                .reasonsForWithoutNotice(STRING_CONSTANT)
                                                .build())
                .generalAppDateDeadline(DUMMY_DATE)
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder()
                                                  .generalAppUrgency(YES)
                                                  .reasonsForUrgency(STRING_CONSTANT)
                                                  .urgentAppConsiderationDate(APP_DATE_EPOCH)
                                                  .build())
                .isMultiParty(NO)
                .businessProcess(BusinessProcess.builder()
                                     .status(STARTED)
                                     .processInstanceId(PROCESS_INSTANCE_ID)
                                     .camundaEvent(CREATE_GENERAL_APPLICATION_CASE.name())
                                     .build())
                .build();
        }

    }

    public List<Element<GeneralApplication>> getGeneralApplications(GeneralApplication generalApplication) {
        return wrapElements(generalApplication);
    }

    public CaseData buildData(GeneralApplication generalApplication) {
        generalApplications = getGeneralApplications(generalApplication);
        generalApplicationsDetailsList = Lists.newArrayList();
        gaDetailsMasterCollection = Lists.newArrayList();
        gaDetailsRespondentSolList = Lists.newArrayList();
        gaDetailsRespondentSolTwoList = Lists.newArrayList();

        GeneralApplicationsDetails generalApplicationsDetails = GeneralApplicationsDetails.builder()
            .generalApplicationType("Summary judgment")
            .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
            .caseLink(generalApplication.getCaseLink())
            .caseState("pending").build();
        generalApplicationsDetailsList.add(element(generalApplicationsDetails));

        GADetailsRespondentSol gaDetailsRespondentSol = GADetailsRespondentSol.builder()
            .generalApplicationType("Summary judgment")
            .generalAppSubmittedDateGAspec(generalApplication.getGeneralAppSubmittedDateGAspec())
            .caseLink(generalApplication.getCaseLink())
            .caseState("pending").build();
        gaDetailsRespondentSolList.add(element(gaDetailsRespondentSol));

        CaseData caseData = new CaseDataBuilder().atStateClaimDraft()
            .respondent1OrganisationPolicy(OrganisationPolicy
                                               .builder().organisation(Organisation
                                                                           .builder()
                                                                           .organisationID("respondent1").build())
                                               .build())
            .respondent2OrganisationPolicy(OrganisationPolicy
                                               .builder().organisation(Organisation
                                                                           .builder()
                                                                           .organisationID("respondent2").build())
                                               .build())
            .ccdState(CaseState.PENDING_APPLICATION_ISSUED)
            .generalApplications(generalApplications)
            .gaDetailsMasterCollection(gaDetailsMasterCollection)
            .generalApplicationsDetails(generalApplicationsDetailsList)
            .gaDetailsRespondentSol(gaDetailsRespondentSolList)
            .gaDetailsRespondentSolTwo(gaDetailsRespondentSolTwoList)
            .businessProcess(BusinessProcess.builder().status(STARTED)
                                 .processInstanceId(PROCESS_INSTANCE_ID).build()).build();

        VariableMap variables = Variables.createVariables();
        variables.putValue(BaseExternalTaskHandler.FLOW_STATE, "MAIN.DRAFT");
        variables.putValue(FLOW_FLAGS, Map.of());
        variables.putValue("generalApplicationCaseId", GA_ID);

        CaseDetails caseDetails = CaseDetailsBuilder.builder().data(caseData).build();
        StartEventResponse startEventResponse = StartEventResponse.builder().caseDetails(caseDetails).build();
        caseDataContent = CaseDataContent.builder().build();

        when(coreCaseDataService.startUpdate(CASE_ID, CREATE_GENERAL_APPLICATION_CASE))
            .thenReturn(startEventResponse);

        when(caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails()))
            .thenReturn(caseData);

        when(coreCaseDataService.caseDataContentFromStartEventResponse(
            any(StartEventResponse.class),
            anyMap()
        )).thenReturn(caseDataContent);

        when(coreCaseDataService.submitUpdate(CASE_ID, caseDataContent)).thenReturn(caseData);

        Map<String, Object> map = generalApplication.toMap(objectMapper);
        map.put(
            "generalAppNotificationDeadlineDate",
            generalApplication
                .getGeneralAppDateDeadline()
        );

        when(coreCaseDataService.createGeneralAppCase(map)).thenReturn(caseData);

        createApplicationTaskHandler.execute(mockTask, externalTaskService);

        verify(coreCaseDataService).startUpdate(CASE_ID, CREATE_GENERAL_APPLICATION_CASE);

        verify(coreCaseDataService).createGeneralAppCase(map);

        verify(coreCaseDataService).submitUpdate(CASE_ID, caseDataContent);

        CaseData data = coreCaseDataService.submitUpdate(CASE_ID, caseDataContent);

        return data;
    }

    public Map<String, Object> getUpdatedCaseData(CaseData caseData,
                                                  List<Element<GeneralApplication>> generalApplications,
                                                  List<Element<GeneralApplicationsDetails>>
                                                      generalApplicationsDetails,
                                                  List<Element<GADetailsRespondentSol>>
                                                      gaDetailsRespondentSol) {
        Map<String, Object> output = caseData.toMap(objectMapper);
        output.put(GENERAL_APPLICATIONS, generalApplications);
        output.put(GENERAL_APPLICATIONS_DETAILS, generalApplicationsDetails);
        output.put(GENERAL_APPLICATIONS_DETAILS_FOR_RESP_SOL, gaDetailsRespondentSol);
        return output;
    }
}
