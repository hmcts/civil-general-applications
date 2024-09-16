package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.MakeAppAvailableCheckGAspec;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialWrittenRepresentations;
import uk.gov.hmcts.reform.civil.model.genapplication.GAMakeApplicationAvailableCheck;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import uk.gov.hmcts.reform.dashboard.data.ScenarioRequestParams;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_JUDGE_UNCLOAK_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_REQUEST_MORE_INFO_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_WRITTEN_REPRESENTATION_REQUIRED_APPLICANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_WRITTEN_REPRESENTATION_REQUIRED_RESPONDENT;

@ExtendWith(MockitoExtension.class)
public class CreateMakeDecisionDashboardNotificationForRespondentHandlerTest extends BaseCallbackHandlerTest {

    @Mock
    private DashboardApiClient dashboardApiClient;
    @Mock
    private DashboardNotificationsParamsMapper mapper;
    @Mock
    private FeatureToggleService featureToggleService;
    @InjectMocks
    private CreateMakeDecisionDashboardNotificationForRespondentHandler handler;

    private final List<MakeAppAvailableCheckGAspec> makeAppAvailableCheck = List.of(MakeAppAvailableCheckGAspec.CONSENT_AGREEMENT_CHECKBOX);

    private final GAMakeApplicationAvailableCheck gaMakeApplicationAvailableCheck = GAMakeApplicationAvailableCheck.builder()
        .makeAppAvailableCheck(makeAppAvailableCheck).build();

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION);
    }

    @Test
    void shouldReturnCorrectCamundaActivityId_whenInvoked() {
        assertThat(handler.camundaActivityId())
            .isEqualTo("default");
    }

    @Nested
    class AboutToSubmitCallback {

        @Test
        void shouldRecordMoreInfoRequiredRespondentScenarioWhenInvoked() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .parentCaseReference(caseData.getCcdCaseReference().toString())
                .isGaApplicantLip(YesOrNo.YES)
                .parentClaimantIsApplicant(YesOrNo.YES)
                .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder().deadlineForMoreInfoSubmission(
                    LocalDateTime.now().plusDays(5)).build())
                .ccdState(CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YesOrNo.YES).build())
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION.name())
                    .build()
            ).build();

            handler.handle(params);
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPLICATION_REQUEST_MORE_INFO_RESPONDENT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

        @Test
        void shouldNotRecordMoreInfoRequiredRespondentScenarioWhenAppIsWithoutNoticeInvoked() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .parentCaseReference(caseData.getCcdCaseReference().toString())
                .isGaApplicantLip(YesOrNo.YES)
                .parentClaimantIsApplicant(YesOrNo.YES)
                .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder().requestMoreInfoOption(
                    GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION).deadlineForMoreInfoSubmission(
                    LocalDateTime.now().plusDays(5)).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YesOrNo.NO).build())
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION.name())
                    .build()
            ).build();

            handler.handle(params);
            verifyNoInteractions(dashboardApiClient);
        }

        @Test
        void shouldNotRecordMoreInfoRequiredRespondentScenarioWhenGALipsToggleIsDisableInvoked() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .parentCaseReference(caseData.getCcdCaseReference().toString())
                .isGaApplicantLip(YesOrNo.YES)
                .parentClaimantIsApplicant(YesOrNo.YES)
                .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder().requestMoreInfoOption(
                    GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION).deadlineForMoreInfoSubmission(
                    LocalDateTime.now().plusDays(5)).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YesOrNo.YES).build())
                .build();
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(false);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION.name())
                    .build()
            ).build();

            handler.handle(params);
            verifyNoInteractions(dashboardApiClient);
        }

        @Test
        void shouldRecordApplicationUncloakedRespondentScenarioWhenAppIsWithoutNoticeInvoked() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .parentCaseReference(caseData.getCcdCaseReference().toString())
                .isGaApplicantLip(YesOrNo.YES)
                .parentClaimantIsApplicant(YesOrNo.YES)
                .applicationIsUncloakedOnce(YesOrNo.YES)
                .makeAppVisibleToRespondents(gaMakeApplicationAvailableCheck)
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YesOrNo.NO).build())
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION.name())
                    .build()
            ).build();

            handler.handle(params);
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPLICATION_JUDGE_UNCLOAK_RESPONDENT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

        @Test
        void shouldNotRecordApplicationUncloakedRespondentScenarioWhenAppIsWithNoticeInvoked() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .parentCaseReference(caseData.getCcdCaseReference().toString())
                .isGaApplicantLip(YesOrNo.YES)
                .parentClaimantIsApplicant(YesOrNo.YES)
                .applicationIsUncloakedOnce(YesOrNo.NO)
                .makeAppVisibleToRespondents(gaMakeApplicationAvailableCheck)
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YesOrNo.YES).build())
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION.name())
                    .build()
            ).build();

            handler.handle(params);
            verifyNoInteractions(dashboardApiClient);
        }

        @Test
        void shouldNotRecordApplicationUncloakedRespondentScenarioWhenAppIsNotUncloakedInvoked() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .parentCaseReference(caseData.getCcdCaseReference().toString())
                .isGaApplicantLip(YesOrNo.YES)
                .parentClaimantIsApplicant(YesOrNo.YES)
                .applicationIsUncloakedOnce(YesOrNo.NO)
                .makeAppVisibleToRespondents(gaMakeApplicationAvailableCheck)
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YesOrNo.NO).build())
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION.name())
                    .build()
            ).build();

            handler.handle(params);
            verifyNoInteractions(dashboardApiClient);
        }

        @Test
        void shouldNotRecordApplicationUncloakedRespondentScenarioWhenGALipsToggleIsDisabledInvoked() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .parentCaseReference(caseData.getCcdCaseReference().toString())
                .isGaApplicantLip(YesOrNo.YES)
                .parentClaimantIsApplicant(YesOrNo.YES)
                .applicationIsUncloakedOnce(YesOrNo.NO)
                .makeAppVisibleToRespondents(gaMakeApplicationAvailableCheck)
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YesOrNo.YES).build())
                .build();
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(false);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION.name())
                    .build()
            ).build();

            handler.handle(params);
            verifyNoInteractions(dashboardApiClient);
        }

        @Test
        void shouldRecordRequestWrittenRepresentationsRespondentScenarioWhenInvoked() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .parentCaseReference(caseData.getCcdCaseReference().toString())
                .judicialDecisionMakeAnOrderForWrittenRepresentations(GAJudicialWrittenRepresentations.builder().build())
                .judicialDecision(GAJudicialDecision.builder().decision(GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YesOrNo.YES).build())
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION.name())
                    .build()
            ).build();

            handler.handle(params);
            verify(dashboardApiClient).recordScenario(
                caseData.getCcdCaseReference().toString(),
                SCENARIO_AAA6_GENERAL_APPLICATION_WRITTEN_REPRESENTATION_REQUIRED_RESPONDENT.getScenario(),
                "BEARER_TOKEN",
                ScenarioRequestParams.builder().params(scenarioParams).build()
            );
        }

        @Test
        void shouldNotRecordRequestWrittenRepresentationsRespondentScenarioWhenAppIsNotUncloakedInvoked() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeCaseData();
            caseData = caseData.toBuilder()
                .parentCaseReference(caseData.getCcdCaseReference().toString())
                .judicialDecisionMakeAnOrderForWrittenRepresentations(GAJudicialWrittenRepresentations.builder().build())
                .judicialDecision(GAJudicialDecision.builder().decision(GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS).build())
                .generalAppInformOtherParty(GAInformOtherParty.builder().isWithNotice(YesOrNo.NO).build())
                .build();

            HashMap<String, Object> scenarioParams = new HashMap<>();
            when(featureToggleService.isGaForLipsEnabled()).thenReturn(true);
            when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
            when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);

            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().eventId(CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION.name())
                    .build()
            ).build();

            handler.handle(params);
            verifyNoInteractions(dashboardApiClient);
        }
    }
}
