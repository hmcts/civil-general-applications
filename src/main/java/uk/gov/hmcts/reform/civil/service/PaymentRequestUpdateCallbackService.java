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
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID;
import static uk.gov.hmcts.reform.civil.enums.PaymentStatus.SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRequestUpdateCallbackService {

    public static final String PAID = "Paid";

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;
    private final JudicialNotificationService judicialNotificationService;
    private final GeneralApplicationCreationNotificationService gaNotificationService;
    private final ObjectMapper objectMapper;
    private final Time time;
    private final StateGeneratorService stateGeneratorService;

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

                switch (caseData.getCcdState()) {
                    case APPLICATION_ADD_PAYMENT:
                        processAndTriggerAdditionalPayment(caseData, serviceRequestUpdateDto);
                        break;
                    case AWAITING_APPLICATION_PAYMENT:
                        processAndTriggerAwaitingPayment(caseData, serviceRequestUpdateDto);
                        break;
                    default:
                        log.error("This Case id {} is not in a valid state APPLICATION_ADD_PAYMENT,"
                                      + "AWAITING_APPLICATION_PAYMENT to process payment callback ",
                                  serviceRequestUpdateDto.getCcdCaseNumber());
                }
            } else {
                log.error("Case id {} not present", serviceRequestUpdateDto.getCcdCaseNumber());
            }
        }
    }

    private void processAndTriggerAwaitingPayment(CaseData caseData, ServiceRequestUpdateDto serviceRequestUpdateDto) {
        log.info("Processing the callback for Application Payment "
                     + "for the caseId {}", serviceRequestUpdateDto.getCcdCaseNumber());
        caseData = updateCaseDataWithPaymentDetails(serviceRequestUpdateDto, caseData);

        createEvent(caseData, INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT,
                    serviceRequestUpdateDto.getCcdCaseNumber());
    }

    private void processAndTriggerAdditionalPayment(CaseData caseData,
                                                    ServiceRequestUpdateDto serviceRequestUpdateDto) {
        log.info("Processing the callback for making Additional Payment"
                     + "for the caseId {}", serviceRequestUpdateDto.getCcdCaseNumber());
        try {
            judicialNotificationService.sendNotification(caseData, "respondent");

            caseData = updateCaseDataWithStateAndPaymentDetails(serviceRequestUpdateDto, caseData);

            createEvent(caseData, MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID,
                        serviceRequestUpdateDto.getCcdCaseNumber());
            coreCaseDataService.triggerEvent(caseData.getCcdCaseReference(),
                                             MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID);

        } catch (NotificationException e) {
            log.info("processing callback failed at Judicial Notification service, "
                         + "please update the caseData with ga status "
                         + "along with the Additional payment details "
                         + "and trigger MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID event  %s ", e);
        }
    }

    private CaseData updateCaseDataWithPaymentDetails(ServiceRequestUpdateDto serviceRequestUpdateDto,
                                                      CaseData caseData) {
        GAPbaDetails pbaDetails = caseData.getGeneralAppPBADetails();
        String paymentReference = ofNullable(serviceRequestUpdateDto.getPayment())
            .map(PaymentDto::getCustomerReference)
            .orElse(pbaDetails.getServiceReqReference());

        PaymentDetails paymentDetails = ofNullable(pbaDetails.getPaymentDetails())
            .map(PaymentDetails::toBuilder)
            .orElse(PaymentDetails.builder())
            .status(SUCCESS)
            .customerReference(pbaDetails.getServiceReqReference())
            .reference(paymentReference)
            .errorCode(null)
            .errorMessage(null)
            .build();

        caseData = caseData.toBuilder()
            .generalAppPBADetails(pbaDetails.toBuilder()
                                      .paymentDetails(paymentDetails)
                                      .paymentSuccessfulDate(time.now()).build())
            .build();

        return caseData;
    }

    private CaseData updateCaseDataWithStateAndPaymentDetails(ServiceRequestUpdateDto serviceRequestUpdateDto,
                                                              CaseData caseData) {
        GAPbaDetails pbaDetails = caseData.getGeneralAppPBADetails();
        String paymentReference = ofNullable(serviceRequestUpdateDto.getPayment())
            .map(PaymentDto::getCustomerReference)
            .orElse(pbaDetails.getServiceReqReference());

        PaymentDetails paymentDetails = ofNullable(pbaDetails.getPaymentDetails())
            .map(PaymentDetails::toBuilder)
            .orElse(PaymentDetails.builder())
            .status(SUCCESS)
            .customerReference(pbaDetails.getServiceReqReference())
            .reference(paymentReference)
            .errorCode(null)
            .errorMessage(null)
            .build();

        caseData = caseData.toBuilder()
            .generalAppPBADetails(pbaDetails.toBuilder()
                                      .paymentDetails(paymentDetails)
                                      .paymentSuccessfulDate(time.now()).build())
            .build();

        return caseData;
    }

    private void createEvent(CaseData caseData, CaseEvent eventName, String generalApplicationCaseId) {
        StartEventResponse startEventResponse = coreCaseDataService.startGaUpdate(
            generalApplicationCaseId,
            eventName
        );
        CaseData startEventData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());

        BusinessProcess businessProcess = startEventData.getBusinessProcess();
        CaseDataContent caseDataContent = buildCaseDataContent(
            startEventResponse,
            caseData,
            businessProcess,
            generalApplicationCaseId,
            startEventData.getGeneralAppParentCaseLink()
        );
        coreCaseDataService.submitGaUpdate(generalApplicationCaseId, caseDataContent);
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
