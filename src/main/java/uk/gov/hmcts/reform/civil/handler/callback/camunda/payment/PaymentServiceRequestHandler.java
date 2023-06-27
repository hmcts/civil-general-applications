package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.service.GeneralAppFeesService;
import uk.gov.hmcts.reform.civil.service.PaymentsService;
import uk.gov.hmcts.reform.civil.service.Time;
import uk.gov.hmcts.reform.civil.service.docmosis.applicationdraft.GeneralApplicationDraftGenerator;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAKE_PAYMENT_SERVICE_REQ_GASPEC;
import static uk.gov.hmcts.reform.civil.enums.PaymentStatus.SUCCESS;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceRequestHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(MAKE_PAYMENT_SERVICE_REQ_GASPEC);
    private static final String ERROR_MESSAGE = "Technical error occurred";
    private static final String TASK_ID = "GeneralApplicationPaymentServiceReq";

    private final PaymentsService paymentsService;
    private final GeneralAppFeesService feeService;
    private final ObjectMapper objectMapper;
    private final Time time;

    private final GeneralApplicationDraftGenerator gaDraftGenerator;
    private final AssignCategoryId assignCategoryId;

    @Override
    public String camundaActivityId() {
        return TASK_ID;
    }

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::makePaymentServiceReq
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse makePaymentServiceReq(CallbackParams callbackParams) {
        var caseData = callbackParams.getCaseData();
        var authToken = callbackParams.getParams().get(BEARER_TOKEN).toString();
        List<String> errors = new ArrayList<>();

        CaseDocument gaDraftDocument;
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        try {
            log.info("calling payment service request " + caseData.getCcdCaseReference());
            String serviceRequestReference = GeneralAppFeesService.FREE_REF;
            boolean freeGa = feeService.isFreeApplication(caseData);
            if (!freeGa) {
                serviceRequestReference = paymentsService.createServiceRequest(caseData, authToken)
                        .getServiceRequestReference();
            }
            GAPbaDetails pbaDetails = caseData.getGeneralAppPBADetails();
            GAPbaDetails.GAPbaDetailsBuilder pbaDetailsBuilder = pbaDetails.toBuilder();
            pbaDetailsBuilder
                    .fee(caseData.getGeneralAppPBADetails().getFee())
                    .serviceReqReference(serviceRequestReference);
            caseData = caseData.toBuilder()
                .generalAppPBADetails(pbaDetailsBuilder
                                          .fee(caseData.getGeneralAppPBADetails().getFee())
                                          .serviceReqReference(serviceRequestReference).build())
                .build();
            if (freeGa) {
                PaymentDetails paymentDetails = ofNullable(pbaDetails.getPaymentDetails())
                        .map(PaymentDetails::toBuilder)
                        .orElse(PaymentDetails.builder())
                        .status(SUCCESS)
                        .customerReference(serviceRequestReference)
                        .reference(serviceRequestReference)
                        .errorCode(null)
                        .errorMessage(null)
                        .build();
                pbaDetailsBuilder.paymentDetails(paymentDetails)
                                .paymentSuccessfulDate(time.now()).build();

                if (caseData.getGeneralAppUrgencyRequirement() != null
                    && caseData.getGeneralAppUrgencyRequirement().getGeneralAppUrgency() == YesOrNo.YES) {

                    gaDraftDocument = gaDraftGenerator.generate(
                        caseDataBuilder.build(),
                        callbackParams.getParams().get(BEARER_TOKEN).toString()
                    );

                    List<Element<CaseDocument>> draftApplicationList = newArrayList();

                    draftApplicationList.addAll(wrapElements(gaDraftDocument));

                    assignCategoryId.assignCategoryIdToCollection(draftApplicationList,
                                                                  document -> document.getValue().getDocumentLink(),
                                                                  AssignCategoryId.APPLICATIONS);
                    caseDataBuilder.gaDraftDocument(draftApplicationList);
                }
            }
            caseData = caseDataBuilder
                    .generalAppPBADetails(pbaDetailsBuilder.build()).build();
        } catch (FeignException e) {
            log.info(String.format("Http Status %s ", e.status()), e);
            errors.add(ERROR_MESSAGE);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseData.toMap(objectMapper))
            .errors(errors)
            .build();
    }

}
