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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;

@SpringBootTest(classes = {
    ModifyStateAfterAdditionalFeeReceivedCallbackHandler.class,
    JacksonAutoConfiguration.class,
})
class ModifyStateAfterAdditionalFeeReceivedCallbackHandlerTest extends BaseCallbackHandlerTest {

    @MockBean
    private ParentCaseUpdateHelper parentCaseUpdateHelper;

    @MockBean JudicialNotificationService judicialNotificationService;
    @Autowired
    private ModifyStateAfterAdditionalFeeReceivedCallbackHandler handler;

    @Test
    void shouldRespondWithStateChanged() {
        CaseData caseData = CaseDataBuilder.builder().ccdCaseReference(1234L).build();
        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        assertThat(response.getErrors()).isNull();
        assertThat(response.getState()).isEqualTo(ORDER_MADE.toString());
    }

    @Test
    void shouldDispatchBusinessProcess_whenStatusIsReady() {
        CaseData caseData = CaseDataBuilder.builder().ccdCaseReference(1234L).build();
        CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

        handler.handle(params);

        verify(parentCaseUpdateHelper, times(1)).updateParentWithGAState(
            caseData,
            ORDER_MADE.getDisplayedValue()
        );
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID);
    }
}
