package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.PaymentStatus;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.citizenui.HelpWithFees;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import uk.gov.hmcts.reform.civil.service.GaForLipService;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;
import uk.gov.hmcts.reform.dashboard.data.ScenarioRequestParams;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NOTIFY_HELP_WITH_FEE;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPS_HWF_REQUESTED_APPLICANT;

@ExtendWith(MockitoExtension.class)
class ApplyForHwFDashboardNotificationHandlerTest extends BaseCallbackHandlerTest {

    @Mock
    private DashboardApiClient dashboardApiClient;
    @Mock
    private DashboardNotificationsParamsMapper mapper;
    @Mock
    private GaForLipService gaForLipService;
    @Mock
    private ParentCaseUpdateHelper parentCaseUpdateHelper;

    @InjectMocks
    private ApplyForHwFDashboardNotificationHandler handler;
    @MockBean
    private ObjectMapper objectMapper;

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(NOTIFY_HELP_WITH_FEE);
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
            objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules();
            handler = new ApplyForHwFDashboardNotificationHandler(objectMapper, dashboardApiClient, mapper, gaForLipService, parentCaseUpdateHelper);
        }

        @Test
        void shouldRecordApplicantScenario_ApplyForHwF_whenInvoked() {

            when(gaForLipService.isGaForLip(any())).thenReturn(true);

            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .generalAppType(GAApplicationType.builder().types(List.of(GeneralApplicationTypes.VARY_ORDER))
                                    .build())
                .generalAppPBADetails(GAPbaDetails.builder().build())
                .generalAppHelpWithFees(HelpWithFees.builder().helpWithFee(YesOrNo.YES)
                                            .helpWithFeesReferenceNumber("HWF-234-456").build())
                .ccdState(CaseState.AWAITING_APPLICATION_PAYMENT)
                .isGaApplicantLip(YesOrNo.YES)
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();

            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(NOTIFY_HELP_WITH_FEE.name())
                    .build()
            ).build();

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData updatedData = objectMapper.convertValue(response.getData(), CaseData.class);
            assertThat(updatedData.getGaHwfDetails().getHwfFeeType().getLabel().equals("application"));
            assertThat(updatedData.getGaHwfDetails().getHwfReferenceNumber().equals("HWF-234-456"));

            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPS_HWF_REQUESTED_APPLICANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

        @Test
        void shouldRecordApplicantScenario_ApplyForHwF_whenInvoked_gaLipIsFalse() {

            when(gaForLipService.isGaForLip(any())).thenReturn(false);

            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .generalAppType(GAApplicationType.builder().types(List.of(GeneralApplicationTypes.VARY_ORDER))
                                    .build())
                .generalAppPBADetails(GAPbaDetails.builder().build())
                .generalAppHelpWithFees(HelpWithFees.builder().helpWithFee(YesOrNo.YES).build())
                .ccdState(CaseState.AWAITING_APPLICATION_PAYMENT)
                .isGaApplicantLip(YesOrNo.YES)
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();

            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(NOTIFY_HELP_WITH_FEE.name())
                    .build()
            ).build();

            handler.handle(params);
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPS_HWF_REQUESTED_APPLICANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

        @Test
        void shouldRecordApplicantScenario_ApplyForHwF_whenInvoked_for_additional() {

            when(gaForLipService.isGaForLip(any())).thenReturn(true);

            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .generalAppType(GAApplicationType.builder().types(List.of(GeneralApplicationTypes.VARY_ORDER))
                                    .build())
                .generalAppPBADetails(GAPbaDetails.builder().paymentDetails(PaymentDetails.builder().status(
                    PaymentStatus.SUCCESS).build()).build())
                .generalAppHelpWithFees(HelpWithFees.builder().helpWithFee(YesOrNo.YES).build())
                .ccdState(CaseState.APPLICATION_ADD_PAYMENT)
                .isGaApplicantLip(YesOrNo.YES)
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();

            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(NOTIFY_HELP_WITH_FEE.name())
                    .build()
            ).build();

            handler.handle(params);
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPS_HWF_REQUESTED_APPLICANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

        @Test
        void shouldRecordApplicantScenario_ApplyForHwFAdditionalApplicationFee_whenInvoked() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .generalAppType(GAApplicationType.builder().types(List.of(GeneralApplicationTypes.VARY_ORDER))
                                    .build())
                .ccdState(CaseState.APPLICATION_ADD_PAYMENT)
                .generalAppHelpWithFees(HelpWithFees.builder().helpWithFeesReferenceNumber("HWF-111-222").build())
                .isGaApplicantLip(YesOrNo.YES)
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();

            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(NOTIFY_HELP_WITH_FEE.name())
                    .build()
            ).build();

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData updatedData = objectMapper.convertValue(response.getData(), CaseData.class);
            assertThat(updatedData.getAdditionalHwfDetails().getHwfFeeType().getLabel().equals("additional"));
            assertThat(updatedData.getAdditionalHwfDetails().getHwfReferenceNumber().equals("HWF-111-222"));

            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPS_HWF_REQUESTED_APPLICANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

    }
}
