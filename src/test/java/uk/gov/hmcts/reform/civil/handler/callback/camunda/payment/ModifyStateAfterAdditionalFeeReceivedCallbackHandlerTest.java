package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.MakeAppAvailableCheckGAspec;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.citizenui.HelpWithFees;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.FeePaymentOutcomeDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAMakeApplicationAvailableCheck;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUrgencyRequirement;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.AssignCaseToResopondentSolHelper;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import uk.gov.hmcts.reform.civil.service.GaForLipService;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;
import uk.gov.hmcts.reform.civil.service.StateGeneratorService;
import uk.gov.hmcts.reform.dashboard.data.ScenarioRequestParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_CLAIMANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_DEFENDANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_NONURGENT_UNCLOAKED_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_URGENT_UNCLOAKED_RESPONDENT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_APPLICANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPS_HWF_FEE_PAID_APPLICANT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPS_HWF_FULL_REMISSION_APPLICANT;

import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@ExtendWith(MockitoExtension.class)
class ModifyStateAfterAdditionalFeeReceivedCallbackHandlerTest extends BaseCallbackHandlerTest {

    public static final long CCD_CASE_REFERENCE = 1234L;
    public static final String PARENT_CASE_REFERENCE = "123498";

    @Mock
    private ParentCaseUpdateHelper parentCaseUpdateHelper;

    @Mock
    private StateGeneratorService stateGeneratorService;

    @Mock
    private DashboardApiClient dashboardApiClient;

    @Mock
    private DashboardNotificationsParamsMapper mapper;

    @Mock
    private FeatureToggleService featureToggleService;

    @Mock
    private AssignCaseToResopondentSolHelper assignCaseToResopondentSolHelper;
    @Mock
    private GaForLipService gaForLipService;

    @InjectMocks
    private ModifyStateAfterAdditionalFeeReceivedCallbackHandler handler;

    private final List<MakeAppAvailableCheckGAspec> makeAppAvailableCheck = List.of(MakeAppAvailableCheckGAspec.CONSENT_AGREEMENT_CHECKBOX);

    private final GAMakeApplicationAvailableCheck gaMakeApplicationAvailableCheck = GAMakeApplicationAvailableCheck.builder()
        .makeAppAvailableCheck(makeAppAvailableCheck).build();

    @Test
    void shouldRespondWithStateChanged() {

        CaseData caseData = CaseDataBuilder.builder()
            .isMultiParty(YesOrNo.NO)
            .generalAppRespondentSolicitors(getRespondentSolicitors())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id")
                                          .email("test@gmail.com").organisationIdentifier("org1").build())
            .makeAppVisibleToRespondents(gaMakeApplicationAvailableCheck)
            .isGaRespondentOneLip(NO)
            .isGaApplicantLip(NO)
            .isGaRespondentTwoLip(NO)
            .ccdCaseReference(CCD_CASE_REFERENCE).build();

        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response.getErrors()).isNull();
        assertThat(response.getState()).isEqualTo(AWAITING_RESPONDENT_RESPONSE.toString());

        verify(assignCaseToResopondentSolHelper, times(1)).assignCaseToRespondentSolicitor(
            any(),
            any()
        );
    }

    @Test
    void shouldRespondWithStateChangedWhenApplicationUncloaked() {

        CaseData caseData = CaseDataBuilder.builder()
            .isMultiParty(YesOrNo.NO)
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id")
                                          .email("test@gmail.com").organisationIdentifier("org1").build())
            .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder().requestMoreInfoOption(
                GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY).build())
            .isGaRespondentOneLip(NO)
            .isGaApplicantLip(NO)
            .isGaRespondentTwoLip(NO)
            .generalAppRespondentSolicitors(getRespondentSolicitors())
            .ccdCaseReference(CCD_CASE_REFERENCE).build();

        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response.getErrors()).isNull();
        assertThat(response.getState()).isEqualTo(AWAITING_RESPONDENT_RESPONSE.toString());

        verify(assignCaseToResopondentSolHelper, times(1)).assignCaseToRespondentSolicitor(
            any(),
            any()
        );
    }

    @Test
    void shouldNotRespondWithStateChangedWhenApplicationUncloaked() {

        CaseData caseData = CaseDataBuilder.builder()
            .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder().requestMoreInfoOption(
                GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION).build())
            .generalAppRespondentSolicitors(getRespondentSolicitors())
            .ccdCaseReference(CCD_CASE_REFERENCE).build();

        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response.getErrors()).isNull();
        assertThat(response.getState()).isEqualTo(AWAITING_RESPONDENT_RESPONSE.toString());

        verify(assignCaseToResopondentSolHelper, times(0)).assignCaseToRespondentSolicitor(
            any(),
            any()
        );
    }

    @Test
    void shouldDispatchBusinessProcess_whenStatusIsReady() {
        CaseData caseData = CaseDataBuilder.builder().ccdCaseReference(CCD_CASE_REFERENCE).build();
        CallbackParams params = callbackParamsOf(caseData, SUBMITTED);
        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);

        handler.handle(params);

        verify(parentCaseUpdateHelper, times(1)).updateParentApplicationVisibilityWithNewState(
            caseData,
            AWAITING_RESPONDENT_RESPONSE.getDisplayedValue()
        );
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID);
    }

    @Test
    void shouldUpdateDefendantTaskListIfGaRespondentLip() {

        CaseData caseData = CaseDataBuilder.builder()
            .isMultiParty(YesOrNo.NO)
            .generalAppRespondentSolicitors(getRespondentSolicitors())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id")
                                          .email("test@gmail.com").organisationIdentifier("org1").build())
            .makeAppVisibleToRespondents(gaMakeApplicationAvailableCheck)
            .isGaRespondentOneLip(YES)
            .parentClaimantIsApplicant(YES)
            .isGaApplicantLip(NO)
            .ccdCaseReference(CCD_CASE_REFERENCE).build();

        HashMap<String, Object> scenarioParams = new HashMap<>();

        when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
        when(gaForLipService.isGaForLip(caseData)).thenReturn(true);
        when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);
        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);

        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        handler.handle(params);

        verify(dashboardApiClient).recordScenario(
            caseData.getParentCaseReference(),
            SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_DEFENDANT.getScenario(),
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
    void shouldUpdateClaimantTaskListIfGaApplicantLip() {

        CaseData caseData = CaseDataBuilder.builder()
            .isMultiParty(YesOrNo.NO)
            .generalAppRespondentSolicitors(getRespondentSolicitors())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id")
                                          .email("test@gmail.com").organisationIdentifier("org1").build())
            .makeAppVisibleToRespondents(gaMakeApplicationAvailableCheck)
            .isGaRespondentOneLip(NO)
            .isGaApplicantLip(YES)
            .ccdCaseReference(CCD_CASE_REFERENCE).build();

        HashMap<String, Object> scenarioParams = new HashMap<>();

        when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
        when(gaForLipService.isGaForLip(caseData)).thenReturn(true);
        when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);
        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);

        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        handler.handle(params);

        verify(dashboardApiClient).recordScenario(
            caseData.getParentCaseReference(),
            SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_CLAIMANT.getScenario(),
            "BEARER_TOKEN",
            ScenarioRequestParams.builder().params(scenarioParams).build()
        );
    }

    @Test
    void shouldUpdateClaimantTaskListIfGaApplicantLipAndFeeIsPaid() {

        CaseData caseData = CaseData.builder()
            .isMultiParty(NO)
            .generalAppRespondentSolicitors(getRespondentSolicitors())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id")
                                          .email("test@gmail.com").organisationIdentifier("org1").build())
            .makeAppVisibleToRespondents(gaMakeApplicationAvailableCheck)
            .isGaRespondentOneLip(NO)
            .isGaApplicantLip(YES)
            .feePaymentOutcomeDetails(FeePaymentOutcomeDetails
                                          .builder()
                                          .hwfFullRemissionGrantedForGa(NO).build())
            .generalAppHelpWithFees(
                HelpWithFees.builder()
                    .helpWithFeesReferenceNumber("ABC-DEF-IJK")
                    .helpWithFee(YES).build())
            .ccdCaseReference(CCD_CASE_REFERENCE).build();

        HashMap<String, Object> scenarioParams = new HashMap<>();

        when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
        when(gaForLipService.isGaForLip(caseData)).thenReturn(true);
        when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);
        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);

        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        handler.handle(params);

        verify(dashboardApiClient).recordScenario(
            caseData.getCcdCaseReference().toString(),
            SCENARIO_AAA6_GENERAL_APPS_HWF_FEE_PAID_APPLICANT.getScenario(),
            "BEARER_TOKEN",
            ScenarioRequestParams.builder().params(scenarioParams).build()
        );
    }

    @Test
    void shouldUpdateClaimantTaskListIfGaApplicantLipAndFeeIsPaidFullRemission() {

        CaseData caseData = CaseData.builder()
            .isMultiParty(NO)
            .generalAppRespondentSolicitors(getRespondentSolicitors())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id")
                                          .email("test@gmail.com").organisationIdentifier("org1").build())
            .makeAppVisibleToRespondents(gaMakeApplicationAvailableCheck)
            .isGaRespondentOneLip(NO)
            .isGaApplicantLip(YES)
            .feePaymentOutcomeDetails(FeePaymentOutcomeDetails
                                          .builder()
                                          .hwfFullRemissionGrantedForGa(YES).build())
            .generalAppHelpWithFees(
                HelpWithFees.builder()
                    .helpWithFeesReferenceNumber("ABC-DEF-IJK")
                    .helpWithFee(YES).build())
            .ccdCaseReference(CCD_CASE_REFERENCE).build();

        HashMap<String, Object> scenarioParams = new HashMap<>();

        when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
        when(gaForLipService.isGaForLip(caseData)).thenReturn(true);
        when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);
        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);

        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        handler.handle(params);

        verify(dashboardApiClient).recordScenario(
            caseData.getCcdCaseReference().toString(),
            SCENARIO_AAA6_GENERAL_APPS_HWF_FULL_REMISSION_APPLICANT.getScenario(),
            "BEARER_TOKEN",
            ScenarioRequestParams.builder().params(scenarioParams).build()
        );
    }

    @Test
    void shouldNotUpdateIfGaApplicantOrRespondentNotLip() {

        CaseData caseData = CaseDataBuilder.builder()
            .isMultiParty(YesOrNo.NO)
            .generalAppRespondentSolicitors(getRespondentSolicitors())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().id("id")
                                          .email("test@gmail.com").organisationIdentifier("org1").build())
            .makeAppVisibleToRespondents(gaMakeApplicationAvailableCheck)
            .ccdCaseReference(CCD_CASE_REFERENCE)
            .isGaApplicantLip(NO)
            .isGaRespondentOneLip(NO).build();

        HashMap<String, Object> scenarioParams = new HashMap<>();

        when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
        when(gaForLipService.isGaForLip(caseData)).thenReturn(false);
        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);
        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        handler.handle(params);

        verifyNoInteractions(dashboardApiClient);
    }

    @Test
    void shouldCreateDashboardNotificationForRespondentWhenJudgeUncloaksNonUrgentApplication() {

        CaseData caseData = CaseDataBuilder.builder()
            .isMultiParty(NO)
            .isGaApplicantLip(YES)
            .isGaRespondentOneLip(YES)
            .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                 .requestMoreInfoOption(
                                                     GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY).build())
            .ccdCaseReference(CCD_CASE_REFERENCE).build().toBuilder()
            .parentCaseReference(PARENT_CASE_REFERENCE)
            .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(NO).build()).build();

        HashMap<String, Object> scenarioParams = new HashMap<>();

        when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
        when(gaForLipService.isGaForLip(caseData)).thenReturn(true);
        when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);
        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);
        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        handler.handle(params);

        verify(dashboardApiClient).recordScenario(
            caseData.getCcdCaseReference().toString(),
            SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_NONURGENT_UNCLOAKED_RESPONDENT.getScenario(),
            "BEARER_TOKEN",
            ScenarioRequestParams.builder().params(scenarioParams).build()
        );
    }

    @Test
    void shouldCreateDashboardNotificationForRespondentWhenJudgeUncloaksUrgentApplication() {

        CaseData caseData = CaseDataBuilder.builder()
            .isMultiParty(NO)
            .isGaApplicantLip(YES)
            .isGaRespondentOneLip(YES)
            .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                 .requestMoreInfoOption(
                                                     GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY).build())
            .ccdCaseReference(CCD_CASE_REFERENCE).build().toBuilder()
            .parentCaseReference(PARENT_CASE_REFERENCE)
            .generalAppUrgencyRequirement(GAUrgencyRequirement.builder().generalAppUrgency(YES).build()).build();

        HashMap<String, Object> scenarioParams = new HashMap<>();

        when(featureToggleService.isDashboardServiceEnabled()).thenReturn(true);
        when(gaForLipService.isGaForLip(caseData)).thenReturn(true);
        when(mapper.mapCaseDataToParams(any())).thenReturn(scenarioParams);
        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);
        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        handler.handle(params);

        verify(dashboardApiClient).recordScenario(
            caseData.getCcdCaseReference().toString(),
            SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_URGENT_UNCLOAKED_RESPONDENT.getScenario(),
            "BEARER_TOKEN",
            ScenarioRequestParams.builder().params(scenarioParams).build()
        );
    }

    public List<Element<GASolicitorDetailsGAspec>> getRespondentSolicitors() {
        List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

        GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
            .email("test@gmail.com").organisationIdentifier("org2").build();

        GASolicitorDetailsGAspec respondent2 = GASolicitorDetailsGAspec.builder().id("id")
            .email("test@gmail.com").organisationIdentifier("org3").build();

        respondentSols.add(element(respondent1));
        respondentSols.add(element(respondent2));

        return respondentSols;
    }
}
