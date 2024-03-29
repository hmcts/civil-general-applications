package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.GeneralAppFeesService;
import uk.gov.hmcts.reform.civil.service.PaymentsService;
import uk.gov.hmcts.reform.civil.service.Time;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAKE_PAYMENT_SERVICE_REQ_GASPEC;
import static uk.gov.hmcts.reform.civil.enums.PaymentStatus.SUCCESS;

@SpringBootTest(classes = {
    PaymentServiceRequestHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class CreateServiceRequestHandlerTest extends BaseCallbackHandlerTest {

    private static final String SUCCESSFUL_PAYMENT_REFERENCE = "2022-1655915218557";
    private static final String FREE_PAYMENT_REFERENCE = "FREE";

    @MockBean
    private PaymentsService paymentsService;

    @MockBean
    private PaymentServiceResponse paymentServiceResponse;

    @MockBean
    private Time time;

    @Autowired
    private PaymentServiceRequestHandler handler;

    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private GeneralAppFeesService generalAppFeesService;
    private CaseData caseData;
    private CallbackParams params;

    @BeforeEach
    public void setup() {
        caseData = CaseDataBuilder.builder().buildMakePaymentsCaseData();

        when(time.now()).thenReturn(LocalDateTime.of(2020, 1, 1, 12, 0, 0));
    }

    @Nested
    class MakeServiceRequestPayments {

        @BeforeEach
        void setup() {
            params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        }

        @Test
        void shouldMakePaymentServiceRequest_whenInvoked() throws Exception {
            when(paymentsService.createServiceRequest(any(), any()))
                .thenReturn(paymentServiceResponse.builder()
                                .serviceRequestReference(SUCCESSFUL_PAYMENT_REFERENCE).build());
            when(generalAppFeesService.isFreeApplication(any())).thenReturn(false);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(paymentsService).createServiceRequest(caseData, "BEARER_TOKEN");
            assertThat(extractPaymentDetailsFromResponse(response).getServiceReqReference())
                .isEqualTo(SUCCESSFUL_PAYMENT_REFERENCE);
        }

        @Test
        void shouldNotMakePaymentServiceRequest_shouldAddFreePaymentDetails_whenInvoked() throws Exception {
            when(paymentsService.createServiceRequest(any(), any()))
                    .thenReturn(paymentServiceResponse.builder()
                            .serviceRequestReference(FREE_PAYMENT_REFERENCE).build());
            when(generalAppFeesService.isFreeApplication(any())).thenReturn(true);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(paymentsService, never()).createServiceRequest(caseData, "BEARER_TOKEN");
            assertThat(extractPaymentDetailsFromResponse(response).getServiceReqReference())
                    .isEqualTo(FREE_PAYMENT_REFERENCE);
            PaymentDetails paymentDetails = extractPaymentDetailsFromResponse(response).getPaymentDetails();
            assertThat(paymentDetails).isNotNull();
            assertThat(paymentDetails.getStatus()).isEqualTo(SUCCESS);
            assertThat(paymentDetails.getCustomerReference()).isEqualTo(FREE_PAYMENT_REFERENCE);
            assertThat(paymentDetails.getReference()).isEqualTo(FREE_PAYMENT_REFERENCE);
            assertThat(extractPaymentDetailsFromResponse(response).getPaymentSuccessfulDate())
                    .isNotNull();
        }

        @Test
        void shouldReturnCorrectActivityId_whenRequested() {
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            assertThat(handler.camundaActivityId()).isEqualTo("GeneralApplicationPaymentServiceReq");
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

}
