package uk.gov.hmcts.reform.civil.handler.callback.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPONDENT_RESPONSE_DEADLINE_CHECK;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;
import uk.gov.hmcts.reform.civil.service.DocUploadDashboardNotificationService;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    RespondentResponseDeadlineCallbackHandler.class,
    DashboardApiClient.class,
    JacksonAutoConfiguration.class,
    ValidationAutoConfiguration.class
})
public class RespondentResponseDeadlineCallbackHandlerTest {

    @Autowired
    private RespondentResponseDeadlineCallbackHandler handler;
    @MockBean
    private DashboardNotificationsParamsMapper mapper;
    @MockBean
    private DashboardApiClient dashboardApiClient;

    @MockBean
    private DocUploadDashboardNotificationService dashboardNotificationService;

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(RESPONDENT_RESPONSE_DEADLINE_CHECK);
    }

    @Nested
    class AboutToSubmitCallback {
        @BeforeEach
        void setup() {
            when(dashboardApiClient.recordScenario(any(), any(), anyString(), any())).thenReturn(ResponseEntity.of(
                Optional.empty()));
        }

        @Test
        void shouldDeleteNotifications_whenInvoked() {
            CaseData caseData = CaseDataBuilder.builder().build();
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).request(
                CallbackRequest.builder().caseDetails(CaseDetails.builder().id(123456L).build())
                    .eventId(RESPONDENT_RESPONSE_DEADLINE_CHECK.name()).build()
            ).build();

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            assertThat(response.getData()).extracting("respondentResponseDeadlineChecked")
                .isEqualTo("Yes");
            verify(dashboardNotificationService).createResponseDashboardNotification(any(), eq("RESPONDENT"), anyString());
        }
    }
}
