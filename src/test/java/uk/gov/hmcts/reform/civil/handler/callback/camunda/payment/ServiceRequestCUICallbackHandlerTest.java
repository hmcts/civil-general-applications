package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.PaymentsService;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CREATE_SERVICE_REQUEST_CUI_GENERAL_APP;

@ExtendWith(MockitoExtension.class)
class ServiceRequestCUICallbackHandlerTest extends BaseCallbackHandlerTest {

    private static final String SUCCESSFUL_PAYMENT_REFERENCE = "2022-1655915218557";

    @Mock
    private PaymentsService paymentsService;

    private ServiceRequestCUICallbackHandler handler;

    private ObjectMapper objectMapper;

    private CaseData caseData;
    private CallbackParams params;

    @BeforeEach
    public void setup() {
        caseData = CaseData.builder()
            .ccdCaseReference(1644495739087775L)
            .generalAppPBADetails(GAPbaDetails.builder()
                                      .fee(Fee.builder()
                                               .calculatedAmountInPence(BigDecimal.valueOf(100))
                                               .code("CODE").build()).build())
            .build();
        ;
    }

    @Nested
    class MakeServiceRequestPayments {

        @BeforeEach
        void setup() {
            objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules();
            handler = new ServiceRequestCUICallbackHandler(paymentsService, objectMapper);
            params = callbackParamsOf(caseData, CREATE_SERVICE_REQUEST_CUI_GENERAL_APP, ABOUT_TO_SUBMIT);
        }

        @Test
        void shouldMakePaymentServiceRequestForClaimFee_whenInvoked() {
            //GIVEN
            params = callbackParamsOf(caseData, CREATE_SERVICE_REQUEST_CUI_GENERAL_APP, ABOUT_TO_SUBMIT);
            when(paymentsService.createServiceRequest(any(), any()))
                .thenReturn(PaymentServiceResponse.builder()
                                .serviceRequestReference(SUCCESSFUL_PAYMENT_REFERENCE).build());
            //WHEN
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            //THEN
            verify(paymentsService).createServiceRequest(caseData, "BEARER_TOKEN");
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
            String serviceRequestReference = responseCaseData.getGeneralAppPBADetails().getServiceReqReference();
            assertThat(serviceRequestReference).isEqualTo(SUCCESSFUL_PAYMENT_REFERENCE);
        }

        @Test
        void shouldNotMakeAnyServiceRequest_whenServiceRequestHasBeenInvokedPreviously() {
            //GIVEN
            caseData = caseData.toBuilder()
                .generalAppPBADetails(GAPbaDetails.builder()
                                          .serviceReqReference(CaseDataBuilder.CUSTOMER_REFERENCE).build())
                .build();
            params = callbackParamsOf(caseData, CREATE_SERVICE_REQUEST_CUI_GENERAL_APP, ABOUT_TO_SUBMIT);
            //WHEN
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            //THEN
            verifyNoInteractions(paymentsService);
            CaseData responseCaseData = objectMapper.convertValue(response.getData(), CaseData.class);
            String serviceRequestReference = responseCaseData.getGeneralAppPBADetails().getServiceReqReference();
            assertThat(serviceRequestReference).isEqualTo(CaseDataBuilder.CUSTOMER_REFERENCE);
        }

        @Test
        void handleEventsReturnsTheExpectedCallbackEvent() {
            //THEN
            assertThat(handler.handledEvents()).contains(CREATE_SERVICE_REQUEST_CUI_GENERAL_APP);
        }

        @Test
        void shouldReturnCorrectActivityId_whenRequested() {
            //GIVEN
            CallbackParams params = params = callbackParamsOf(caseData,
                                                              CREATE_SERVICE_REQUEST_CUI_GENERAL_APP, ABOUT_TO_SUBMIT
            );
            //THEN
            assertThat(handler.camundaActivityId()).isEqualTo("CreateServiceRequestCUI");
        }

        @Test
        void shouldHandleException_whenServiceRequestFails() {
            //GIVEN
            params = params = callbackParamsOf(caseData, CREATE_SERVICE_REQUEST_CUI_GENERAL_APP, ABOUT_TO_SUBMIT);
            when(paymentsService.createServiceRequest(any(), any()))
                .thenThrow(FeignException.class);
            //WHEN
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            //THEN
            assertThat(response.getErrors()).isNotEmpty();
        }
    }
}
