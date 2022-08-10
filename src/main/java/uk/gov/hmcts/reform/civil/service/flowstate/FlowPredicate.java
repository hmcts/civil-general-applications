package uk.gov.hmcts.reform.civil.service.flowstate;

import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.function.Predicate;

import static uk.gov.hmcts.reform.civil.enums.PaymentStatus.FAILED;

public class FlowPredicate {

    private FlowPredicate() {
        //Utility class
    }

    public static final Predicate<CaseData> paymentSuccessful = caseData ->
        caseData.getGeneralAppPBADetails().getPaymentSuccessfulDate() != null;

    public static final Predicate<CaseData> paymentFailed = caseData ->
        caseData.getGeneralAppPBADetails().getPaymentSuccessfulDate() == null
            && (caseData.getGeneralAppPBADetails().getPaymentDetails() != null
            && caseData.getGeneralAppPBADetails().getPaymentDetails().getStatus() == FAILED);
}
