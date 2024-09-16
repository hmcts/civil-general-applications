package uk.gov.hmcts.reform.civil.service;

import java.util.HashMap;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.utils.MonetaryConversions;
import uk.gov.hmcts.reform.civil.utils.DateUtils;

@Service
@RequiredArgsConstructor
public class DashboardNotificationsParamsMapper {

    public HashMap<String, Object> mapCaseDataToParams(CaseData caseData) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("ccdCaseReference", caseData.getCcdCaseReference());
        if (caseData.getGeneralAppPBADetails() != null) {
            params.put("applicationFee",
                       "£" + MonetaryConversions.penniesToPounds(caseData.getGeneralAppPBADetails().getFee().getCalculatedAmountInPence()));
        }

        if (Objects.nonNull(caseData.getJudicialDecisionRequestMoreInfo())) {
            params.put("judgeRequestMoreInfoByDateEn", DateUtils.formatDate(caseData.getJudicialDecisionRequestMoreInfo().getJudgeRequestMoreInfoByDate()));
            params.put("judgeRequestMoreInfoByDateCy",
                       DateUtils.formatDateInWelsh(caseData.getJudicialDecisionRequestMoreInfo().getJudgeRequestMoreInfoByDate()));
        }
        //ToDo: refactor below string to allow for notifications that do not require additional params
        params.put("testRef", "string");

        if (caseData.getHwfFeeType() != null) {
            if (FeeType.APPLICATION == caseData.getHwfFeeType()) {
                params.put("applicationFeeTypeEn", "application");
                params.put("applicationFeeTypeCy", "cais");
            } else if (FeeType.ADDITIONAL == caseData.getHwfFeeType()) {
                params.put("applicationFeeTypeEn", "additional application");
                params.put("applicationFeeTypeCy", "cais ychwanegol");
            }
        }

        return params;
    }

}
