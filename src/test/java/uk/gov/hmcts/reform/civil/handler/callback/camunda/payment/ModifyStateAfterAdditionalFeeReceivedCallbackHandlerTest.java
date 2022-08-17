package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.JudicialNotificationService;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;
import uk.gov.hmcts.reform.civil.service.StateGeneratorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;

@SpringBootTest(classes = {
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

    @Test
    void shouldRespondWithStateChanged() {
        CaseData caseData = CaseDataBuilder.builder().ccdCaseReference(CCD_CASE_REFERENCE).build();
        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        when(stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(any()))
            .thenReturn(AWAITING_RESPONDENT_RESPONSE);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response.getErrors()).isNull();
        assertThat(response.getState()).isEqualTo(AWAITING_RESPONDENT_RESPONSE.getDisplayedValue());
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
}
