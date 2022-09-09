package uk.gov.hmcts.reform.civil.handler.callback.camunda.docmosis;

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
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.PDFBuilder;
import uk.gov.hmcts.reform.civil.service.Time;
import uk.gov.hmcts.reform.civil.service.docmosis.directionorder.DirectionOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.dismissalorder.DismissalOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.generalorder.GeneralOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.hearingorder.HearingOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.requestmoreinformation.RequestForInformationGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationconcurrentorder.WrittenRepresentationConcurrentOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationsequentialorder.WrittenRepresentationSequentailOrderGenerator;

import java.time.LocalDate;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;

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

    private final LocalDate submittedOn = now();

    @BeforeEach
    void setup() {
        when(generalOrderGenerator.generate(any(CaseData.class), anyString()))
            .thenReturn(PDFBuilder.GENERAL_ORDER_DOCUMENT);
        when(directionOrderGenerator.generate(any(CaseData.class), anyString()))
            .thenReturn(PDFBuilder.DIRECTION_ORDER_DOCUMENT);
        when(dismissalOrderGenerator.generate(any(CaseData.class), anyString()))
            .thenReturn(PDFBuilder.DISMISSAL_ORDER_DOCUMENT);
        when(hearingOrderGenerator.generate(any(CaseData.class), anyString()))
            .thenReturn(PDFBuilder.HEARING_ORDER_DOCUMENT);
        when(writtenRepresentationSequentailOrderGenerator.generate(any(CaseData.class), anyString()))
            .thenReturn(PDFBuilder.WRITTEN_REPRESENTATION_SEQUENTIAL_DOCUMENT);
        when(writtenRepresentationConcurrentOrderGenerator.generate(any(CaseData.class), anyString()))
            .thenReturn(PDFBuilder.WRITTEN_REPRESENTATION_CONCURRENT_DOCUMENT);
        when(requestForInformationGenerator.generate(any(CaseData.class), anyString()))
            .thenReturn(PDFBuilder.REQUEST_FOR_INFORMATION_DOCUMENT);
        when(time.now()).thenReturn(submittedOn.atStartOfDay());
    }

    @Nested
    class AboutToSubmitCallback {

        @Test
        void shouldGenerateGeneralOrderDocument_whenAboutToSubmitEventIsCalled() {
            CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(generalOrderGenerator).generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getGeneralOrderDocument().get(0).getValue())
                .isEqualTo(PDFBuilder.GENERAL_ORDER_DOCUMENT);
            assertThat(updatedData.getSubmittedOn()).isEqualTo(submittedOn);
        }

        @Test
        void shouldGenerateDirectionOrderDocument_whenAboutToSubmitEventIsCalled() {
            CaseData caseData = CaseDataBuilder.builder().directionOrderApplication()
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(directionOrderGenerator).generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getDirectionOrderDocument().get(0).getValue())
                .isEqualTo(PDFBuilder.DIRECTION_ORDER_DOCUMENT);
            assertThat(updatedData.getSubmittedOn()).isEqualTo(submittedOn);
        }

        @Test
        void shouldGenerateDismissalOrderDocument_whenAboutToSubmitEventIsCalled() {
            CaseData caseData = CaseDataBuilder.builder().dismissalOrderApplication()
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(dismissalOrderGenerator).generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getDismissalOrderDocument().get(0).getValue())
                .isEqualTo(PDFBuilder.DISMISSAL_ORDER_DOCUMENT);
            assertThat(updatedData.getSubmittedOn()).isEqualTo(submittedOn);
        }

        @Test
        void shouldGenerateHearingOrderDocument_whenAboutToSubmitEventIsCalled() {
            CaseData caseData = CaseDataBuilder.builder().hearingOrderApplication(YesOrNo.NO, YesOrNo.NO)
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(hearingOrderGenerator).generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getHearingOrderDocument().get(0).getValue())
                .isEqualTo(PDFBuilder.HEARING_ORDER_DOCUMENT);
            assertThat(updatedData.getSubmittedOn()).isEqualTo(submittedOn);
        }

        @Test
        void shouldGenerateWrittenRepresentationSequentialDocument_whenAboutToSubmitEventIsCalled() {
            CaseData caseData = CaseDataBuilder.builder().writtenRepresentationSequentialApplication()
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(writtenRepresentationSequentailOrderGenerator).generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getWrittenRepSequentialDocument().get(0).getValue())
                .isEqualTo(PDFBuilder.WRITTEN_REPRESENTATION_SEQUENTIAL_DOCUMENT);
            assertThat(updatedData.getSubmittedOn()).isEqualTo(submittedOn);
        }

        @Test
        void shouldGenerateWrittenRepresentationConccurentDocument_whenAboutToSubmitEventIsCalled() {
            CaseData caseData = CaseDataBuilder.builder().writtenRepresentationConcurrentApplication()
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(writtenRepresentationConcurrentOrderGenerator).generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getWrittenRepConcurrentDocument().get(0).getValue())
                .isEqualTo(PDFBuilder.WRITTEN_REPRESENTATION_CONCURRENT_DOCUMENT);
            assertThat(updatedData.getSubmittedOn()).isEqualTo(submittedOn);
        }

        @Test
        void shouldGenerateRequestForInformationDocument_whenAboutToSubmitEventIsCalled() {
            CaseData caseData = CaseDataBuilder.builder().requestForInformationApplication()
                .build();
            CallbackParams params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(requestForInformationGenerator).generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getRequestForInformationDocument().get(0).getValue())
                .isEqualTo(PDFBuilder.REQUEST_FOR_INFORMATION_DOCUMENT);
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
