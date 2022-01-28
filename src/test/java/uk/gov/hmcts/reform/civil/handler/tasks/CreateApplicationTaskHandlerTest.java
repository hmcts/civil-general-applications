package uk.gov.hmcts.reform.civil.handler.tasks;

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
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.LocalDate.EPOCH;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CREATE_GENERAL_APPLICATION_CASE;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.STARTED;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.SUMMARY_JUDGEMENT;
import static uk.gov.hmcts.reform.civil.handler.tasks.BaseExternalTaskHandler.FLOW_FLAGS;
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

    @Mock
    private ExternalTask mockTask;

    @Mock
    private ExternalTaskService externalTaskService;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private CreateApplicationTaskHandler createApplicationTaskHandler;

    @BeforeEach
    void init() {
        when(mockTask.getTopicName()).thenReturn("test");
        when(mockTask.getWorkerId()).thenReturn("worker");
        when(mockTask.getActivityId()).thenReturn("activityId");
    }

    @Nested
    class CreateApplication {

        @BeforeEach
        void init() {
            Map<String, Object> variables = Map.of(
                "caseId", CASE_ID,
                "caseEvent", CREATE_GENERAL_APPLICATION_CASE.name()
            );

            when(mockTask.getAllVariables()).thenReturn(variables);
        }

        @Test
        void shouldTriggerCCDEvent_whenHandlerIsExecuted() {

            GeneralApplication generalApplication = getGeneralApplication();

            CaseData caseData = new CaseDataBuilder().atStateClaimDraft()
                .generalApplications(getGeneralApplications(generalApplication))
                .businessProcess(BusinessProcess.builder().status(STARTED)
                                     .processInstanceId(PROCESS_INSTANCE_ID).build()).build();

            VariableMap variables = Variables.createVariables();
            variables.putValue(BaseExternalTaskHandler.FLOW_STATE, "MAIN.DRAFT");
            variables.putValue(FLOW_FLAGS, Map.of());
            variables.putValue("generalApplicationCaseId", GA_ID);

            CaseDetails caseDetails = CaseDetailsBuilder.builder().data(caseData).build();

            when(coreCaseDataService.startUpdate(CASE_ID, CREATE_GENERAL_APPLICATION_CASE))
                .thenReturn(StartEventResponse.builder().caseDetails(caseDetails).build());

            when(coreCaseDataService.caseDataContentFromStartEventResponse(any(StartEventResponse.class), any(Map.class)));

            createApplicationTaskHandler.execute(mockTask, externalTaskService);

            verify(coreCaseDataService).startUpdate(CASE_ID, CREATE_GENERAL_APPLICATION_CASE);
            //verify(externalTaskService).complete(mockTask, variables);
        }

        public CaseDataContent caseDataContentFromStartEventResponse(
            StartEventResponse startEventResponse, Map<String, Object> contentModified) {
            var payload = new HashMap<>(startEventResponse.getCaseDetails().getData());
            payload.putAll(contentModified);

            return CaseDataContent.builder()
                .eventToken(startEventResponse.getToken())
                .event(Event.builder()
                           .id(startEventResponse.getEventId())
                           .build())
                .data(payload)
                .build();
        }

        private List<Element<GeneralApplication>> getGeneralApplications(GeneralApplication generalApplication) {
            return wrapElements(generalApplication);
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
                .generalAppUrgencyRequirement(GAUrgencyRequirement.builder()
                                                  .generalAppUrgency(YES)
                                                  .reasonsForUrgency(STRING_CONSTANT)
                                                  .urgentAppConsiderationDate(APP_DATE_EPOCH)
                                                  .build())
                .isMultiParty(YesOrNo.NO)
                .businessProcess(BusinessProcess.builder()
                                     .status(STARTED)
                                     .processInstanceId(PROCESS_INSTANCE_ID)
                                     .build())
                .build();
        }
    }
}
