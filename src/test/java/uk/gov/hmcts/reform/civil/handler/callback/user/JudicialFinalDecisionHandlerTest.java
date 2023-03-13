package uk.gov.hmcts.reform.civil.handler.callback.user;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        JudicialFinalDecisionHandlerTest.class,
        JacksonAutoConfiguration.class,
})
class JudicialFinalDecisionHandlerTest {

    @Nested
    class GetAllPartyNames {
        @Test
        void oneVOne() {
            CaseData caseData = CaseDataBuilder.builder()
                    .atStateClaimDraft()
                    .build().toBuilder()
                    .claimant1PartyName("Mr. John Rambo")
                    .defendant1PartyName("Mr. Sole Trader")
                    .build();
            String title = JudicialFinalDecisionHandler.getAllPartyNames(caseData);
            assertThat(title).isEqualTo("Mr. John Rambo V Mr. Sole Trader");
        }

        @Test
        void oneVTwoSameSol() {
            CaseData caseData = CaseDataBuilder.builder()
                    .atStateClaimDraft()
                    .build().toBuilder()
                    .respondent2SameLegalRepresentative(YesOrNo.YES)
                    .claimant1PartyName("Mr. John Rambo")
                    .defendant1PartyName("Mr. Sole Trader")
                    .defendant2PartyName("Mr. John Rambo")
                    .build();

            String title = JudicialFinalDecisionHandler.getAllPartyNames(caseData);
            assertThat(title).isEqualTo("Mr. John Rambo V Mr. Sole Trader");
        }

        @Test
        void oneVTwo() {
            CaseData caseData = CaseDataBuilder.builder()
                    .atStateClaimDraft()
                    .build().toBuilder()
                    .respondent2SameLegalRepresentative(YesOrNo.NO)
                    .claimant1PartyName("Mr. John Rambo")
                    .defendant1PartyName("Mr. Sole Trader")
                    .defendant2PartyName("Mr. John Rambo")
                    .build();

            String title = JudicialFinalDecisionHandler.getAllPartyNames(caseData);
            assertThat(title).isEqualTo("Mr. John Rambo V Mr. Sole Trader, Mr. John Rambo");
        }
    }
}