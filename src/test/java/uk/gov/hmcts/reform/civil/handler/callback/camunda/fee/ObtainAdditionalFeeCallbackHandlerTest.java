package uk.gov.hmcts.reform.civil.handler.callback.camunda.fee;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.config.GeneralAppFeesConfiguration;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.GeneralAppFeesService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.OBTAIN_ADDITIONAL_FEE_VALUE;

@SpringBootTest(classes = {
    AdditionalFeeValueCallbackHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
})
class ObtainAdditionalFeeCallbackHandlerTest extends BaseCallbackHandlerTest {

    private static final Fee FEE167 = Fee.builder().calculatedAmountInPence(
        BigDecimal.valueOf(16700)).code("FEE0444").version("1").build();
    private static final BigDecimal TEST_FEE_AMOUNT_POUNDS_167 = BigDecimal.valueOf(16700);
    @Autowired
    private AdditionalFeeValueCallbackHandler handler;
    private static final String TASK_ID = "ObtainAdditionalFeeValue";
    @MockBean
    private GeneralAppFeesService generalAppFeesService;
    @MockBean
    GeneralAppFeesConfiguration generalAppFeesConfiguration;
    private CallbackParams params;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        when(generalAppFeesConfiguration.getApplicationUncloakAdditionalFee())
            .thenReturn("Some Fee Code");
    }

    @Test
    public void shouldReturnCorrectTaskId() {
        CaseData caseData = CaseDataBuilder.builder().buildFeeValidationCaseData(FEE167, false, false);
        params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        assertThat(handler.camundaActivityId(params)).isEqualTo(TASK_ID);
    }

    @Test
    public void shouldReturnCorrectEvent() {
        CaseData caseData = CaseDataBuilder.builder().buildFeeValidationCaseData(FEE167, false, false);
        params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        assertThat(handler.handledEvents()).contains(OBTAIN_ADDITIONAL_FEE_VALUE);
    }

    @Test
    public void shouldReturnAdditionalFeeValue_WhenApplicationUncloaked() {
        when(generalAppFeesService.getFeeForGA(any()))
            .thenReturn(Fee.builder().calculatedAmountInPence(
                TEST_FEE_AMOUNT_POUNDS_167).code("test_fee_code").version("1").build());

        Fee expectedFeeDto = Fee.builder()
            .calculatedAmountInPence(TEST_FEE_AMOUNT_POUNDS_167)
            .code("test_fee_code")
            .version("1")
            .build();
        var caseData = CaseDataBuilder.builder()
            .requestForInformationApplicationWithOutNoticeToWithNotice()
            .build();
        params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(extractAdditionalUncloakFee(response)).isEqualTo(expectedFeeDto);
    }

    @Test
    public void shouldNotGetAdditionalFeeValue_WhenApplicationIsNotUncloaked() {
        when(generalAppFeesService.getFeeForGA(any()))
            .thenReturn(Fee.builder().calculatedAmountInPence(
                BigDecimal.valueOf(16700)).code("").version("1").build());

        var caseData = CaseDataBuilder.builder()
            .requestForInforationApplication()
            .build();
        params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        verify(generalAppFeesService, never()).getFeeForGA(any());
    }

    @Test
    void shouldThrowError_whenRunTimeExceptionHappens() {

        when(generalAppFeesService.getFeeForGA(any()))
            .thenThrow(new RuntimeException("Some Exception"));

        var caseData = CaseDataBuilder.builder()
            .requestForInformationApplicationWithOutNoticeToWithNotice()
            .build();
        params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        Exception exception = assertThrows(RuntimeException.class, () -> handler.handle(params));

        assertThat(exception.getMessage()).isEqualTo("Some Exception");
    }

    private Fee extractAdditionalUncloakFee(AboutToStartOrSubmitCallbackResponse response) {
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        if (responseCaseData.getGeneralAppPBADetails() != null
            && responseCaseData.getGeneralAppPBADetails().getFee() != null) {
            return responseCaseData.getGeneralAppPBADetails().getFee();
        }

        return null;
    }
}
