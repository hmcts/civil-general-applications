package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;
import uk.gov.hmcts.reform.civil.service.StateGeneratorService;
import uk.gov.hmcts.reform.civil.utils.ApplicationNotificationUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_JUDGE_BUSINESS_PROCESS_GASPEC;

@SpringBootTest(classes = {
    EndJudgeMakesDecisionBusinessProcessCallbackHandler.class,
    CaseDetailsConverter.class,
    CoreCaseDataService.class,
    ParentCaseUpdateHelper.class,
    ObjectMapper.class,
    ApplicationNotificationUtil.class
})
class EndJudgeMakesDecisionBusinessProcessCallbackHandlerTest {

    @Autowired
    private EndJudgeMakesDecisionBusinessProcessCallbackHandler handler;

    @MockBean
    private CaseDetailsConverter caseDetailsConverter;

    @MockBean
    private CoreCaseDataService coreCaseDataService;

    @MockBean
    private StateGeneratorService stateGeneratorService;

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(END_JUDGE_BUSINESS_PROCESS_GASPEC);
    }
}
