package uk.gov.hmcts.reform.civil.handler.callback.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.config.ClaimIssueConfiguration;
import uk.gov.hmcts.reform.civil.config.MockDatabaseConfiguration;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.Time;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;
import uk.gov.hmcts.reform.civil.validation.OrgPolicyValidator;
import uk.gov.hmcts.reform.civil.validation.ValidateEmailService;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.handler.callback.user.CreateGeneralApplicationHandler.CONFIRMATION_SUMMARY;

@SpringBootTest(classes = {
    CreateGeneralApplicationHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    ClaimIssueConfiguration.class,
    MockDatabaseConfiguration.class,
    ValidationAutoConfiguration.class,
    OrgPolicyValidator.class,
    StateFlowEngine.class,
    StateFlowEngine.class,
    ValidateEmailService.class},
    properties = {"reference.database.enabled=false"})
class CreateGeneralApplicationHandlerTest extends BaseCallbackHandlerTest {

    public static final String REFERENCE_NUMBER = "000DC001";

    @MockBean
    private Time time;

    @MockBean
    private IdamClient idamClient;

    @Autowired
    private CreateGeneralApplicationHandler handler;

    @Value("${civil.response-pack-url}")
    private String responsePackLink;

    @Nested
    class AboutToSubmitCallback {

        private CallbackParams params;
        private CaseData caseData;
        private String userId;

        private static final String EMAIL = "example@email.com";
        private static final String DIFFERENT_EMAIL = "other_example@email.com";
        private final LocalDateTime submittedDate = LocalDateTime.now();

        @BeforeEach
        void setup() {
            caseData = CaseDataBuilder.builder().atStateClaimDraft().build();
            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            userId = UUID.randomUUID().toString();

            given(idamClient.getUserDetails(any()))
                .willReturn(UserDetails.builder().email(EMAIL).id(userId).build());

            given(time.now()).willReturn(submittedDate);
        }

        @Test
        void shouldUpdateBusinessProcess_whenInvoked() {
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getData())
                .extracting("businessProcess")
                .extracting("camundaEvent", "status")
                .containsOnly(INITIATE_GENERAL_APPLICATION.name(), "READY");
        }

        @Nested
        class IdamEmail {

            @BeforeEach
            void setup() {
                caseData = CaseDataBuilder.builder().atStateClaimDraft().build();
                userId = UUID.randomUUID().toString();

                given(idamClient.getUserDetails(any()))
                    .willReturn(UserDetails.builder().email(EMAIL).id(userId).build());
            }
        }

        @Nested
        class SubmittedCallback {

            @Nested
            class RespondentsDoNotHaveLegalRepresentation {

                @Test
                void shouldReturnExpectedSubmittedCallbackResponse() {
                    CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().build();
                    CallbackParams params = callbackParamsOf(caseData, SUBMITTED);
                    SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);

                    String body = format(
                        CONFIRMATION_SUMMARY,
                        format("/cases/case-details/%s#CaseDocuments", CASE_ID));

                    assertThat(response).usingRecursiveComparison().isEqualTo(
                        SubmittedCallbackResponse.builder()
                            .confirmationHeader(format(
                                "You have made an application"
                            ))
                            .confirmationBody(body)
                            .build());
                }
            }
        }
    }
}
