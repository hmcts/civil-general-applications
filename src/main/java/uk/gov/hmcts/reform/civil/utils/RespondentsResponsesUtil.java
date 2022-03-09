package uk.gov.hmcts.reform.civil.utils;

import uk.gov.hmcts.reform.ccd.model.SolicitorDetails;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;

import java.util.List;

public class RespondentsResponsesUtil {

    public static boolean isRespondentsResponesSatisfied(CaseData caseData) {

        // check multiple solicitors and check if all the solicitors has response the application
        // if one solictor has response and another not return False
        // if both solicitor has responsed or timer elpase, return True
        List<Element<SolicitorDetails>> defendantSolicitors = caseData.getDefendantSolicitors();


        return true;
    }
}
