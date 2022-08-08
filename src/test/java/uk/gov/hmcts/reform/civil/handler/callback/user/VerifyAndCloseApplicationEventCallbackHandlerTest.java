package uk.gov.hmcts.reform.civil.handler.callback.user;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAIN_CASE_CLOSED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.VERIFY_AND_CLOSE_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_ADDITIONAL_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    VerifyAndCloseApplicationEventCallbackHandler.class, CoreCaseDataService.class,
})
class VerifyAndCloseApplicationEventCallbackHandlerTest extends BaseCallbackHandlerTest {

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @Autowired
    private VerifyAndCloseApplicationEventCallbackHandler handler;

    @Nested
    class AboutToStartCallback {
        @Test
        void shouldReturnEmptyCallbackResponseForAboutToStart() {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(AWAITING_ADDITIONAL_INFORMATION)
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_START);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isEqualTo(AboutToStartOrSubmitCallbackResponse.builder().build());
        }
    }

    @Nested
    class AboutToSubmitCallback {

        @ParameterizedTest(name = "The application is in {0} state")
        @EnumSource(
            value = CaseState.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"APPLICATION_CLOSED", "PROCEEDS_IN_HERITAGE", "ORDER_MADE",
                "LISTING_FOR_A_HEARING", "APPLICATION_DISMISSED"})
        void shouldRespondWithStateChangedWhenApplicationIsLive(CaseState state) {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(state).build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isEqualTo(AboutToStartOrSubmitCallbackResponse.builder().build());
            verify(coreCaseDataService, times(1))
                .triggerEvent(1234L, MAIN_CASE_CLOSED);
        }

        @ParameterizedTest(name = "The application is in {0} state")
        @EnumSource(
            value = CaseState.class,
            names = {"APPLICATION_CLOSED", "PROCEEDS_IN_HERITAGE", "ORDER_MADE",
                "LISTING_FOR_A_HEARING", "APPLICATION_DISMISSED"})
        void shouldRespondWithoutStateChangedWhenApplicationIsNotLive(CaseState state) {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(state).build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isEqualTo(AboutToStartOrSubmitCallbackResponse.builder().build());
            verifyNoInteractions(coreCaseDataService);
        }

    }

    @Nested
    class SubmittedCallback {

        @Test
        void shouldUpdateParentClaimWhenBusinessProcessIsMainCaseClosedAndCaseStatusIsApplicationClosed() {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(AWAITING_RESPONDENT_RESPONSE)
                .build();
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response).isEqualTo(SubmittedCallbackResponse.builder().build());
        }
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(VERIFY_AND_CLOSE_APPLICATION);
    }
}
