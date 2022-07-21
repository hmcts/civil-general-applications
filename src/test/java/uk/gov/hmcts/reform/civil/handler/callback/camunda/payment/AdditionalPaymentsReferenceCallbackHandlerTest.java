package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;


import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
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

import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.PaymentServiceResponse;
import uk.gov.hmcts.reform.civil.service.PaymentsService;
import uk.gov.hmcts.reform.civil.service.Time;
import uk.gov.hmcts.reform.payments.client.InvalidPaymentRequestException;

import java.time.LocalDateTime;
import java.util.Map;

import static feign.Request.HttpMethod.GET;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.START_MAKE_ADDITIONAL_PAYMENT_REF;


@SpringBootTest(classes = {
    AdditionalPaymentsReferenceCallbackHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class AdditionalPaymentsReferenceCallbackHandlerTest  extends BaseCallbackHandlerTest {
    private static final String PAYMENT_REQUEST_REFERENCE = "RC-1234-1234-1234-1234";


    @MockBean
    private Time time;

    @Autowired
    private AdditionalPaymentsReferenceCallbackHandler handler;

    @Autowired
    private ObjectMapper objectMapper;

    private CallbackParams params;
    @MockBean
    private PaymentsService paymentsService;

    @BeforeEach
    public void setup() {
        when(time.now()).thenReturn(LocalDateTime.of(2020, 1, 1, 12, 0, 0));
    }
    @Nested
    class MakeAdditionalPaymentReference {

        @BeforeEach
        void setup() {

            when(paymentsService.createServiceRequestAdditionalPayment(any(), any()))
                .thenReturn(PaymentServiceResponse.builder().serviceRequestReference(PAYMENT_REQUEST_REFERENCE)
                                .build());
        }

        @Test
        void shouldMakeAdditionalPaymentReference_whenJudgeUncloakedApplication() throws Exception {
            var caseData=CaseDataBuilder.builder().requestForInformationApplicationWithOutNoticeToWithNotice()
                .build();
            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(paymentsService).createServiceRequestAdditionalPayment(caseData,"BEARER_TOKEN" );
            assertThat(extractPaymentRequestReferenceFromResponse(response))
                .isEqualTo(PAYMENT_REQUEST_REFERENCE);
        }
        @Test
        void shouldNotMakeAdditionalPaymentReference_whenJudgeNotUncloakedApplication() throws Exception {
            var caseData=CaseDataBuilder.builder().requestForInforationApplication()
                .build();
            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(extractPaymentRequestReferenceFromResponse(response))
                .isEqualTo(null);
        }
        @Test
        void shouldThrowException_whenForbiddenExceptionThrownContainsInvalidResponse() {
            doThrow(buildForbiddenFeignExceptionWithInvalidResponse())
                .when(paymentsService).createServiceRequestAdditionalPayment(any(), any());
            var caseData=CaseDataBuilder.builder().requestForInformationApplicationWithOutNoticeToWithNotice()
                .build();
            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            assertThrows(FeignException.class, () -> handler.handle(params));
            verify(paymentsService).createServiceRequestAdditionalPayment(caseData, "BEARER_TOKEN");
        }
        @Test
        void shouldNotThrowError_whenPaymentIsResubmittedWithInTwoMinutes() {
            doThrow(new InvalidPaymentRequestException("Duplicate Payment."))
                .when(paymentsService).createServiceRequestAdditionalPayment(any(), any());

            var caseData=CaseDataBuilder.builder().requestForInformationApplicationWithOutNoticeToWithNotice()
                .build();
            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(paymentsService).createServiceRequestAdditionalPayment(caseData, "BEARER_TOKEN");

            assertThat(extractPaymentRequestReferenceFromResponse(response)).isNull();
            assertThat(response.getErrors()).isEmpty();
        }
        @Test
        void shouldReturnCorrectActivityId_whenRequested() {
            var caseData=CaseDataBuilder.builder().requestForInformationApplicationWithOutNoticeToWithNotice()
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            assertThat(handler.camundaActivityId(params)).isEqualTo("GeneralApplicationMakeAdditionalPayment");
        }

        @Test
        void handleEventsReturnsTheExpectedCallbackEvent() {
            assertThat(handler.handledEvents()).contains(START_MAKE_ADDITIONAL_PAYMENT_REF);
        }

    }

    private String extractPaymentRequestReferenceFromResponse(AboutToStartOrSubmitCallbackResponse response) {
        CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
        return responseCaseData.getGeneralAppPBADetails().getPaymentServiceRequestReferenceNumber();
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
