package uk.gov.hmcts.reform.civil.service.tasklisteners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.model.CaseData;

import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;

@Slf4j
@Service
@RequiredArgsConstructor
public class GaForLipService {

    public boolean isGaForLip(CaseData caseData) {
        return caseData.getIsGaApplicantLip().equals(YES)
            || caseData.getIsGaRespondentOneLip().equals(YES)
            || caseData.getIsGaRespondentOneLip().equals(YES);

    }
}
