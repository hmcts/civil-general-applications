package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.CasePaymentRequestDto;
import uk.gov.hmcts.reform.civil.model.CreateServiceRequest;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.PaymentServiceResponse;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    PaymentServiceClient.class,
    JacksonAutoConfiguration.class
})
public class PaymentServiceClientTest {

    private CaseData caseData;

    @MockBean
    private GeneralAppFeesService feesService;

    @MockBean
    private PaymentServiceResponse paymentServiceResponse;

    @MockBean
    private CreateServiceRequest paymentServiceRequest;

    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private PaymentStoreApi paymentApi;

    @InjectMocks
    PaymentServiceClient paymentServiceClient;

    public static final String callBackUrl = "http://test";
    private static final String AUTH_TOKEN = "Bearer token";
    private final String serviceAuthToken = "Bearer testServiceAuth";
    public static final String PAYMENT_ACTION = "payment";
    public static final String RESPONSIBLE_PARTY = "1233";

    @BeforeEach
    void setUp() {
        caseData = CaseDataBuilder.builder().buildMakePaymentsCaseData();

        paymentServiceRequest = CreateServiceRequest.builder()
            .callBackUrl(callBackUrl)
            .casePaymentRequest(CasePaymentRequestDto.builder()
                                    .action(PAYMENT_ACTION)
                                    .responsibleParty("123").build())
            .caseReference("123")
            .ccdCaseNumber("123")
            .fees(new FeeDto[]{
                FeeDto.builder()
                    .calculatedAmount(caseData.getGeneralAppPBADetails().getFee().toFeeDto().getCalculatedAmount())
                    .code(caseData.getGeneralAppPBADetails().getFee().toFeeDto().getCode())
                    .version(caseData.getGeneralAppPBADetails().getFee().toFeeDto().getVersion())
                    .volume(1).build()
            })
            .build();
    }

    @Test
    public void shouldReturnPaymentServiceResponseWithReferenceResponse() throws Exception {

        when(feesService.getFeeForGA(any()))
            .thenReturn(Fee.builder().calculatedAmountInPence(
                BigDecimal.valueOf(10800)).code("FEE0443").version("1").build());
        when(authTokenGenerator.generate()).thenReturn(serviceAuthToken);

        paymentServiceResponse = PaymentServiceResponse.builder().serviceRequestReference("response").build();

        when(paymentApi.createServiceRequest(AUTH_TOKEN, serviceAuthToken, paymentServiceRequest))
            .thenReturn(paymentServiceResponse);

        PaymentServiceResponse serviceResponse = paymentServiceClient.createServiceRequest(
                                                    AUTH_TOKEN, paymentServiceRequest);

        assertNotNull(serviceResponse);
        assertEquals("response", serviceResponse.getServiceRequestReference());

    }

    @Test
    public void shouldReturnPaymentServiceResponseWithNullReference() throws Exception {

        when(authTokenGenerator.generate()).thenReturn(serviceAuthToken);
        when(paymentApi.createServiceRequest("", serviceAuthToken, paymentServiceRequest)).thenReturn(
            paymentServiceResponse);

        PaymentServiceResponse serviceResponse = paymentServiceClient.createServiceRequest("", paymentServiceRequest);
        assertNull(serviceResponse.getServiceRequestReference());

    }

}
