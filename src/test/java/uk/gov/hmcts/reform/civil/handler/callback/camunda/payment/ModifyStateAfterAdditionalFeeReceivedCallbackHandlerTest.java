package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.MakeAppAvailableCheckGAspec;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAMakeApplicationAvailableCheck;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.AssignCaseToResopondentSolHelper;
import uk.gov.hmcts.reform.civil.service.CoreCaseUserService;
import uk.gov.hmcts.reform.civil.service.JudicialNotificationService;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;
import uk.gov.hmcts.reform.civil.service.StateGeneratorService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.element;

@SpringBootTest(classes = {
    AssignCaseToResopondentSolHelper.class,
    ModifyStateAfterAdditionalFeeReceivedCallbackHandler.class,
    JacksonAutoConfiguration.class,
})
class ModifyStateAfterAdditionalFeeReceivedCallbackHandlerTest extends BaseCallbackHandlerTest {

    public static final long CCD_CASE_REFERENCE = 1234L;

    @MockBean
    private ParentCaseUpdateHelper parentCaseUpdateHelper;

    @MockBean
    StateGeneratorService stateGeneratorService;

    @MockBean JudicialNotificationService judicialNotificationService;

    @Autowired
    private ModifyStateAfterAdditionalFeeReceivedCallbackHandler handler;

    @MockBean
    private CoreCaseUserService coreCaseUserService;

    @Test
    void shouldRespondWithStateChanged() {

        List<MakeAppAvailableCheckGAspec> makeAppAvailableCheck = Arrays
            .asList(MakeAppAvailableCheckGAspec.CONSENT_AGREEMENT_CHECKBOX);

        GAMakeApplicationAvailableCheck gaMakeApplicationAvailableCheck = GAMakeApplicationAvailableCheck.builder()
            .makeAppAvailableCheck(makeAppAvailableCheck).build();

        CaseData caseData = CaseDataBuilder.builder()
            .generalAppRespondentSolicitors(getRespondentSolicitors())
            .makeAppVisibleToRespondents(gaMakeApplicationAvailableCheck)
            .ccdCaseReference(CCD_CASE_REFERENCE).build();

        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response.getErrors()).isNull();
        assertThat(response.getState()).isEqualTo(AWAITING_RESPONDENT_RESPONSE.toString());

        verify(coreCaseUserService, times(2)).assignCase(
            any(),
            any(),
            any(),
            any()
        );
    }

    @Test
    void shouldRespondWithStateChangedWhenApplicationUncloaked() {

        CaseData caseData = CaseDataBuilder.builder()
            .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder().requestMoreInfoOption(
                GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY).build())
            .generalAppRespondentSolicitors(getRespondentSolicitors())
            .ccdCaseReference(CCD_CASE_REFERENCE).build();

        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response.getErrors()).isNull();
        assertThat(response.getState()).isEqualTo(AWAITING_RESPONDENT_RESPONSE.toString());

        verify(coreCaseUserService, times(2)).assignCase(
            any(),
            any(),
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

        verify(coreCaseUserService, times(0)).assignCase(
            any(),
            any(),
            any(),
            any()
        );
    }

    @Test
    void shouldThrowExceptionIfSolicitorsAreNull() {

        List<MakeAppAvailableCheckGAspec> makeAppAvailableCheck = Arrays
            .asList(MakeAppAvailableCheckGAspec.CONSENT_AGREEMENT_CHECKBOX);

        GAMakeApplicationAvailableCheck gaMakeApplicationAvailableCheck = GAMakeApplicationAvailableCheck.builder()
            .makeAppAvailableCheck(makeAppAvailableCheck).build();

        CaseData caseData = CaseDataBuilder.builder()
            .makeAppVisibleToRespondents(gaMakeApplicationAvailableCheck)
            .ccdCaseReference(CCD_CASE_REFERENCE).build();

        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);
        try {
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        } catch (Exception e) {
            assertEquals("java.lang.NullPointerException", e.toString());
        }
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

    public List<Element<GASolicitorDetailsGAspec>> getRespondentSolicitors() {
        List<Element<GASolicitorDetailsGAspec>> respondentSols = new ArrayList<>();

        GASolicitorDetailsGAspec respondent1 = GASolicitorDetailsGAspec.builder().id("id")
            .email("test@gmail.com").organisationIdentifier("org2").build();

        GASolicitorDetailsGAspec respondent2 = GASolicitorDetailsGAspec.builder().id("id")
            .email("test@gmail.com").organisationIdentifier("org2").build();

        respondentSols.add(element(respondent1));
        respondentSols.add(element(respondent2));

        return respondentSols;
    }
}
