package uk.gov.hmcts.reform.civil.handler.callback.user;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFees;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.NO_REMISSION_HWF_GA;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_ADD_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;

@SpringBootTest(classes = {
    NoRemissionHWFCallbackHandler.class,
    JacksonAutoConfiguration.class})
public class NoRemissionHWFCallbackHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    NoRemissionHWFCallbackHandler handler;
    @Autowired
    ObjectMapper objectMapper;
    @MockBean
    CaseDetailsConverter caseDetailsConverter;

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(NO_REMISSION_HWF_GA);
    }

    @Nested
    class AboutToStart {
        @Test
        void shouldSetUpDefaultData_WhileHwfFeeTypeIsBlank() {
            CaseData caseData = CaseData.builder()
                    .ccdState(AWAITING_RESPONDENT_RESPONSE)
                    .generalAppHelpWithFees(HelpWithFees.builder().build()).build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            CallbackParams params = callbackParamsOf(caseData, CallbackType.ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
            assertThat(responseCaseData.getHwfFeeType()).isEqualTo(FeeType.APPLICATION);
        }

        @Test
        void shouldSetUpAddData_WhileHwfFeeTypeIsBlank() {
            CaseData caseData = CaseData.builder()
                    .ccdState(APPLICATION_ADD_PAYMENT)
                    .generalAppHelpWithFees(HelpWithFees.builder().build()).build();
            when(caseDetailsConverter.toCaseData(any())).thenReturn(caseData);
            CallbackParams params = callbackParamsOf(caseData, CallbackType.ABOUT_TO_START);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
            assertThat(responseCaseData.getHwfFeeType()).isEqualTo(FeeType.ADDITIONAL);
        }
    }
}
