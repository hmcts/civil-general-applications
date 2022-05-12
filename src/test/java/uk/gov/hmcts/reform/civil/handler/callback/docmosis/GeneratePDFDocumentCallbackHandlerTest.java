package uk.gov.hmcts.reform.civil.handler.callback.docmosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.docmosis.GeneratePDFDocumentCallbackHandler;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.Time;
import uk.gov.hmcts.reform.civil.service.docmosis.directionorder.DirectionOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.dismissalorder.DismissalOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.generalorder.GeneralOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.hearingorder.HearingOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.requestmoreinformation.RequestForInformationGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationconcurrentorder.WrittenRepresentationConcurrentOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationsequentialorder.WrittenRepresentationSequentailOrderGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.model.documents.DocumentType.GENERAL_ORDER;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    GeneratePDFDocumentCallbackHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class GeneratePDFDocumentCallbackHandlerTest extends BaseCallbackHandlerTest {

    @MockBean
    private Time time;

    @MockBean
    private GeneralOrderGenerator generalOrderGenerator;

    @MockBean
    private RequestForInformationGenerator requestForInformationGenerator;

    @MockBean
    private DirectionOrderGenerator directionOrderGenerator;

    @MockBean
    private DismissalOrderGenerator dismissalOrderGenerator;

    @MockBean
    private HearingOrderGenerator hearingOrderGenerator;

    @MockBean
    private WrittenRepresentationConcurrentOrderGenerator writtenRepresentationConcurrentOrderGenerator;

    @MockBean
    private WrittenRepresentationSequentailOrderGenerator writtenRepresentationSequentailOrderGenerator;

    @Autowired
    private GeneratePDFDocumentCallbackHandler handler;

    @Autowired
    private final ObjectMapper mapper = new ObjectMapper();

    public static final CaseDocument DOCUMENT = CaseDocument.builder()
        .createdBy("John")
        .documentName("document name")
        .documentSize(0L)
        .documentType(GENERAL_ORDER)
        .createdDatetime(LocalDateTime.now())
        .documentLink(Document.builder()
                          .documentUrl("fake-url")
                          .documentFileName("file-name")
                          .documentBinaryUrl("binary-url")
                          .build())
        .build();

    private final LocalDate submittedOn = now();

    @BeforeEach
    void setup() {
        when(generalOrderGenerator.generate(any(CaseData.class), anyString())).thenReturn(DOCUMENT);
        when(time.now()).thenReturn(submittedOn.atStartOfDay());
    }

    @Nested
    class AboutToSubmitCallback {

        @Test
        void shouldGenerateDocument_whenAboutToSubmitEventIsCalled() {
            CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(generalOrderGenerator).generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getMakeDecisionDocuments().get(0).getValue()).isEqualTo(DOCUMENT);
            assertThat(updatedData.getSubmittedOn()).isEqualTo(submittedOn);
        }

        @Test
        void shouldReturnCorrectActivityId_whenRequested() {
            CaseData caseData = CaseDataBuilder.builder().generalOrderApplication().build();

            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            assertThat(handler.camundaActivityId(params)).isEqualTo("CreatePDFDocument");
        }
    }
}
