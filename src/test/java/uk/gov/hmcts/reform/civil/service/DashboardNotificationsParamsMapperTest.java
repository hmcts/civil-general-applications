package uk.gov.hmcts.reform.civil.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

@ExtendWith(MockitoExtension.class)
public class DashboardNotificationsParamsMapperTest {

    private DashboardNotificationsParamsMapper mapper;

    private CaseData caseData;

    @BeforeEach
    void setup() {
        mapper = new DashboardNotificationsParamsMapper();
        caseData = CaseDataBuilder.builder().build();
    }

    @Test
    public void shouldMapAllParameters_WhenIsRequested() {

        LocalDateTime deadline = LocalDateTime.of(2024, 3, 21, 16, 0);
        caseData = caseData.toBuilder()
            .generalAppNotificationDeadlineDate(deadline)
            .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                 .judgeRequestMoreInfoByDate(deadline.toLocalDate()).build())
            .build();

        Map<String, Object> result = mapper.mapCaseDataToParams(caseData);

        assertThat(result).extracting("generalAppNotificationDeadlineDateEn").isEqualTo("21 March 2024");
        assertThat(result).extracting("generalAppNotificationDeadlineDateCy").isEqualTo("21 Mawrth 2024");
        assertThat(result).extracting("judgeRequestMoreInfoByDateEn").isEqualTo("21 March 2024");
        assertThat(result).extracting("judgeRequestMoreInfoByDateCy").isEqualTo("21 Mawrth 2024");
    }

}
