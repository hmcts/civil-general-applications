package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    MoveToJudicialDecisionStateEventCallbackHandler.class,
    ParentCaseUpdateHelper.class
})
class MoveToJudicialDecisionStateEventCallbackHandlerTest extends BaseCallbackHandlerTest {

    @MockBean
    private ParentCaseUpdateHelper parentCaseUpdateHelper;

    @Autowired
    private MoveToJudicialDecisionStateEventCallbackHandler handler;

    @Nested
    class AboutToSubmitCallback {

        @Test
        void shouldRespondWithStateChanged() {
            CaseData caseData = CaseDataBuilder.builder().ccdCaseReference(1234L).build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isNull();
            assertThat(response.getState()).isEqualTo(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION.toString());
        }
    }

    @Nested
    class SubmittedCallback {

        @Test
        void shouldDispatchBusinessProcess_whenStatusIsReady() {
            CaseData caseData = CaseDataBuilder.builder().ccdCaseReference(1234L).build();
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            handler.handle(params);

            verify(parentCaseUpdateHelper, times(1)).updateParentWithGAState(
                caseData,
                APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION.getDisplayedValue()
            );
        }
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION);
    }
}
