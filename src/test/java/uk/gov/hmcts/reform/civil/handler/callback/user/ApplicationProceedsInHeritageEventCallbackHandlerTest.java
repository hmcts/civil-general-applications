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
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.Time;

import java.time.LocalDateTime;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPLICATION_PROCEEDS_IN_HERITAGE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PROCEEDS_IN_HERITAGE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    ApplicationProceedsInHeritageEventCallbackHandler.class, JacksonAutoConfiguration.class, Time.class
})
class ApplicationProceedsInHeritageEventCallbackHandlerTest  extends BaseCallbackHandlerTest {

    @MockBean
    private Time time;

    @Autowired
    private ApplicationProceedsInHeritageEventCallbackHandler handler;

    @Nested
    class AboutToStartCallback {
        @Test
        void shouldReturnEmptyCallbackResponseForAboutToStart() {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(PROCEEDS_IN_HERITAGE)
                .businessProcess(BusinessProcess.ready(APPLICATION_PROCEEDS_IN_HERITAGE))
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_START);

            CallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response).isEqualTo(AboutToStartOrSubmitCallbackResponse.builder().build());
        }
    }

    @Nested
    class AboutToSubmitCallback {

        private LocalDateTime localDateTime;

        @BeforeEach
        void setup() {
            localDateTime = LocalDateTime.now();
            when(time.now()).thenReturn(localDateTime);
        }

        @ParameterizedTest(name = "The application is in {0} state")
        @EnumSource(value = CaseState.class)
        void shouldRespondWithStateChangedWhenApplicationIsLive(CaseState state) {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(state).build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isNull();
            assertThat(response.getState()).isEqualTo(PROCEEDS_IN_HERITAGE.toString());
            assertThat(response.getData()).extracting("businessProcess").extracting("status").isEqualTo("READY");
            assertThat(response.getData()).extracting("businessProcess").extracting("camundaEvent").isEqualTo(
                "APPLICATION_PROCEEDS_IN_HERITAGE");
            assertThat(response.getData()).containsEntry("applicationTakenOfflineDate",
                                                         localDateTime.format(ISO_DATE_TIME));
        }
    }

    @Nested
    class SubmittedCallback {

        @Test
        void shouldUpdateParentClaimWhenBusinessProcessIsMainCaseClosedAndCaseStatusIsApplicationClosed() {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(PROCEEDS_IN_HERITAGE)
                .businessProcess(BusinessProcess.ready(APPLICATION_PROCEEDS_IN_HERITAGE))
                .build();
            CallbackParams params = callbackParamsOf(caseData, SUBMITTED);

            SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response).isEqualTo(SubmittedCallbackResponse.builder().build());
        }
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(APPLICATION_PROCEEDS_IN_HERITAGE);
    }
}
