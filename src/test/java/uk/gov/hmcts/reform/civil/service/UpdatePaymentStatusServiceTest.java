package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.PaymentStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CardPaymentStatusResponse;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CITIZEN_GENERAL_APP_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.CASE_PROGRESSION;

@ExtendWith(MockitoExtension.class)
class UpdatePaymentStatusServiceTest {

    @Mock
    CaseDetailsConverter caseDetailsConverter;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    private CoreCaseDataService coreCaseDataService;
    @InjectMocks
    UpdatePaymentStatusService updatePaymentStatusService;

    public static final String BUSINESS_PROCESS = "JUDICIAL_REFERRAL";
    private static final Long CASE_ID = 1594901956117591L;
    public static final String TOKEN = "1234";

    @Test
    public void shouldSubmitCitizenApplicationFeePaymentEvent() {

        CaseData caseData = CaseData.builder()
            .ccdState(CASE_PROGRESSION)
            .businessProcess(BusinessProcess.builder()
                                 .status(BusinessProcessStatus.READY)
                                 .camundaEvent(BUSINESS_PROCESS)
                                 .build())
            .generalAppPBADetails(GAPbaDetails.builder()
                                      .paymentDetails(PaymentDetails.builder()
                                                          .customerReference("RC-1604-0739-2145-4711")
                                                          .build())
                                      .build())
            .build();
        CaseDetails caseDetails = buildCaseDetails(caseData);

        when(coreCaseDataService.getCase(CASE_ID)).thenReturn(caseDetails);
        when(caseDetailsConverter.toCaseData(caseDetails)).thenReturn(caseData);
        when(coreCaseDataService.startUpdate(any(), any())).thenReturn(startEventResponse(
            caseDetails,
            CITIZEN_GENERAL_APP_PAYMENT
        ));
        when(coreCaseDataService.submitUpdate(any(), any())).thenReturn(caseData);

        updatePaymentStatusService.updatePaymentStatus(String.valueOf(CASE_ID), getCardPaymentStatusResponse());

        verify(coreCaseDataService, times(1)).getCase(Long.valueOf(CASE_ID));
        verify(coreCaseDataService).startUpdate(String.valueOf(CASE_ID), CITIZEN_GENERAL_APP_PAYMENT);
        verify(coreCaseDataService).submitUpdate(any(), any());

    }

    private CaseDetails buildCaseDetails(CaseData caseData) {
        return CaseDetails.builder()
            .data(objectMapper.convertValue(
                caseData,
                new TypeReference<Map<String, Object>>() {
                }
            ))
            .id(Long.valueOf(CASE_ID)).build();
    }

    private StartEventResponse startEventResponse(CaseDetails caseDetails, CaseEvent event) {
        return StartEventResponse.builder()
            .token(TOKEN)
            .eventId(event.name())
            .caseDetails(caseDetails)
            .build();
    }

    private CardPaymentStatusResponse getCardPaymentStatusResponse() {
        return CardPaymentStatusResponse.builder().paymentReference("1234").status(PaymentStatus.SUCCESS.name()).build();
    }
}
