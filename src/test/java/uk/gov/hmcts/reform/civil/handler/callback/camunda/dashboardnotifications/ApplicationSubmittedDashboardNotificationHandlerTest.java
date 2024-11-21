package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.citizenui.HelpWithFees;
import uk.gov.hmcts.reform.civil.model.genapplication.FeePaymentOutcomeDetails;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import uk.gov.hmcts.reform.dashboard.data.ScenarioRequestParams;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_GA_DASHBOARD_NOTIFICATION;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_APPLICANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPS_HWF_FEE_PAID_APPLICANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPS_HWF_FULL_REMISSION_APPLICANT;

@ExtendWith(MockitoExtension.class)
class ApplicationSubmittedDashboardNotificationHandlerTest extends BaseCallbackHandlerTest {

    @Mock
    private DashboardApiClient dashboardApiClient;
    @Mock
    private DashboardNotificationsParamsMapper mapper;
    @Mock
    private FeatureToggleService featureToggleService;
    @InjectMocks
    private ApplicationSubmittedDashboardNotificationHandler handler;

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(UPDATE_GA_DASHBOARD_NOTIFICATION);
    }

    @Nested
    class AboutToSubmitCallback {
        @BeforeEach
        void setup() {
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
        }

        @Test
        void shouldRecordApplicationSubmittedScenario_whenInvoked() {
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder().parentCaseReference(caseData.getCcdCaseReference().toString())
                .isGaApplicantLip(YesOrNo.YES)
                .parentClaimantIsApplicant(YesOrNo.YES)
                .build();
            HashMap<String, Object> scenarioParams = new HashMap<>();
            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(UPDATE_GA_DASHBOARD_NOTIFICATION.name())
                    .build()
            ).build();
            handler.handle(params);
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_APPLICANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

        @Test
        void shouldRecordWhenLipApplicationIsFeePaid() {
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            CaseData caseData = CaseData.builder()
                .ccdCaseReference(123456L)
                .feePaymentOutcomeDetails(FeePaymentOutcomeDetails
                                              .builder()
                                              .hwfFullRemissionGrantedForGa(YesOrNo.NO).build())
                .generalAppHelpWithFees(
                    HelpWithFees.builder()
                        .helpWithFeesReferenceNumber("ABC-DEF-IJK")
                        .helpWithFee(YesOrNo.YES).build()).build();

            HashMap<String, Object> scenarioParams = new HashMap<>();
            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(UPDATE_GA_DASHBOARD_NOTIFICATION.name())
                    .build()
            ).build();
            handler.handle(params);
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPS_HWF_FEE_PAID_APPLICANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_APPLICANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );

        }

        @Test
        void shouldRecordWhenLipApplicationIsFeePaidThroughCard() {
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            CaseData caseData = CaseData.builder()
                .ccdCaseReference(123456L)
                .generalAppHelpWithFees(
                    HelpWithFees.builder()
                        .helpWithFeesReferenceNumber("ABC-DEF-IJK")
                        .helpWithFee(YesOrNo.YES).build()).build();

            HashMap<String, Object> scenarioParams = new HashMap<>();
            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(UPDATE_GA_DASHBOARD_NOTIFICATION.name())
                    .build()
            ).build();
            handler.handle(params);
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_APPLICANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );

        }

        @Test
        void shouldRecordWhenLipApplicationIsFeePaidFullRemission() {
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            CaseData caseData = CaseData.builder()
                .ccdCaseReference(123456L)
                .feePaymentOutcomeDetails(FeePaymentOutcomeDetails
                                              .builder()
                                              .hwfFullRemissionGrantedForGa(YesOrNo.YES).build())
                .generalAppHelpWithFees(
                    HelpWithFees.builder()
                        .helpWithFeesReferenceNumber("ABC-DEF-IJK")
                        .helpWithFee(YesOrNo.YES).build()).build();

            HashMap<String, Object> scenarioParams = new HashMap<>();
            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(UPDATE_GA_DASHBOARD_NOTIFICATION.name())
                    .build()
            ).build();
            handler.handle(params);
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPS_HWF_FULL_REMISSION_APPLICANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_APPLICANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }
    }
}
