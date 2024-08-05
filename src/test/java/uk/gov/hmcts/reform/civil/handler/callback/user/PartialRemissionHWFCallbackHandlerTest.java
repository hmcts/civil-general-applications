package uk.gov.hmcts.reform.civil.handler.callback.user;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.PARTIAL_REMISSION_HWF_GA;
import static uk.gov.hmcts.reform.civil.handler.callback.user.PartialRemissionHWFCallbackHandler.ERR_MSG_REMISSION_AMOUNT_LESS_THAN_ADDITIONAL_FEE;
import static uk.gov.hmcts.reform.civil.handler.callback.user.PartialRemissionHWFCallbackHandler.ERR_MSG_REMISSION_AMOUNT_LESS_THAN_GA_FEE;
import static uk.gov.hmcts.reform.civil.handler.callback.user.PartialRemissionHWFCallbackHandler.ERR_MSG_REMISSION_AMOUNT_LESS_THAN_ZERO;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesDetails;

import java.math.BigDecimal;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PartialRemissionHWFCallbackHandlerTest extends BaseCallbackHandlerTest {

    private ObjectMapper objectMapper;
    private PartialRemissionHWFCallbackHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        handler = new PartialRemissionHWFCallbackHandler(objectMapper);
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(PARTIAL_REMISSION_HWF_GA);
    }

    @Nested
    class AboutToSubmitCallback {

        @Test
        void shouldCallPartialRemissionHwfEventWhenFeeTypeIsGa() {
            CaseData caseData = CaseData.builder()
                    .generalAppPBADetails(GAPbaDetails.builder().fee(
                                    Fee.builder()
                                            .calculatedAmountInPence(BigDecimal.valueOf(10000)).code("OOOCM002").build())
                            .build())
                    .gaHwfDetails(HelpWithFeesDetails.builder()
                            .remissionAmount(BigDecimal.valueOf(1000))
                            .hwfCaseEvent(PARTIAL_REMISSION_HWF_GA)
                            .build())
                    .hwfFeeType(FeeType.APPLICATION)
                    .build();

            CallbackParams params = callbackParamsOf(caseData, CaseEvent.PARTIAL_REMISSION_HWF_GA, CallbackType.ABOUT_TO_SUBMIT);
            //When
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            //Then
            CaseData updatedData = objectMapper.convertValue(response.getData(), CaseData.class);
            assertThat(response.getErrors()).isNull();
            assertThat(updatedData.getGaHwfDetails().getRemissionAmount()).isEqualTo(BigDecimal.valueOf(1000));
            assertThat(updatedData.getGaHwfDetails().getHwfCaseEvent()).isEqualTo(PARTIAL_REMISSION_HWF_GA);
        }

        @Test
        void shouldCallPartialRemissionHwfEventWhenFeeTypeIsHearing() {
            CaseData caseData = CaseData.builder()
                    .generalAppPBADetails(GAPbaDetails.builder().fee(
                                    Fee.builder()
                                            .calculatedAmountInPence(BigDecimal.valueOf(10000)).code("OOOCM002").build())
                            .build())
                    .additionalHwfDetails(HelpWithFeesDetails.builder()
                            .remissionAmount(BigDecimal.valueOf(1000))
                            .hwfCaseEvent(PARTIAL_REMISSION_HWF_GA)
                            .build())
                    .hwfFeeType(FeeType.ADDITIONAL)
                    .build();
            CallbackParams params = callbackParamsOf(caseData, CaseEvent.PARTIAL_REMISSION_HWF_GA, CallbackType.ABOUT_TO_SUBMIT);

            //When
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            //Then
            CaseData updatedData = objectMapper.convertValue(response.getData(), CaseData.class);
            assertThat(response.getErrors()).isNull();
            assertThat(updatedData.getAdditionalHwfDetails().getRemissionAmount()).isEqualTo(BigDecimal.valueOf(1000));
            assertThat(updatedData.getAdditionalHwfDetails().getHwfCaseEvent()).isEqualTo(PARTIAL_REMISSION_HWF_GA);
        }
    }

    @Test
    void shouldPopulateErrorWhenApplicationRemissionAmountIsNegative() {
        //Given
        CaseData caseData = CaseData.builder()
                .generalAppPBADetails(GAPbaDetails.builder().fee(
                                Fee.builder()
                                        .calculatedAmountInPence(BigDecimal.valueOf(30000))
                                        .code("OOOCM002").build())
                        .build())
                .gaHwfDetails(HelpWithFeesDetails.builder()
                        .remissionAmount(BigDecimal.valueOf(-1000))
                        .build())
                .hwfFeeType(FeeType.APPLICATION)
                .build();

        CallbackParams params = callbackParamsOf(caseData, CallbackType.MID, "remission-amount");

        //When
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        //Then
        assertThat(response.getErrors()).containsExactly(ERR_MSG_REMISSION_AMOUNT_LESS_THAN_ZERO);
    }

    @Test
    void shouldPopulateErrorWhenAdditionalRemissionAmountIsNegative() {
        //Given
        CaseData caseData = CaseData.builder()
                .generalAppPBADetails(GAPbaDetails.builder().fee(
                                Fee.builder()
                                        .calculatedAmountInPence(BigDecimal.valueOf(30000))
                                        .code("OOOCM002").build())
                        .build())
                .additionalHwfDetails(HelpWithFeesDetails.builder()
                        .remissionAmount(BigDecimal.valueOf(-1000))
                        .build())
                .hwfFeeType(FeeType.ADDITIONAL)

                .build();
        CallbackParams params = callbackParamsOf(caseData, CallbackType.MID, "remission-amount");

        //When
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        //Then
        assertThat(response.getErrors()).containsExactly(ERR_MSG_REMISSION_AMOUNT_LESS_THAN_ZERO);
    }

    @ParameterizedTest
    @MethodSource("provideFeeTypes")
    void shouldPopulateErrorWhenRemissionAmountIsNotValidForDifferentFeeTypes(FeeType feeType, String errMsg) {
        //Given
        CaseData caseData = CaseData.builder()
                .generalAppPBADetails(GAPbaDetails.builder().fee(
                                Fee.builder()
                                        .calculatedAmountInPence(BigDecimal.valueOf(30000))
                                        .code("OOOCM002").build())
                        .build())
                .additionalHwfDetails(HelpWithFeesDetails.builder()
                        .remissionAmount(BigDecimal.valueOf(35000))
                        .build())
                .gaHwfDetails(HelpWithFeesDetails.builder()
                        .remissionAmount(BigDecimal.valueOf(35000))
                        .build())
                .hwfFeeType(feeType)
                .build();
        CallbackParams params = callbackParamsOf(caseData, CallbackType.MID, "remission-amount");

        //When
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

        //Then
        assertThat(response.getErrors()).containsExactly(errMsg);
    }

    private static Stream<Arguments> provideFeeTypes() {
        return Stream.of(
                Arguments.of(FeeType.APPLICATION, ERR_MSG_REMISSION_AMOUNT_LESS_THAN_GA_FEE),
                Arguments.of(FeeType.ADDITIONAL, ERR_MSG_REMISSION_AMOUNT_LESS_THAN_ADDITIONAL_FEE)
        );
    }

}
