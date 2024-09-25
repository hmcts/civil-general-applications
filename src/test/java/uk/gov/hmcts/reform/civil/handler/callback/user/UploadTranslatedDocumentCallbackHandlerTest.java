package uk.gov.hmcts.reform.civil.handler.callback.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;

public class UploadTranslatedDocumentCallbackHandlerTest extends BaseCallbackHandlerTest {

    private ObjectMapper objectMapper;

    private UploadTranslatedDocumentCallbackHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        handler = new UploadTranslatedDocumentCallbackHandler(objectMapper);
    }

    @Nested
    class AboutToSubmitCallback {

        @Test
        void shouldCallAboutToSubmit() {
            CaseData caseData = CaseData.builder()
                .build();

            CallbackParams params = callbackParamsOf(
                caseData,
                CaseEvent.UPLOAD_TRANSLATED_DOCUMENT,
                CallbackType.ABOUT_TO_SUBMIT
            );
            //When
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            //Then
            objectMapper.convertValue(response.getData(), CaseData.class);
            assertThat(response.getErrors()).isNull();
        }

    }
}
