package uk.gov.hmcts.reform.civil.utils;

import uk.gov.hmcts.reform.ccd.model.SolicitorDetails;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentResponse;

import java.util.List;

public class RespondentsResponsesUtil {

    private static final int ONE_V_ONE = 1;
    private static final int ONE_V_TWO = 2;


    public static boolean isRespondentsResponseSatisfied(CaseData caseData
        , CaseData.CaseDataBuilder caseDataBuilder) {

        if (caseData.getDefendantSolicitors() == null
            || caseDataBuilder.build().getRespondentsResponses() == null) {
            return false;
        }

        List<Element<GARespondentResponse>> respondentsResponses = caseDataBuilder.build().getRespondentsResponses();
        List<Element<SolicitorDetails>> defendantSolicitors = caseData.getDefendantSolicitors();

        if (defendantSolicitors.size() == ONE_V_ONE
            && respondentsResponses != null && respondentsResponses.size() == ONE_V_ONE) {
            return true;
        }

        if (defendantSolicitors.size() == ONE_V_TWO && respondentsResponses != null) {
            return respondentsResponses.size() == ONE_V_TWO;
        }

        return false;

    }
}
