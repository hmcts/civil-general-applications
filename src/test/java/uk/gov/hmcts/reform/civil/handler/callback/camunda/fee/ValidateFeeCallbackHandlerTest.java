package uk.gov.hmcts.reform.civil.handler.callback.camunda.fee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.config.GeneralAppFeesConfiguration;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.GeneralAppFeesService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.VALIDATE_FEE_GASPEC;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.STAY_THE_CLAIM;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.VARY_JUDGEMENT;
import static uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes.VARY_ORDER;

@SpringBootTest(classes = {
    ValidateFeeCallbackHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class ValidateFeeCallbackHandlerTest extends BaseCallbackHandlerTest {

    @MockBean
    private GeneralAppFeesService feesService;

    @Autowired
    private ValidateFeeCallbackHandler handler;
    public static final String VERSION = "1";
    private static final Fee FEE108 = Fee.builder().calculatedAmountInPence(
        BigDecimal.valueOf(10800)).code("FEE0443").version(VERSION).build();
    private static final Fee FEE275 = Fee.builder().calculatedAmountInPence(
        BigDecimal.valueOf(27500)).code("FEE0442").version(VERSION).build();
    private static final Fee FEE14 = Fee.builder().calculatedAmountInPence(
        BigDecimal.valueOf(1400)).code("FEE0458").version("2").build();

    private static final String ERROR_MESSAGE_NO_FEE_IN_CASEDATA = "Application case data does not have fee details";
    private static final String ERROR_MESSAGE_FEE_CHANGED = "Fee has changed since application was submitted. "
        + "It needs to be validated again";
    private static final String TASK_ID = "GeneralApplicationValidateFee";

    private CallbackParams params;

    @Nested
    class MakePBAPayments {

        @Test
        void shouldReturnErrors_whenCaseDataDoesNotHavePBADetails() {
            CaseData caseData = CaseDataBuilder.builder().build();
            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            when(feesService.getFeeForGA(any()))
                .thenReturn(Fee.builder().calculatedAmountInPence(
                    BigDecimal.valueOf(10800)).code("").version(VERSION).build());

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(feesService).getFeeForGA(any());
            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(ERROR_MESSAGE_NO_FEE_IN_CASEDATA);
        }

        @Test
        void shouldReturnErrors_whenCaseDataDoesNotHaveFeeDetails() {
            CaseData caseData = CaseDataBuilder.builder().gaPbaDetails(GAPbaDetails.builder().build()).build();
            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            when(feesService.getFeeForGA(any()))
                .thenReturn(Fee.builder().calculatedAmountInPence(
                    BigDecimal.valueOf(10800)).code("").version(VERSION).build());

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(feesService).getFeeForGA(any());
            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(ERROR_MESSAGE_NO_FEE_IN_CASEDATA);
        }

        @Test
        void shouldReturnErrors_whenConsentedApplicationWithDifferentFeesOnCaseDataAndFeeFromFeeService() {

            CaseData caseData = CaseDataBuilder.builder().buildFeeValidationCaseData(FEE108, true, false);
            when(feesService.getFeeForGA(any()))
                .thenReturn(FEE275);
            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(feesService).getFeeForGA(caseData);
            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors()).contains(ERROR_MESSAGE_FEE_CHANGED);
        }

        @Test
        void shouldReturnNoErrors_whenWithNoticeApplicationSameFeesOnCaseDataAndFeeFromFeeService() {
            CaseData caseData = CaseDataBuilder.builder().buildFeeValidationCaseData(FEE275, false, true);
            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            when(feesService.getFeeForGA(any()))
                .thenReturn(FEE275);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            verify(feesService).getFeeForGA(caseData);
            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldReturnNoErrors_whenNotConsentedNotifiedApplicationIsBeingMade() {

            CaseData caseData =  CaseDataBuilder.builder().buildFeeValidationCaseData(FEE108, false, false);
            when(feesService.getFeeForGA(caseData))
                .thenReturn(FEE108);

            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(feesService).getFeeForGA(caseData);
            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void returnsCorrectTaskId() {
            CaseData caseData = CaseDataBuilder.builder().buildFeeValidationCaseData(FEE108, false, false);
            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            assertThat(handler.camundaActivityId()).isEqualTo(TASK_ID);
        }

        @Test
        void returnsCorrectEvents() {
            CaseData caseData = CaseDataBuilder.builder().buildFeeValidationCaseData(FEE108, false, false);
            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            assertThat(handler.handledEvents()).contains(VALIDATE_FEE_GASPEC);
        }

    }
}
