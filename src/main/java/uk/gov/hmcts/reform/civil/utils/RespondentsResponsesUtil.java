package uk.gov.hmcts.reform.civil.utils;

import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentResponse;

import java.util.List;

public class RespondentsResponsesUtil {

    private static final int ONE_V_ONE = 1;
    private static final int ONE_V_TWO = 2;

    private RespondentsResponsesUtil() {
        // Utilities class, no instances
    }

    public static boolean isRespondentsResponseSatisfied(CaseData caseData, CaseData updatedCaseData) {

        if (caseData.getGeneralAppRespondentSolicitors() == null
            || updatedCaseData.getRespondentsResponses() == null) {
            return false;
        }

        List<Element<GARespondentResponse>> respondentsResponses = updatedCaseData.getRespondentsResponses();
        int noOfDefendantSolicitors = caseData.getGeneralAppRespondentSolicitors().size();

        if (noOfDefendantSolicitors == ONE_V_ONE
            && respondentsResponses != null && respondentsResponses.size() == ONE_V_ONE) {
            return true;
        }

        if (noOfDefendantSolicitors == ONE_V_TWO && respondentsResponses != null) {
            return respondentsResponses.size() == ONE_V_TWO;
        }

        return false;

    }
}
