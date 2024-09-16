package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesDetails;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import uk.gov.hmcts.reform.dashboard.data.ScenarioRequestParams;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPLICANT_LIP_HWF_DASHBOARD_NOTIFICATION;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NO_REMISSION_HWF_GA;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_CLAIMANT_TASK_LIST_GA_COMPLETE;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPS_HWF_REJECTED_APPLICANT;

@ExtendWith(MockitoExtension.class)
public class HwFDashboardNotificationsHandlerTest extends BaseCallbackHandlerTest {

    @Mock
    private DashboardApiClient dashboardApiClient;
    @Mock
    private DashboardNotificationsParamsMapper mapper;
    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private CoreCaseDataService coreCaseDataService;
    @Mock
    private CaseDetailsConverter caseDetailsConverter;
    @InjectMocks
    private HwFDashboardNotificationsHandler handler;

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(APPLICANT_LIP_HWF_DASHBOARD_NOTIFICATION);
    }

    @Test
    void shouldReturnCorrectCamundaActivityId_whenInvoked() {
        assertThat(handler.camundaActivityId())
            .isEqualTo("default");
    }

    @Nested
    class AboutToSubmitCallback {

        @BeforeEach
        void setup() {
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
        }

        @Test
        void shouldRecordClaimantScenarioApplicationFee_whenInvoked() {
            CaseDetails caseDetails = CaseDetails.builder().build();
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .hwfFeeType(FeeType.APPLICATION)
                .gaHwfDetails(HelpWithFeesDetails.builder()
                                  .hwfCaseEvent(NO_REMISSION_HWF_GA)
                                  .build())
                .build();
            when(coreCaseDataService.getCase(any())).thenReturn(caseDetails);
            when(caseDetailsConverter.toCaseData(caseDetails)).thenReturn(caseData);

            HashMap<String, Object> scenarioParams = new HashMap<>();

            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(UPDATE_CLAIMANT_TASK_LIST_GA_COMPLETE.name())
                    .build()
            ).build();

            handler.handle(params);
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPS_HWF_REJECTED_APPLICANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

        @Test
        void shouldRecordClaimantScenarioAdditionalApplicationFee_whenInvoked() {
            CaseDetails caseDetails = CaseDetails.builder().build();
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .hwfFeeType(FeeType.ADDITIONAL)
                .additionalHwfDetails(HelpWithFeesDetails.builder()
                                  .hwfCaseEvent(NO_REMISSION_HWF_GA)
                                  .build())
                .build();
            when(coreCaseDataService.getCase(any())).thenReturn(caseDetails);
            when(caseDetailsConverter.toCaseData(caseDetails)).thenReturn(caseData);

            HashMap<String, Object> scenarioParams = new HashMap<>();

            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(UPDATE_CLAIMANT_TASK_LIST_GA_COMPLETE.name())
                    .build()
            ).build();

            handler.handle(params);
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPS_HWF_REJECTED_APPLICANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

    }
}
