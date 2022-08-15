package uk.gov.hmcts.reform.civil.handler.callback.user;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.event.CloseApplicationsEvent;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.VERIFY_AND_CLOSE_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_ADDITIONAL_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;

@ExtendWith(SpringExtension.class)
class VerifyAndCloseApplicationEventCallbackHandlerTest extends BaseCallbackHandlerTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
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
    class SubmitCallback {

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
            ArgumentCaptor<CloseApplicationsEvent> argument = ArgumentCaptor.forClass(CloseApplicationsEvent.class);

            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response).isEqualTo(SubmittedCallbackResponse.builder().build());
            verify(applicationEventPublisher).publishEvent(argument.capture());
            assertThat(1234L).isEqualTo(argument.getValue().getCaseId());
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
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            var response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response).isEqualTo(SubmittedCallbackResponse.builder().build());
            verifyNoInteractions(applicationEventPublisher);
        }

    }

    @Nested
    class AboutToSubmitCallback {

        @Test
        void shouldUpdateParentClaimWhenBusinessProcessIsMainCaseClosedAndCaseStatusIsApplicationClosed() {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(AWAITING_RESPONDENT_RESPONSE)
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            AboutToStartOrSubmitCallbackResponse resp = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(resp).isEqualTo(AboutToStartOrSubmitCallbackResponse.builder().build());
        }
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(VERIFY_AND_CLOSE_APPLICATION);
    }
}
