package uk.gov.hmcts.reform.civil.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.ServiceRequestUpdateDto;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;

import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_ADD_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.enums.PaymentStatus.SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRequestUpdateCallbackService {

    public static final String PAID = "Paid";
    public static final String ADDITIONAL_PAYMENT_SUCCESS_CALLBACK = "AdditionalPaymentSuccessCallback";
    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;
    private final JudicialNotificationService judicialNotificationService;
    private final ObjectMapper objectMapper;
    private final Time time;

    private CaseData data;

    public void processCallback(ServiceRequestUpdateDto serviceRequestUpdateDto) {
        log.info("Processing the callback for the caseId {} with status {}", serviceRequestUpdateDto.getCcdCaseNumber(),
                 serviceRequestUpdateDto.getServiceRequestStatus());

        if (serviceRequestUpdateDto.getServiceRequestStatus().equalsIgnoreCase(PAID)) {

            log.info("Fetching the Case details based on caseId {}", serviceRequestUpdateDto.getCcdCaseNumber());
            CaseDetails caseDetails = coreCaseDataService.getCase(Long.valueOf(serviceRequestUpdateDto
                                                                                   .getCcdCaseNumber()));
            CaseData caseData = caseDetailsConverter.toCaseData(caseDetails);

            if (!Objects.isNull(caseData)) {
                if (caseData.getCcdState().equals(APPLICATION_ADD_PAYMENT)) {

                    caseData = updateCaseDataWithStateAndPaymentDetails(serviceRequestUpdateDto, caseData);
                    judicialNotificationService.sendNotification(caseData);
                    createEvent(caseData, CaseEvent.ADDITIONAL_PAYMENT_SUCCESS_CALLBACK,
                                serviceRequestUpdateDto.getCcdCaseNumber()
                    );
                }
            }

        } else {
            log.error("Case id {} not present", serviceRequestUpdateDto.getCcdCaseNumber());
        }
    }

    private void createEvent(CaseData caseData, CaseEvent eventName, String generalApplicationCaseId) {

        StartEventResponse startEventResponse = coreCaseDataService.startGaUpdate(
            generalApplicationCaseId,
            eventName
        );
        CaseData startEventData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());
        BusinessProcess businessProcess = startEventData.getBusinessProcess()
            .updateActivityId(ADDITIONAL_PAYMENT_SUCCESS_CALLBACK);

        CaseDataContent caseDataContent = buildCaseDataContent(
            startEventResponse,
            caseData,
            businessProcess,
            generalApplicationCaseId,
            startEventData.getGeneralAppParentCaseLink()
        );
        data = coreCaseDataService.submitGaUpdate(generalApplicationCaseId, caseDataContent);
        coreCaseDataService.triggerEvent(caseData.getCcdCaseReference(), MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID);

    }

    private CaseData updateCaseDataWithStateAndPaymentDetails(ServiceRequestUpdateDto serviceRequestUpdateDto,
                                                              CaseData caseData) {

        GAPbaDetails pbaDetails = caseData.getGeneralAppPBADetails();
        String customerReference = ofNullable(serviceRequestUpdateDto.getPayment())
            .map(PaymentDto::getCustomerReference)
            .orElse(pbaDetails.getAdditionalPaymentServiceRef());

        PaymentDetails paymentDetails = ofNullable(pbaDetails.getAdditionalPaymentDetails())
            .map(PaymentDetails::toBuilder)
            .orElse(PaymentDetails.builder())
            .status(SUCCESS)
            .customerReference(customerReference)
            .reference(serviceRequestUpdateDto.getPayment().getPaymentReference())
            .errorCode(null)
            .errorMessage(null)
            .build();

        caseData = caseData.toBuilder()
            .ccdState(ORDER_MADE)
            .generalAppPBADetails(pbaDetails.toBuilder()
                                      .additionalPaymentDetails(paymentDetails)
                                      .paymentSuccessfulDate(time.now()).build()
            ).build();

        return caseData;
    }

    private CaseDataContent buildCaseDataContent(StartEventResponse startEventResponse, CaseData caseData,
                                                 BusinessProcess businessProcess, String caseId,
                                                 GeneralAppParentCaseLink generalAppParentCaseLink) {
        Map<String, Object> updatedData = caseData.toMap(objectMapper);
        updatedData.put("businessProcess", businessProcess);

        if (generalAppParentCaseLink == null
            || StringUtils.isBlank(generalAppParentCaseLink.getCaseReference())) {
            updatedData.put("generalAppParentCaseLink", GeneralAppParentCaseLink.builder()
                .caseReference(caseId).build());
        }

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder().id(startEventResponse.getEventId())
                       .summary(null)
                       .description(null)
                       .build())
            .data(updatedData)
            .build();
    }

}
