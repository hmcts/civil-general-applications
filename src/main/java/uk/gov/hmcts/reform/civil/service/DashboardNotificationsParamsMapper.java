package uk.gov.hmcts.reform.civil.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.utils.DateUtils;
import uk.gov.hmcts.reform.civil.utils.MonetaryConversions;

@Service
@RequiredArgsConstructor
public class DashboardNotificationsParamsMapper {

    public HashMap<String, Object> mapCaseDataToParams(CaseData caseData) {
        HashMap<String, Object> params = new HashMap<>();

        params.put("ccdCaseReference", caseData.getCcdCaseReference());

        getGeneralAppNotificationDeadlineDate(caseData).ifPresent(date -> {
            params.put("generalAppNotificationDeadlineDateEn", DateUtils.formatDate(date));
            params.put("generalAppNotificationDeadlineDateCy", DateUtils.formatDateInWelsh(date));
        });

        getJudgeRequestMoreInfoByDate(caseData).ifPresent(date -> {
            params.put("judgeRequestMoreInfoByDateEn", DateUtils.formatDate(date));
            params.put("judgeRequestMoreInfoByDateCy", DateUtils.formatDateInWelsh(date));
        });

        if (caseData.getCcdState().equals(CaseState.LISTING_FOR_A_HEARING)) {

            getGeneralAppListingForHearingDate(caseData).ifPresent(date -> {
                params.put("hearingNoticeApplicationDateEn", DateUtils.formatDate(date));
                params.put("hearingNoticeApplicationDateCy",
                           DateUtils.formatDateInWelsh(date));
            });

        }

        if (caseData.getGeneralAppPBADetails() != null) {
            params.put("applicationFee",
                       "£" + MonetaryConversions.penniesToPounds(caseData.getGeneralAppPBADetails().getFee().getCalculatedAmountInPence()));
        }

        if (caseData.getHwfFeeType() != null || caseData.getGeneralAppType() != null) {
            if (FeeType.ADDITIONAL == caseData.getHwfFeeType()
                || caseData.getCcdState().equals(CaseState.APPLICATION_ADD_PAYMENT)) {
                params.put("applicationFeeTypeEn", "additional application");
                params.put("applicationFeeTypeCy", "cais ychwanegol");
            } else if (FeeType.APPLICATION == caseData.getHwfFeeType()
                || caseData.getCcdState().equals(CaseState.AWAITING_APPLICATION_PAYMENT)) {
                params.put("applicationFeeTypeEn", "application");
                params.put("applicationFeeTypeCy", "cais");
            }
        }

        return params;
    }

    private static Optional<LocalDate> getGeneralAppListingForHearingDate(CaseData caseData) {
        return Optional.ofNullable(caseData.getGaHearingNoticeDetail().getHearingDate());
    }

    private static Optional<LocalDate> getGeneralAppNotificationDeadlineDate(CaseData caseData) {
        return Optional.ofNullable(caseData.getGeneralAppNotificationDeadlineDate())
            .map(LocalDateTime::toLocalDate);
    }

    private static Optional<LocalDate> getJudgeRequestMoreInfoByDate(CaseData caseData) {
        return Optional.ofNullable(caseData.getJudicialDecisionRequestMoreInfo())
            .map(GAJudicialRequestMoreInfo::getJudgeRequestMoreInfoByDate);
    }

}
