package uk.gov.hmcts.reform.civil.service.flowstate;

import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.function.Predicate;

import static uk.gov.hmcts.reform.civil.enums.PaymentStatus.FAILED;

public class FlowPredicate {

    private FlowPredicate() {
        //Utility class
    }

    public static final Predicate<CaseData> paymentSuccessful = caseData ->
        getPaymentSuccessful(caseData);

    private static boolean getPaymentSuccessful(CaseData caseData) {
        boolean predicate = false;
        if (caseData.getGeneralAppPBADetails() != null) {
            predicate = (caseData.getGeneralAppPBADetails().getPaymentSuccessfulDate() != null);
        }
        return predicate;
    }

    public static final Predicate<CaseData> paymentFailed = caseData ->
        getPaymentFailurePredicate(caseData);

    private static boolean getPaymentFailurePredicate(CaseData caseData) {
        boolean predicate = false;
        if (caseData.getGeneralAppPBADetails() != null) {
            predicate = (caseData.getGeneralAppPBADetails().getPaymentSuccessfulDate() == null
                && (caseData.getGeneralAppPBADetails().getPaymentDetails() != null
                && caseData.getGeneralAppPBADetails().getPaymentDetails().getStatus() == FAILED));
        }
        return predicate;
    }

}
