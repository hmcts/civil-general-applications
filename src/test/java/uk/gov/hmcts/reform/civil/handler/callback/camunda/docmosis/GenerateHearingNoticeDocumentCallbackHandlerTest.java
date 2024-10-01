package uk.gov.hmcts.reform.civil.handler.callback.camunda.docmosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.GaForLipService;
import uk.gov.hmcts.reform.civil.service.SendFinalOrderPrintService;
import uk.gov.hmcts.reform.civil.service.docmosis.hearingorder.HearingFormGenerator;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    GenerateHearingNoticeDocumentCallbackHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    AssignCategoryId.class
})
class GenerateHearingNoticeDocumentCallbackHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private GenerateHearingNoticeDocumentCallbackHandler handler;
    @MockBean
    private HearingFormGenerator hearingFormGenerator;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private GaForLipService gaForLipService;
    @MockBean
    private CaseDetailsConverter caseDetailsConverter;
    @MockBean
    private CoreCaseDataService coreCaseDataService;
    @MockBean
    private SendFinalOrderPrintService sendFinalOrderPrintService;

    @Autowired
    private AssignCategoryId assignCategoryId;

    @Test
    void shouldReturnCorrectActivityId_whenRequested() {
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication().build();

        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        assertThat(handler.camundaActivityId()).isEqualTo("GenerateHearingNoticeDocument");
    }

    @Test
    void shouldGenerateHearingNoticeDocument_whenAboutToSubmitEventIsCalled() {
        CaseDocument caseDocument = CaseDocument.builder()
            .documentLink(Document.builder().documentUrl("doc").build()).build();

        when(hearingFormGenerator.generate(any(), any())).thenReturn(caseDocument);
        when(gaForLipService.isGaForLip(any())).thenReturn(false);
        CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
            .build();
        CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

        var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);
        CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);
        assertThat(updatedData.getHearingNoticeDocument().size()).isEqualTo(1);
    }
}
