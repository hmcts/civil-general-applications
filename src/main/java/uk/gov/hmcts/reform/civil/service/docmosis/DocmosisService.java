package uk.gov.hmcts.reform.civil.service.docmosis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class DocmosisService {

    private final IdamClient idamInfo;

    public String getJudgeNameTitle(String authorisation) {
        UserDetails userDetails = idamInfo.getUserDetails(authorisation);
        return userDetails.getFullName();
    }

    public YesOrNo reasonAvailable(CaseData caseData) {
        if (Objects.nonNull(caseData.getJudicialDecisionMakeOrder().getShowReasonForDecision())
            && caseData.getJudicialDecisionMakeOrder().getShowReasonForDecision().equals(YesOrNo.NO)) {
            return YesOrNo.NO;
        }
        return YesOrNo.YES;
    }
}
