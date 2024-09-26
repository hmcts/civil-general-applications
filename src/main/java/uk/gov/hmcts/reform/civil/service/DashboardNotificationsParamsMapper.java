package uk.gov.hmcts.reform.civil.service;

import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
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

        if (caseData.getGaHwfDetails() != null && (caseData.getHwfFeeType() != null && FeeType.APPLICATION == caseData.getHwfFeeType())) {
            params.put("remissionAmount", "£" + MonetaryConversions.penniesToPounds(caseData.getGaHwfDetails().getRemissionAmount()));
            params.put("outstandingFeeInPounds", "£" + caseData.getGaHwfDetails().getOutstandingFeeInPounds());
        } else if (caseData.getAdditionalHwfDetails() != null && (caseData.getHwfFeeType() != null
            && FeeType.ADDITIONAL == caseData.getHwfFeeType())) {
            params.put("remissionAmount", "£" + MonetaryConversions.penniesToPounds(caseData.getAdditionalHwfDetails()
                                                                                        .getRemissionAmount()));
            params.put("outstandingFeeInPounds", "£" + caseData.getAdditionalHwfDetails().getOutstandingFeeInPounds());

        }

        if (Objects.nonNull(caseData.getJudicialDecisionRequestMoreInfo())) {
            params.put("judgeRequestMoreInfoByDateEn", DateUtils.formatDate(caseData.getJudicialDecisionRequestMoreInfo().getJudgeRequestMoreInfoByDate()));
            params.put("judgeRequestMoreInfoByDateCy",
                       DateUtils.formatDateInWelsh(caseData.getJudicialDecisionRequestMoreInfo().getJudgeRequestMoreInfoByDate()));
        }

        if (caseData.getHwfFeeType() != null) {
            if (FeeType.APPLICATION == caseData.getHwfFeeType()) {
                params.put("applicationFeeTypeEn", "application");
                params.put("applicationFeeTypeCy", "cais");
            } else if (FeeType.ADDITIONAL == caseData.getHwfFeeType()) {
                params.put("applicationFeeTypeEn", "additional application");
                params.put("applicationFeeTypeCy", "cais ychwanegol");
            }
        }
        if (Objects.nonNull(caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations())) {
            LocalDate applicantDeadlineDate;
            LocalDate respondentDeadlineDate;
            if (caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations().getWrittenOption() == SEQUENTIAL_REPRESENTATIONS) {
                applicantDeadlineDate = caseData.getParentClaimantIsApplicant() == YesOrNo.YES
                    ? caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations().getSequentialApplicantMustRespondWithin()
                    : caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations().getWrittenSequentailRepresentationsBy();
                respondentDeadlineDate = caseData.getParentClaimantIsApplicant() == YesOrNo.YES
                    ? caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations().getWrittenSequentailRepresentationsBy()
                    : caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations().getSequentialApplicantMustRespondWithin();
            } else {
                applicantDeadlineDate = caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations().getWrittenConcurrentRepresentationsBy();
                respondentDeadlineDate = caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations().getWrittenConcurrentRepresentationsBy();
            }
            params.put("writtenRepApplicantDeadlineDateEn", DateUtils.formatDate(applicantDeadlineDate));
            params.put("writtenRepApplicantDeadlineDateCy", DateUtils.formatDateInWelsh(applicantDeadlineDate));
            params.put("writtenRepRespondentDeadlineDateEn", DateUtils.formatDate(respondentDeadlineDate));
            params.put("writtenRepRespondentDeadlineDateCy", DateUtils.formatDateInWelsh(respondentDeadlineDate));
        }
        //ToDo: refactor below string to allow for notifications that do not require additional params
        params.put("testRef", "string");

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
