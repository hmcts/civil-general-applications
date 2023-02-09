package uk.gov.hmcts.reform.civil.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaseMigrationUtil {

    private final CoreCaseDataService coreCaseDataService;

    public void migrateGaCaseProgression(CaseData.CaseDataBuilder caseDataBuilder,
                                              YesOrNo enabledFlag) {

        caseDataBuilder.isCaseProgressionEnabled(enabledFlag);
    }

}
