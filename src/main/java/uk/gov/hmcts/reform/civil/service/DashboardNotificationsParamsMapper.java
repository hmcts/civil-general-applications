package uk.gov.hmcts.reform.civil.service;

import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions.SEQUENTIAL_REPRESENTATIONS;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
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

}
