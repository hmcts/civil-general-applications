package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_DOC_UPLOAD_BUSINESS_PROCESS_GASPEC;

@SpringBootTest(classes = {
    EndGaDocUploadProcessCallbackHandler.class,
    ObjectMapper.class
})
class EndGaDocUploadProcessCallbackHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private EndGaDocUploadProcessCallbackHandler handler;

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(END_DOC_UPLOAD_BUSINESS_PROCESS_GASPEC);
    }
}
