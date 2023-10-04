package uk.gov.hmcts.reform.civil.service.docmosis;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.DATE_FORMATTER;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    DocmosisService.class
})
public class DocmosisServiceTest {

    @Autowired
    private DocmosisService docmosisService;
    @MockBean
    private IdamClient idamClient;

    @Test
    void shouldRetunJudgeFullName() {
        when(idamClient
                 .getUserDetails(any()))
            .thenReturn(UserDetails.builder().forename("John").surname("Doe").build());

        assertThat(docmosisService.getJudgeNameTitle("auth")).isEqualTo("John Doe");

    }

    @Test
    void shouldPopulateJudgeReason() {

        CaseData caseData = CaseDataBuilder.builder().build();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                      .reasonForDecisionText("Test Reason")
                                                      .showReasonForDecision(YesOrNo.YES).build()).build();
        CaseData updateData = caseDataBuilder.build();

        assertThat(docmosisService.populateJudgeReason(updateData)).isEqualTo("Test Reason");
    }

    @Test
    void shouldReturnEmptyJudgeReason() {

        CaseData caseData = CaseDataBuilder.builder().build();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                      .reasonForDecisionText("Test Reason")
                                                      .showReasonForDecision(YesOrNo.NO).build()).build();
        CaseData updateData = caseDataBuilder.build();

        assertThat(docmosisService.populateJudgeReason(updateData)).isEqualTo(StringUtils.EMPTY);
    }

    @Test
    void shouldReturn_EmptyString_JudgeCourtsInitiative_Option3() {

        CaseData caseData = CaseDataBuilder.builder().build();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                      .judicialByCourtsInitiative(
                                                          GAByCourtsInitiativeGAspec.OPTION_3).build()).build();
        CaseData updateData = caseDataBuilder.build();

        assertThat(docmosisService.populateJudicialByCourtsInitiative(updateData)).isEqualTo(StringUtils.EMPTY);
    }

    @Test
    void shouldPopulate_JudgeCourtsInitiative_Option2() {

        CaseData caseData = CaseDataBuilder.builder().build();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                      .orderWithoutNotice("abcdef")
                                                      .orderWithoutNoticeDate(LocalDate.now())
                                                      .judicialByCourtsInitiative(
                                                          GAByCourtsInitiativeGAspec.OPTION_2)
                                                      .build()).build();
        CaseData updateData = caseDataBuilder.build();

        assertThat(docmosisService.populateJudicialByCourtsInitiative(updateData))
            .isEqualTo("abcdef ".concat(LocalDate.now().format(DATE_FORMATTER)));
    }

    @Test
    void shouldPopulate_JudgeCourtsInitiative_Option1() {

        CaseData caseData = CaseDataBuilder.builder().build();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder.judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                      .orderCourtOwnInitiative("abcdef")
                                                      .orderCourtOwnInitiativeDate(LocalDate.now())
                                                      .judicialByCourtsInitiative(
                                                          GAByCourtsInitiativeGAspec.OPTION_1)
                                                      .build()).build();
        CaseData updateData = caseDataBuilder.build();

        assertThat(docmosisService.populateJudicialByCourtsInitiative(updateData))
            .isEqualTo("abcdef ".concat(LocalDate.now().format(DATE_FORMATTER)));
    }

}
