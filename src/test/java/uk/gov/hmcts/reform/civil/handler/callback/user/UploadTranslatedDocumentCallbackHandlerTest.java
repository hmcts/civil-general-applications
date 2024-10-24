package uk.gov.hmcts.reform.civil.handler.callback.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CallbackType;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.UploadTranslatedDocumentService;

@ExtendWith(MockitoExtension.class)
public class UploadTranslatedDocumentCallbackHandlerTest extends BaseCallbackHandlerTest {

    private ObjectMapper objectMapper;
    @Mock
    private UploadTranslatedDocumentService uploadTranslatedDocumentService;
    @InjectMocks
    private UploadTranslatedDocumentCallbackHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        handler = new UploadTranslatedDocumentCallbackHandler(objectMapper, uploadTranslatedDocumentService);
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
            CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
            when(uploadTranslatedDocumentService.processTranslatedDocument(caseData)).thenReturn(caseDataBuilder);
            //When
            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            //Then
            objectMapper.convertValue(response.getData(), CaseData.class);
            assertThat(response.getErrors()).isNull();
            verify(uploadTranslatedDocumentService).processTranslatedDocument(caseData);
        }

    }
}
