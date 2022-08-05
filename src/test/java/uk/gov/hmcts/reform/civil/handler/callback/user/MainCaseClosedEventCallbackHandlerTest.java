package uk.gov.hmcts.reform.civil.handler.callback.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;
import uk.gov.hmcts.reform.civil.service.Time;

import java.time.LocalDateTime;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAIN_CASE_CLOSED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_CLOSED;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    MainCaseClosedEventCallbackHandler.class,
    ParentCaseUpdateHelper.class, JacksonAutoConfiguration.class, Time.class
})
class MainCaseClosedEventCallbackHandlerTest extends BaseCallbackHandlerTest {

    @MockBean
    private ParentCaseUpdateHelper parentCaseUpdateHelper;

    @MockBean
    private Time time;

    @Autowired
    private MainCaseClosedEventCallbackHandler handler;

    @Nested
    class AboutToSubmitCallback {

        private LocalDateTime localDateTime;

        @BeforeEach
        void setup() {
            localDateTime = LocalDateTime.now();
            when(time.now()).thenReturn(localDateTime);
        }

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

            assertThat(response.getErrors()).isNull();
            assertThat(response.getState()).isEqualTo(APPLICATION_CLOSED.toString());
            assertThat(response.getData()).extracting("businessProcess").extracting("status").isEqualTo("READY");
            assertThat(response.getData()).extracting("businessProcess").extracting("camundaEvent").isEqualTo(
                "MAIN_CASE_CLOSED");
            assertThat(response.getData()).containsEntry("applicationClosedDate", localDateTime.format(ISO_DATE_TIME));
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

            assertThat(response.getErrors()).isNull();
            assertThat(response.getState()).isNull();
            assertThat(response.getData()).isNull();
        }
    }

    @Nested
    class SubmittedCallback {

        @Test
        void shouldUpdateParentClaimWhenBusinessProcessIsMainCaseClosedAndCaseStatusIsApplicationClosed() {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(APPLICATION_CLOSED)
                .businessProcess(BusinessProcess.ready(MAIN_CASE_CLOSED))
                .build();
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            handler.handle(params);

            verify(parentCaseUpdateHelper, times(1)).updateParentWithGAState(
                caseData,
                APPLICATION_CLOSED.getDisplayedValue()
            );
        }

        @Test
        void shouldNotUpdateParentClaimWhenBusinessProcessIsAvailable() {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .build();
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            handler.handle(params);

            verifyNoInteractions(parentCaseUpdateHelper);
        }

        @Test
        void shouldNotUpdateParentClaimWhenBusinessProcessIsNotMainCaseClosed() {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .businessProcess(BusinessProcess.ready(INITIATE_GENERAL_APPLICATION))
                .build();
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            handler.handle(params);

            verifyNoInteractions(parentCaseUpdateHelper);
        }

        @ParameterizedTest(name = "The application is in {0} state")
        @EnumSource(
            value = CaseState.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"APPLICATION_CLOSED"})
        void shouldNotUpdateParentClaimWhenCaseStateIsNotApplicationClosed(CaseState state) {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(state)
                .businessProcess(BusinessProcess.ready(MAIN_CASE_CLOSED))
                .build();
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            handler.handle(params);

            verifyNoInteractions(parentCaseUpdateHelper);
        }

    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(MAIN_CASE_CLOSED);
    }
}
