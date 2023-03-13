package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GENERATE_DIRECTIONS_ORDER;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;

@SpringBootTest(classes = {
        JudicialFinalDecisionHandler.class,
        JacksonAutoConfiguration.class,
})
class JudicialFinalDecisionHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private JudicialFinalDecisionHandler handler;
    @Autowired
    private ObjectMapper objMapper;

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(GENERATE_DIRECTIONS_ORDER);
    }

    @Test
    void setCaseName() {
        CaseData caseData = CaseDataBuilder.builder()
                .atStateClaimDraft()
                .build().toBuilder()
                .claimant1PartyName("Mr. John Rambo")
                .defendant1PartyName("Mr. Sole Trader")
                .build();
        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_START);
        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        assertThat(response.getData().get("caseNameHmctsInternal")
                .toString()).isEqualTo("Mr. John Rambo V Mr. Sole Trader");
    }

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