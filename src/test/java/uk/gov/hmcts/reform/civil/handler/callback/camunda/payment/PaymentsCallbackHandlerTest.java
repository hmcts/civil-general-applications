package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.PaymentsService;
import uk.gov.hmcts.reform.civil.service.Time;
import uk.gov.hmcts.reform.payments.client.InvalidPaymentRequestException;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;
import uk.gov.hmcts.reform.payments.client.models.StatusHistoryDto;
import uk.gov.hmcts.reform.payments.response.PBAServiceRequestResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static feign.Request.HttpMethod.GET;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAKE_PBA_PAYMENT_GASPEC;
import static uk.gov.hmcts.reform.civil.enums.PaymentStatus.FAILED;
import static uk.gov.hmcts.reform.civil.enums.PaymentStatus.SUCCESS;

@SpringBootTest(classes = {
    PaymentsCallbackHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class PaymentsCallbackHandlerTest extends BaseCallbackHandlerTest {

    private static final String SUCCESSFUL_PAYMENT_REFERENCE = "RC-1234-1234-1234-1234";
    private static final String PAYMENT_ERROR_MESSAGE = "Your account is deleted";
    private static final String PAYMENT_ERROR_CODE = "CA-E0004";
    public static final String DUPLICATE_PAYMENT_MESSAGE
        = "You attempted to retry the payment to soon. Try again later.";

    @MockBean
    private PaymentsService paymentsService;

    @MockBean
    private Time time;

    @Autowired
    private PaymentsCallbackHandler handler;

    @Autowired
    private ObjectMapper objectMapper;

    private CaseData caseData;
    private CallbackParams params;

    @BeforeEach
    public void setup() {
        caseData = CaseDataBuilder.builder().buildMakePaymentsCaseData();

        when(time.now()).thenReturn(LocalDateTime.of(2020, 1, 1, 12, 0, 0));
    }

    @Nested
    class MakePBAPayments {

        @BeforeEach
        void setup() {
            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        }

        @Test
        void shouldMakePbaPayment_whenInvoked() throws Exception {
            when(paymentsService.createCreditAccountPayment(any(), any()))
                .thenReturn(PBAServiceRequestResponse.builder().paymentReference(SUCCESSFUL_PAYMENT_REFERENCE).build());

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(paymentsService).createCreditAccountPayment(caseData, "BEARER_TOKEN");
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails().getReference())
                    .isEqualTo(SUCCESSFUL_PAYMENT_REFERENCE);
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails().getStatus())
                    .isEqualTo(SUCCESS);
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails().getCustomerReference())
                    .isEqualTo("12345");
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentSuccessfulDate())
                    .isEqualTo(LocalDateTime.of(LocalDate.of(2020, 01, 01),
                            LocalTime.of(12, 00, 00)));
        }

        @ParameterizedTest
        @ValueSource(ints = {403, 422, 504})
        void shouldUpdateFailureReason_whenForbiddenExceptionThrown(int status) {
            doThrow(buildFeignException(status)).when(paymentsService).createCreditAccountPayment(any(), any());

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(paymentsService).createCreditAccountPayment(caseData, "BEARER_TOKEN");
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails().getReference()).isNull();
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails().getErrorMessage())
                    .isEqualTo(PAYMENT_ERROR_MESSAGE);
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails().getErrorCode())
                    .isEqualTo(PAYMENT_ERROR_CODE);
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails().getStatus())
                    .isEqualTo(FAILED);
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails().getCustomerReference())
                    .isEqualTo("12345");
            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldNotThrowError_whenPaymentIsResubmittedWithInTwoMinutes() {
            doThrow(new InvalidPaymentRequestException("Duplicate Payment."))
                .when(paymentsService).createCreditAccountPayment(any(), any());

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(paymentsService).createCreditAccountPayment(caseData, "BEARER_TOKEN");

            assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails().getReference()).isNull();
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails().getErrorMessage())
                    .isEqualTo(DUPLICATE_PAYMENT_MESSAGE);
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails().getErrorCode())
                    .isNull();
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails().getStatus())
                    .isEqualTo(FAILED);
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails().getCustomerReference())
                    .isEqualTo("12345");
            assertThat(response.getErrors()).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(ints = {404})
        void shouldAddError_whenOtherExceptionThrown(int status) {
            doThrow(buildFeignException(status)).when(paymentsService).createCreditAccountPayment(any(), any());

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(paymentsService).createCreditAccountPayment(caseData, "BEARER_TOKEN");

            assertThat(extractPaymentDetailsFromResponse(response).getServiceReqReference()).isEqualTo("12345");
         //   assertThat(extractPaymentDetailsFromResponse(response).getPaymentDetails()).isNotNull();
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentSuccessfulDate()).isNull();
            assertThat(response.getErrors()).containsOnly("Technical error occurred");
        }

        @Test
        void shouldThrowException_whenForbiddenExceptionThrownContainsInvalidResponse() {
            doThrow(buildForbiddenFeignExceptionWithInvalidResponse())
                .when(paymentsService).createCreditAccountPayment(any(), any());

            assertThrows(FeignException.class, () -> handler.handle(params));
            verify(paymentsService).createCreditAccountPayment(caseData, "BEARER_TOKEN");
        }

        @Test
        void shouldReturnCorrectActivityId_whenRequested() {
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            assertThat(handler.camundaActivityId(params)).isEqualTo("GeneralAppServiceReqPbaPayment");
        }

        @Test
        void handleEventsReturnsTheExpectedCallbackEvent() {
            assertThat(handler.handledEvents()).contains(MAKE_PBA_PAYMENT_GASPEC);
        }
    }

    private GAPbaDetails extractPaymentDetailsFromResponse(AboutToStartOrSubmitCallbackResponse response) {
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        return responseCaseData.getGeneralAppPBADetails();
    }

    @SneakyThrows
    private FeignException buildFeignException(int status) {
        return buildFeignClientException(status, objectMapper.writeValueAsBytes(
            PaymentDto.builder()
                .statusHistories(new StatusHistoryDto[]{
                    StatusHistoryDto.builder()
                        .errorCode(PAYMENT_ERROR_CODE)
                        .errorMessage(PAYMENT_ERROR_MESSAGE)
                        .build()
                })
                .build()
        ));
    }

    private FeignException buildForbiddenFeignExceptionWithInvalidResponse() {
        return buildFeignClientException(403, "unexpected response body".getBytes(UTF_8));
    }

    private FeignException.FeignClientException buildFeignClientException(int status, byte[] body) {
        return new FeignException.FeignClientException(
            status,
            "exception message",
            Request.create(GET, "", Map.of(), new byte[]{}, UTF_8, null),
            body
        );
    }
}
