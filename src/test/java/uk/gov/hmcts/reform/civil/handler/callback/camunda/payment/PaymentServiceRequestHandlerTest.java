package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import uk.gov.hmcts.reform.civil.service.PaymentServiceResponse;
import uk.gov.hmcts.reform.civil.service.PaymentsService;
import uk.gov.hmcts.reform.civil.service.Time;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;
import uk.gov.hmcts.reform.payments.client.models.StatusHistoryDto;

import java.time.LocalDateTime;
import java.util.Map;

import static feign.Request.HttpMethod.GET;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAKE_PAYMENT_SERVICE_REQ_GASPEC;

@SpringBootTest(classes = {
    PaymentServiceRequestHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class PaymentServiceRequestHandlerTest extends BaseCallbackHandlerTest {

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
    private PaymentServiceRequestHandler handler;

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
        void shouldMakePaymentServiceRequest_whenInvoked() throws Exception {
            when(paymentsService.createPaymentServiceReq(any(), any()))
                .thenReturn(PaymentServiceResponse.builder()
                                .serviceRequestReference(SUCCESSFUL_PAYMENT_REFERENCE).build());

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(paymentsService).createPaymentServiceReq(caseData, "BEARER_TOKEN");
            assertThat(extractPaymentDetailsFromResponse(response).getServiceReqReference())
                .isEqualTo(SUCCESSFUL_PAYMENT_REFERENCE);
        }

        @Test
        void shouldReturnCorrectActivityId_whenRequested() {
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            assertThat(handler.camundaActivityId(params)).isEqualTo("GeneralApplicationMakePayment");
        }

        @Test
        void handleEventsReturnsTheExpectedCallbackEvent() {
            assertThat(handler.handledEvents()).contains(MAKE_PAYMENT_SERVICE_REQ_GASPEC);
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

    private FeignException.FeignClientException buildFeignClientException(int status, byte[] body) {
        return new FeignException.FeignClientException(
            status,
            "exception message",
            Request.create(GET, "", Map.of(), new byte[]{}, UTF_8, null),
            body
        );
    }
}
