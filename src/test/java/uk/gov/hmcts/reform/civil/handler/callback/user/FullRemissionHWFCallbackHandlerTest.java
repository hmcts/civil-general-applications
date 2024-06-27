package uk.gov.hmcts.reform.civil.handler.callback.user;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.FULL_REMISSION_HWF_GA;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesDetails;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FullRemissionHWFCallbackHandlerTest extends BaseCallbackHandlerTest {

    private FullRemissionHWFCallbackHandler handler;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        handler = new FullRemissionHWFCallbackHandler(mapper);
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(FULL_REMISSION_HWF_GA);
    }

    @Nested
    class AboutToSubmitCallback {
        @Test
        void shouldUpdateFullRemissionData_GaFee() {
            CaseData caseData = CaseData.builder()
                .generalAppPBADetails(GAPbaDetails.builder().fee(
                        Fee.builder()
                            .calculatedAmountInPence(BigDecimal.valueOf(10000)).code("OOOCM002").build())
                        .build())
                .gaHwfDetails(HelpWithFeesDetails.builder().build())
                .hwfFeeType(FeeType.APPLICATION)
                .build();
            CallbackParams params = callbackParamsOf(caseData, CallbackType.ABOUT_TO_SUBMIT);
            //When
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            //Then
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);
            assertThat(updatedData.getGaHwfDetails().getRemissionAmount()).isEqualTo(BigDecimal.valueOf(10000));
            assertThat(updatedData.getGaHwfDetails().getHwfCaseEvent()).isEqualTo(FULL_REMISSION_HWF_GA);
        }

        @Test
        void shouldUpdateFullRemissionDataWithDetailsNull_GaFee() {
            CaseData caseData = CaseData.builder()
                .generalAppPBADetails(GAPbaDetails.builder().fee(
                    Fee.builder()
                        .calculatedAmountInPence(BigDecimal.valueOf(10000)).code("OOOCM002").build())
                    .build())
                .gaHwfDetails(HelpWithFeesDetails.builder().build())
                .hwfFeeType(FeeType.APPLICATION)
                .build();
            CallbackParams params = callbackParamsOf(caseData, CallbackType.ABOUT_TO_SUBMIT);
            //When
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            //Then
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);
            assertThat(updatedData.getGaHwfDetails().getRemissionAmount()).isEqualTo(BigDecimal.valueOf(10000));
            assertThat(updatedData.getGaHwfDetails().getHwfCaseEvent()).isEqualTo(FULL_REMISSION_HWF_GA);
        }

        @Test
        void shouldUpdateFullRemissionData_Additional() {
            CaseData caseData = CaseData.builder()
                    .generalAppPBADetails(GAPbaDetails.builder().fee(
                                    Fee.builder()
                                            .calculatedAmountInPence(BigDecimal.valueOf(30000))
                                            .code("OOOCM002").build())
                            .build())
                .additionalHwfDetails(HelpWithFeesDetails.builder().build())
                .hwfFeeType(FeeType.ADDITIONAL)
                .build();
            CallbackParams params = callbackParamsOf(caseData, CallbackType.ABOUT_TO_SUBMIT);
            //When
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            //Then
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);
            assertThat(updatedData.getAdditionalHwfDetails().getRemissionAmount()).isEqualTo(BigDecimal.valueOf(30000));
            assertThat(updatedData.getAdditionalHwfDetails().getHwfCaseEvent()).isEqualTo(FULL_REMISSION_HWF_GA);
        }

        @Test
        void shouldUpdateFullRemissionDataWithDetailsNull_AdditionalFee() {
            CaseData caseData = CaseData.builder()
                    .generalAppPBADetails(GAPbaDetails.builder().fee(
                                    Fee.builder()
                                            .calculatedAmountInPence(BigDecimal.valueOf(30000))
                                            .code("OOOCM002").build())
                            .build())
                .hwfFeeType(FeeType.ADDITIONAL)
                .build();
            CallbackParams params = callbackParamsOf(caseData, CallbackType.ABOUT_TO_SUBMIT);
            //When
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            //Then
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);
            assertThat(updatedData.getAdditionalHwfDetails().getRemissionAmount()).isEqualTo(BigDecimal.valueOf(30000));
            assertThat(updatedData.getAdditionalHwfDetails().getHwfCaseEvent()).isEqualTo(FULL_REMISSION_HWF_GA);
        }

        @Test
        void shouldNotUpdateFullRemissionData_ifGaFeeIsZero() {
            CaseData caseData = CaseData.builder()
                    .generalAppPBADetails(GAPbaDetails.builder().fee(
                                    Fee.builder()
                                            .calculatedAmountInPence(BigDecimal.ZERO)
                                            .code("FREE").build())
                            .build())
                .gaHwfDetails(HelpWithFeesDetails.builder().build())
                .hwfFeeType(FeeType.APPLICATION)
                .build();
            CallbackParams params = callbackParamsOf(caseData, CallbackType.ABOUT_TO_SUBMIT);
            //When
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            //Then
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);
            assertThat(updatedData.getGaHwfDetails().getRemissionAmount()).isNull();
        }

        @Test
        void shouldNotUpdateFullRemissionData_ifAdditionalFeeIsZero() {
            CaseData caseData = CaseData.builder()
                .generalAppPBADetails(GAPbaDetails.builder().fee(
                                Fee.builder()
                                        .calculatedAmountInPence(BigDecimal.ZERO)
                                        .code("FREE").build())
                        .build())
                .additionalHwfDetails(HelpWithFeesDetails.builder().build())
                .hwfFeeType(FeeType.ADDITIONAL)
                .build();
            CallbackParams params = callbackParamsOf(caseData, CallbackType.ABOUT_TO_SUBMIT);
            //When
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            //Then
            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);
            assertThat(updatedData.getAdditionalHwfDetails().getRemissionAmount()).isNull();
        }

    }
}
