package uk.gov.hmcts.reform.civil.service;

import java.util.HashMap;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.utils.MonetaryConversions;
import uk.gov.hmcts.reform.civil.utils.DateUtils;

@Service
@RequiredArgsConstructor
public class DashboardNotificationsParamsMapper {

    public HashMap<String, Object> mapCaseDataToParams(CaseData caseData) {
        HashMap<String, Object> params = new HashMap<>();

        if (caseData.getGeneralAppPBADetails() != null) {
            params.put("applicationFee",
                       "£" + MonetaryConversions.penniesToPounds(caseData.getGeneralAppPBADetails().getFee().getCalculatedAmountInPence()));
        }

        if (Objects.nonNull(caseData.getJudicialDecisionRequestMoreInfo())) {
            params.put("judgeRequestMoreInfoByDateEn", DateUtils.formatDate(caseData
                                                                                .getJudicialDecisionRequestMoreInfo()
                                                                                .getJudgeRequestMoreInfoByDate()));
            params.put("judgeRequestMoreInfoByDateCy",
                       DateUtils.formatDateInWelsh(caseData.getJudicialDecisionRequestMoreInfo()
                                                       .getJudgeRequestMoreInfoByDate()));
        }

        if (caseData.getCcdState().equals(CaseState.LISTING_FOR_A_HEARING)
            && Objects.nonNull(caseData.getGaHearingNoticeDetail())) {

            params.put("hearingNoticeApplicationDateEn", DateUtils.formatDate(caseData.getGaHearingNoticeDetail()
                                                                                  .getHearingDate()));
            params.put("hearingNoticeApplicationDateCy",
                       DateUtils.formatDateInWelsh(caseData.getGaHearingNoticeDetail().getHearingDate()));
        }
        return params;
    }

}
