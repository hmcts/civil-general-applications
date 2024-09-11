package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.Fee;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.REQUEST_MORE_INFORMATION;

@ExtendWith(MockitoExtension.class)
public class DashboardNotificationsParamsMapperTest {

    private CaseData caseData;

    @InjectMocks
    private DashboardNotificationsParamsMapper mapper;
    public static final String CUSTOMER_REFERENCE = "12345";

    @Test
    void shouldMapAllParametersWhenIsRequested() {
        caseData = CaseDataBuilder.builder().build().toBuilder()
            .ccdCaseReference(1644495739087775L)
            .legacyCaseReference("000DC001")
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .fee(
                        Fee.builder()
                            .code("FE203")
                            .calculatedAmountInPence(BigDecimal.valueOf(27500))
                            .version("1")
                            .build())
                    .serviceReqReference(CUSTOMER_REFERENCE).build())
            .generalAppSuperClaimType("SPEC_CLAIM")
              .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder().requestMoreInfoOption(
                REQUEST_MORE_INFORMATION).judgeRequestMoreInfoByDate(LocalDate.of(2024, 9, 04)).build()).build();

        Map<String, Object> result = mapper.mapCaseDataToParams(caseData);

        assertThat(result).extracting("applicationFee").isEqualTo("£275.00");
        assertThat(result).extracting("judgeRequestMoreInfoByDateEn").isEqualTo("4 September 2024");
        assertThat(result).extracting("judgeRequestMoreInfoByDateCy").isEqualTo("4 Medi 2024");
    }

    @Test
    void shouldNotMapDataWhenNotPresent() {
        CaseData caseData = CaseDataBuilder.builder().buildMakePaymentsCaseData();
        caseData = caseData.toBuilder().generalAppPBADetails(null).build();
        Map<String, Object> result = mapper.mapCaseDataToParams(caseData);
        assertFalse(result.containsKey("applicationFee"));
        assertFalse(result.containsKey("judgeRequestMoreInfoByDateEn"));
    }

    @Test
    void shouldMapAllParametersWhenIsRequestedForHearingScheduled() {

        CaseData caseData = CaseDataBuilder.builder().buildCaseWorkerHearingScheduledInfo();
        Map<String, Object> result = mapper.mapCaseDataToParams(caseData);
        assertThat(result).extracting("hearingNoticeApplicationDateEn").isEqualTo("4 September 2024");
        assertThat(result).extracting("hearingNoticeApplicationDateCy").isEqualTo("4 Medi 2024");
    }

    @Test
    void shouldNotMapCaseworkerHearingDateInfoDateNotPresent() {

        CaseData caseData = CaseDataBuilder.builder().buildMakePaymentsCaseData();
        caseData = caseData.toBuilder().generalAppPBADetails(null).build();
        Map<String, Object> result = mapper.mapCaseDataToParams(caseData);
        assertFalse(result.containsKey("hearingNoticeApplicationDateEn"));
    }
}
