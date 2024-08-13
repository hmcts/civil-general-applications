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
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import uk.gov.hmcts.reform.dashboard.data.ScenarioRequestParams;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_RESPONDENT_TASK_LIST_GA_CREATED;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_DEFENDANT;

@ExtendWith(MockitoExtension.class)
public class ApplicationCreatedTaskListForRespondentUpdateHandlerTest extends BaseCallbackHandlerTest {

    @Mock
    private DashboardApiClient dashboardApiClient;
    @Mock
    private DashboardNotificationsParamsMapper mapper;
    @Mock
    private FeatureToggleService featureToggleService;
    @InjectMocks
    private ApplicationCreatedTaskListForRespondentUpdateHandler handler;

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(UPDATE_RESPONDENT_TASK_LIST_GA_CREATED);
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
        void shouldRecordDefendantScenarioWhenGaApplicantIsLipAndWithNotice() {
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .parentCaseReference(caseData.getCcdCaseReference().toString())
                .isGaApplicantLip(YesOrNo.YES)
                .parentClaimantIsApplicant(YesOrNo.NO)
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YesOrNo.YES).build())
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();

            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(UPDATE_RESPONDENT_TASK_LIST_GA_CREATED.name())
                    .build()
            ).build();

            handler.handle(params);
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_DEFENDANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

        @Test
        void shouldRecordDefendantScenarioWhenGaRespondentIsLipAndWithConsent() {
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .parentCaseReference(caseData.getCcdCaseReference().toString())
                .isGaRespondentOneLip(YesOrNo.YES)
                .parentClaimantIsApplicant(YesOrNo.NO)
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YesOrNo.NO).build())
                .generalAppConsentOrder(YesOrNo.YES)
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();

            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(UPDATE_RESPONDENT_TASK_LIST_GA_CREATED.name())
                    .build()
            ).build();

            handler.handle(params);
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_DEFENDANT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

        @Test
        void shouldNotRecordScenario_whenWithoutNoticeAndWithoutConsent() {
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .parentCaseReference(caseData.getCcdCaseReference().toString())
                .isGaRespondentOneLip(YesOrNo.YES)
                .parentClaimantIsApplicant(YesOrNo.NO)
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YesOrNo.NO).build())
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();

            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(UPDATE_RESPONDENT_TASK_LIST_GA_CREATED.name())
                    .build()
            ).build();

            handler.handle(params);
            verifyNoInteractions(dashboardApiClient);
        }

        @Test
        void shouldNotRecordScenario_whenLRVLR() {
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .parentCaseReference(caseData.getCcdCaseReference().toString())
                .parentClaimantIsApplicant(YesOrNo.NO)
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();

            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(UPDATE_RESPONDENT_TASK_LIST_GA_CREATED.name())
                    .build()
            ).build();

            handler.handle(params);
            verifyNoInteractions(dashboardApiClient);
        }

        @Test
        void shouldNotRecordScenario_whenGaForLipsDisabled() {
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(false);
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(UPDATE_RESPONDENT_TASK_LIST_GA_CREATED.name())
                    .build()
            ).build();

            handler.handle(params);
            verifyNoInteractions(dashboardApiClient);
        }
    }
}
