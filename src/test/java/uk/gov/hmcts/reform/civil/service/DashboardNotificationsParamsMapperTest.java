package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
public class DashboardNotificationsParamsMapperTest {

    @InjectMocks
    private DashboardNotificationsParamsMapper mapper;

    @Test
    void shouldMapAllParametersWhenIsRequested() {

        CaseData caseData = CaseDataBuilder.builder().buildJudicialDecisionRequestMoreInfo();
        Map<String, Object> result = mapper.mapCaseDataToParams(caseData);
        assertThat(result).extracting("applicationFee").isEqualTo("£275.00");
        assertThat(result).extracting("judgeRequestMoreInfoByDateEn").isEqualTo("4 September 2024");
        assertThat(result).extracting("judgeRequestMoreInfoByDateCy").isEqualTo("4 Medi 2024");
    }

    @Test
    void shouldNotMapJudgeRequestMoreInfoDateNotPresent() {
        CaseData caseData = CaseDataBuilder.builder().buildMakePaymentsCaseData();
        caseData = caseData.toBuilder().generalAppPBADetails(null).build();
        Map<String, Object> result = mapper.mapCaseDataToParams(caseData);
        assertFalse(result.containsKey("judgeRequestMoreInfoByDateEn"));
    }

    @Test
    void shouldNotMapApplicationFeeWhenPbaDetailsAreNotPresent() {

        CaseData caseData = CaseDataBuilder.builder().buildMakePaymentsCaseData();
        caseData = caseData.toBuilder().generalAppPBADetails(null).build();
        Map<String, Object> result = mapper.mapCaseDataToParams(caseData);
        assertFalse(result.containsKey("applicationFee"));
    }
}
