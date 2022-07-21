package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.civil.config.PaymentsConfiguration;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.payments.client.InvalidPaymentRequestException;
import uk.gov.hmcts.reform.payments.client.models.FeeDto;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;
import uk.gov.hmcts.reform.prd.model.ContactInformation;
import uk.gov.hmcts.reform.prd.model.Organisation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    PaymentsService.class,
    JacksonAutoConfiguration.class
})
class PaymentsServiceTest {

    private static final String SERVICE = "service";
    private static final String SITE_ID = "site_id";
    private static final String AUTH_TOKEN = "Bearer token";
    private static final PaymentDto PAYMENT_DTO = PaymentDto.builder()
                                        .reference("RC-1234-1234-1234-1234").build();
    private static final PaymentServiceResponse PAYMENT_SERVICE_RESPONSE = PaymentServiceResponse.builder()
                                                            .serviceRequestReference("RC-1234-1234-1234-1234").build();
    public static final String PAYMENT_ACTION = "payment";
    private static final Organisation ORGANISATION = Organisation.builder()
        .name("test org")
        .contactInformation(List.of(ContactInformation.builder().build()))
        .build();
    private static final String CUSTOMER_REFERENCE = "12345";
    private static final String FEE_NOT_SET_CORRECTLY_ERROR = "Fees are not set correctly.";
    @Value("${payments.api.callback-url}")
    String callBackUrl;
    @MockBean
    private PaymentServiceClient paymentsClient;

    @MockBean
    private PaymentsConfiguration paymentsConfiguration;

    @MockBean
    private OrganisationService organisationService;

    @Autowired
    private PaymentsService paymentsService;

    @BeforeEach
    void setUp() {
        given(paymentsClient.createPbaPayment(any(), any(), any())).willReturn(PAYMENT_DTO);
        given(paymentsConfiguration.getService()).willReturn(SERVICE);
        given(paymentsConfiguration.getSiteId()).willReturn(SITE_ID);
        given(organisationService.findOrganisationById(any())).willReturn(Optional.of(ORGANISATION));
    }

    @Test
    void validateRequestShouldNotThrowAnError_whenValidCaseDataIsProvided() {
        CaseData caseData = CaseDataBuilder.builder().buildMakePaymentsCaseData();
        paymentsService.validateRequest(caseData);
        assertThat(caseData).isNotNull();
    }

    @Test
    void validateRequestShouldThrowAnError_whenPBADetailsNotProvided() {
        CaseData caseData = CaseData.builder()
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().organisationIdentifier("OrgId").build())
            .build();

        Exception exception = assertThrows(
            InvalidPaymentRequestException.class,
            () -> paymentsService.validateRequest(caseData)
        );
        assertThat(exception.getMessage()).isEqualTo("PBA details not received.");
    }

    @Test
    void validateRequestShouldThrowAnError_whenFeeDetailsNotProvided() {
        CaseData caseData = CaseData.builder()
            .generalAppPBADetails(GAPbaDetails.builder().build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().organisationIdentifier("OrgId").build())
            .build();

        Exception exception = assertThrows(
            InvalidPaymentRequestException.class,
            () -> paymentsService.validateRequest(caseData)
        );
        assertThat(exception.getMessage()).isEqualTo(FEE_NOT_SET_CORRECTLY_ERROR);
    }

    @Test
    void validateRequestShouldThrowAnError_whenFeeDetailsDoNotHaveFeeCode() {
        CaseData caseData = CaseData.builder()
            .generalAppPBADetails(GAPbaDetails.builder()
                                      .fee(Fee.builder()
                                               .calculatedAmountInPence(BigDecimal.valueOf(10800))
                                               .version("1")
                                               .build())
                                      .build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().organisationIdentifier("OrgId").build())
            .build();

        Exception exception = assertThrows(
            InvalidPaymentRequestException.class,
            () -> paymentsService.validateRequest(caseData)
        );
        assertThat(exception.getMessage()).isEqualTo(FEE_NOT_SET_CORRECTLY_ERROR);
    }

    @Test
    void validateRequestShouldThrowAnError_whenFeeDetailsDoNotHaveFeeVersion() {
        CaseData caseData = CaseData.builder()
            .generalAppPBADetails(GAPbaDetails.builder()
                                      .fee(Fee.builder()
                                               .calculatedAmountInPence(BigDecimal.valueOf(10800))
                                               .code("FEE0442")
                                               .build())
                                      .build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().organisationIdentifier("OrgId").build())
            .build();

        Exception exception = assertThrows(
            InvalidPaymentRequestException.class,
            () -> paymentsService.validateRequest(caseData)
        );
        assertThat(exception.getMessage()).isEqualTo(FEE_NOT_SET_CORRECTLY_ERROR);
    }

    @Test
    void validateRequestShouldThrowAnError_whenFeeDetailsDoNotHaveFeeAmount() {
        CaseData caseData = CaseData.builder()
            .generalAppPBADetails(GAPbaDetails.builder()
                                      .fee(Fee.builder()
                                               .code("FEE0442")
                                               .version("1")
                                               .build())
                                      .build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().organisationIdentifier("OrgId").build())
            .build();

        Exception exception = assertThrows(
            InvalidPaymentRequestException.class,
            () -> paymentsService.validateRequest(caseData)
        );
        assertThat(exception.getMessage()).isEqualTo(FEE_NOT_SET_CORRECTLY_ERROR);
    }

    @Test
    void validateRequestShouldThrowAnError_whenApplicantSolicitorDetailsAreNotSet() {
        CaseData caseData = CaseData.builder()
            .generalAppPBADetails(GAPbaDetails.builder().fee(Fee.builder().build()).build())
            .build();

        Exception exception = assertThrows(
            InvalidPaymentRequestException.class,
            () -> paymentsService.validateRequest(caseData)
        );
        assertThat(exception.getMessage()).isEqualTo("Applicant's organization details not received.");
    }

    @Test
    void validateRequestShouldThrowAnError_whenApplicantSolicitorOrgDetailsAreNotSet() {
        CaseData caseData = CaseData.builder()
            .generalAppPBADetails(GAPbaDetails.builder().build())
            .generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().build())
            .build();

        Exception exception = assertThrows(
            InvalidPaymentRequestException.class,
            () -> paymentsService.validateRequest(caseData)
        );
        assertThat(exception.getMessage()).isEqualTo("Applicant's organization details not received.");
    }

    @Test
    void shouldCreateCreditAccountPayment_whenValidCaseDetails() {
        CaseData caseData = CaseDataBuilder.builder().buildMakePaymentsCaseData();

        var expectedCreditAccountPaymentRequest = getExpectedCreditAccountPaymentRequest(caseData);

        PaymentDto paymentResponse = paymentsService.createCreditAccountPayment(caseData, AUTH_TOKEN);

        verify(organisationService).findOrganisationById("OrgId");
        verify(paymentsClient).createPbaPayment(CUSTOMER_REFERENCE, AUTH_TOKEN, expectedCreditAccountPaymentRequest);
        assertThat(paymentResponse).isEqualTo(PAYMENT_DTO);
    }

    private ServiceRequestPaymentDto getExpectedCreditAccountPaymentRequest(CaseData caseData) {
        return ServiceRequestPaymentDto.builder()
            .accountNumber("PBA0078095")
            .amount(caseData.getGeneralAppPBADetails().getFee().toFeeDto().getCalculatedAmount())
            .customerReference(CUSTOMER_REFERENCE)
            .organisationName(ORGANISATION.getName())
            .build();
    }

    @Test
    void shouldCreateAdditionalPaymentServiceRequest_whenValidCaseDetails() {
        CaseData caseData = CaseDataBuilder.builder().buildMakePaymentsCaseData();
        given(paymentsClient.createServiceRequest(any(), any())).willReturn(PAYMENT_SERVICE_RESPONSE);
        var expectedCreditAccountPaymentRequest = buildExpectedServiceRequestAdditionalPaymentResponse(caseData);

        PaymentServiceResponse paymentResponse = paymentsService.createServiceRequestAdditionalPayment(caseData,
                                                                                                       AUTH_TOKEN);

        verify(paymentsClient).createServiceRequest(AUTH_TOKEN, expectedCreditAccountPaymentRequest);
        assertThat(paymentResponse).isEqualTo(PAYMENT_SERVICE_RESPONSE);
    }

    private PaymentServiceRequest buildExpectedServiceRequestAdditionalPaymentResponse(CaseData caseData) {
        return PaymentServiceRequest.builder()
            .callBackUrl(callBackUrl)
            .casePaymentRequest(CasePaymentRequestDto.builder()
                                    .action(PAYMENT_ACTION)
                                    .responsibleParty(caseData.getApplicantPartyName()).build())
            .caseReference(caseData.getLegacyCaseReference())
            .ccdCaseNumber(caseData.getCcdCaseReference().toString())
            .fees(new FeeDto[] { (FeeDto.builder()
                .calculatedAmount(BigDecimal.valueOf(165.00))
                .code("FEE0306")
                .version("1")
                .volume(1).build())
            }).build();
    }
}
