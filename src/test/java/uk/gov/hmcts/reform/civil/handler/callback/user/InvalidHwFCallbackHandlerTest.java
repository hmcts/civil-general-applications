package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.citizenui.HelpWithFees;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_ADD_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;

@ExtendWith(MockitoExtension.class)
public class InvalidHwFCallbackHandlerTest {

    private ObjectMapper objectMapper;
    private InvalidHwFCallbackHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        handler = new InvalidHwFCallbackHandler(objectMapper);
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(CaseEvent.INVALID_HWF_REFERENCE_GA);
    }

    @Nested
    class AboutToStart {
        @Test
        void shouldSetUpDefaultData_WhileHwfFeeTypeIsBlank() {
            CaseData caseData = CaseData.builder()
                .ccdState(AWAITING_RESPONDENT_RESPONSE)
                .generalAppHelpWithFees(HelpWithFees.builder().build()).build();

            CallbackParams params = CallbackParams.builder()
                .type(CallbackType.ABOUT_TO_START)
                .caseData(caseData)
                .build();

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getHwfFeeType()).isEqualTo(FeeType.APPLICATION);
        }

        @Test
        void shouldSetUpAddData_WhileHwfFeeTypeIsBlank() {
            CaseData caseData = CaseData.builder()
                .ccdState(APPLICATION_ADD_PAYMENT)
                .generalAppHelpWithFees(HelpWithFees.builder().build()).build();

            CallbackParams params = CallbackParams.builder()
                .type(CallbackType.ABOUT_TO_START)
                .caseData(caseData)
                .build();

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);

            assertThat(responseCaseData.getHwfFeeType()).isEqualTo(FeeType.ADDITIONAL);
        }
    }
}
