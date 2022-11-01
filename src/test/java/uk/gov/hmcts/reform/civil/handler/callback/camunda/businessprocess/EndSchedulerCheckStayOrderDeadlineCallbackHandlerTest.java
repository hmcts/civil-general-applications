package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_GA_CASE_DATA;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    EndSchedulerCheckStayOrderDeadlineCallbackHandler.class, JacksonAutoConfiguration.class,
    ObjectMapper.class
})
public class EndSchedulerCheckStayOrderDeadlineCallbackHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private EndSchedulerCheckStayOrderDeadlineCallbackHandler handler;
    @MockBean
    private CoreCaseDataService coreCaseDataService;
    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Nested
    class AboutToSubmitCallback {

        @Test
        void shouldCheckTheStateOfApplication() {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(ORDER_MADE)
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(APPROVE_OR_EDIT)
                                               .judgeRecitalText("Sample Text")
                                               .reasonForDecisionText("Sample Test")
                                               .esOrderProcessedByStayScheduler(YesOrNo.NO).build()
                ).build();

            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isNull();
            assertThat(response.getState()).isEqualTo(ORDER_MADE.toString());
        }

        @Test
        void shouldCheck_esOrderProcessedByStayScheduler_changed() {
            CaseData caseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(ORDER_MADE)
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                                .makeAnOrder(APPROVE_OR_EDIT)
                                                .judgeRecitalText("Sample Text")
                                                .reasonForDecisionText("Sample Test")
                                                .esOrderProcessedByStayScheduler(YesOrNo.NO).build()
                ).build();

            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
            CaseData expectedCaseData = CaseDataBuilder.builder()
                .ccdCaseReference(1234L)
                .ccdState(ORDER_MADE)
                .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                               .makeAnOrder(APPROVE_OR_EDIT)
                                               .judgeRecitalText("Sample Text")
                                               .reasonForDecisionText("Sample Test")
                                               .esOrderProcessedByStayScheduler(YesOrNo.YES).build()
                ).build();
            assertThat(response.getErrors()).isNull();
            assertThat(response.getState()).isEqualTo(ORDER_MADE.toString());
            verify(coreCaseDataService).triggerGaEvent(1234L, UPDATE_GA_CASE_DATA,
                                                       getUpdatedCaseDataMapper(expectedCaseData));
        }
    }

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        Assertions.assertThat(handler.handledEvents()).contains(END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE);
    }

    private Map<String, Object> getUpdatedCaseDataMapper(CaseData caseData) {
        Map<String, Object> output = caseData.toMap(mapper);
        return output;
    }
}
