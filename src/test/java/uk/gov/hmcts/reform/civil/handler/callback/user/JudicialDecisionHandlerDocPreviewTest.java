package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.PDFBuilder;
import uk.gov.hmcts.reform.civil.service.AssignCaseToResopondentSolHelper;
import uk.gov.hmcts.reform.civil.service.CoreCaseUserService;
import uk.gov.hmcts.reform.civil.service.DeadlinesCalculator;
import uk.gov.hmcts.reform.civil.service.GeneralAppLocationRefDataService;
import uk.gov.hmcts.reform.civil.service.JudicialDecisionHelper;
import uk.gov.hmcts.reform.civil.service.JudicialDecisionWrittenRepService;
import uk.gov.hmcts.reform.civil.service.Time;
import uk.gov.hmcts.reform.civil.service.docmosis.directionorder.DirectionOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.dismissalorder.DismissalOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.generalorder.GeneralOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.hearingorder.HearingOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.requestmoreinformation.RequestForInformationGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationconcurrentorder.WrittenRepresentationConcurrentOrderGenerator;
import uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationsequentialorder.WrittenRepresentationSequentailOrderGenerator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDate;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MAKE_DECISION;

@SpringBootTest(classes = {
    JudicialDecisionHandler.class,
    AssignCaseToResopondentSolHelper.class,
    DeadlinesCalculator.class,
    JacksonAutoConfiguration.class},
    properties = {"reference.database.enabled=false"})
public class JudicialDecisionHandlerDocPreviewTest extends BaseCallbackHandlerTest {

    @Autowired
    JudicialDecisionHandler handler;

    @MockBean
    JudicialDecisionWrittenRepService service;

    @MockBean
    JudicialDecisionHelper helper;

    @MockBean
    GeneralAppLocationRefDataService locationRefDataService;

    @MockBean
    private Time time;

    @MockBean
    private DeadlinesCalculator deadlinesCalculator;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CoreCaseUserService coreCaseUserService;

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

    @MockBean
    private IdamClient idamClient;

    @MockBean
    private UserDetails userDetails;

    @Autowired
    private final ObjectMapper mapper = new ObjectMapper();

    private final LocalDate submittedOn = now();

    @Test
    void handleEventsReturnsTheExpectedCallbackEvent() {
        assertThat(handler.handledEvents()).contains(MAKE_DECISION);
    }

    @BeforeEach
    void setup() {
        when(generalOrderGenerator.generate(any(), any()))
            .thenReturn(PDFBuilder.GENERAL_ORDER_DOCUMENT);
        when(directionOrderGenerator.generate(any(), any()))
            .thenReturn(PDFBuilder.DIRECTION_ORDER_DOCUMENT);
        when(dismissalOrderGenerator.generate(any(), any()))
            .thenReturn(PDFBuilder.DISMISSAL_ORDER_DOCUMENT);
        when(hearingOrderGenerator.generate(any(), any()))
            .thenReturn(PDFBuilder.HEARING_ORDER_DOCUMENT);
        when(writtenRepresentationSequentailOrderGenerator.generate(any(), any()))
            .thenReturn(PDFBuilder.WRITTEN_REPRESENTATION_SEQUENTIAL_DOCUMENT);
        when(writtenRepresentationConcurrentOrderGenerator.generate(any(), any()))
            .thenReturn(PDFBuilder.WRITTEN_REPRESENTATION_CONCURRENT_DOCUMENT);
        when(requestForInformationGenerator.generate(any(), any()))
            .thenReturn(PDFBuilder.REQUEST_FOR_INFORMATION_DOCUMENT);
        when(time.now()).thenReturn(submittedOn.atStartOfDay());
        when(idamClient.getUserDetails(any()))
            .thenReturn(UserDetails.builder().forename("test").surname("judge").build());
    }

    @Nested
    class MidEventForMakeDecisionPdfGeneration {

        private static final String VALIDATE_MAKE_DECISION_SCREEN = "validate-make-decision-screen";

        @Test
        void shouldReturnGenerateOrderDocument() {
            CaseData caseData = CaseDataBuilder.builder().generalOrderApplication()
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_MAKE_DECISION_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();

            verify(generalOrderGenerator).generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getJudicialMakeOrderDocPreview())
                .isEqualTo(PDFBuilder.GENERAL_ORDER_DOCUMENT.getDocumentLink());
        }

        @Test
        void shouldGenerateDirectionOrderDocument() {
            CaseData caseData = CaseDataBuilder.builder().directionOrderApplication()
                .build();

            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_MAKE_DECISION_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(directionOrderGenerator).generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getJudicialMakeOrderDocPreview())
                .isEqualTo(PDFBuilder.REQUEST_FOR_INFORMATION_DOCUMENT.getDocumentLink());
        }

        @Test
        void shouldGenerateDismissalOrderDocument() {
            CaseData caseData = CaseDataBuilder.builder().dismissalOrderApplication()
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_MAKE_DECISION_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(dismissalOrderGenerator).generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getJudicialMakeOrderDocPreview())
                .isEqualTo(PDFBuilder.DISMISSAL_ORDER_DOCUMENT.getDocumentLink());
        }
    }

    @Nested
    class MidEventForRequestMoreInfoPdfGeneration {

        private static final String VALIDATE_REQUEST_MORE_INFO_SCREEN = "validate-request-more-info-screen";

        @Test
        void shouldGenerateRequestMoreInfoDocument() {
            CaseData caseData = CaseDataBuilder.builder().requestForInformationApplication()
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_REQUEST_MORE_INFO_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(requestForInformationGenerator).generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getJudicialRequestMoreInfoDocPreview())
                .isEqualTo(PDFBuilder.REQUEST_FOR_INFORMATION_DOCUMENT.getDocumentLink());
        }
    }

    @Nested
    class MidEventForWrittenRepPdfGeneration {

        private static final String VALIDATE_WRITTEN_REPRESENTATION_DATE = "ga-validate-written-representation-date";

        @Test
        void shouldGenerateConcurrentApplicationDocument() {
            CaseData caseData = CaseDataBuilder.builder().writtenRepresentationConcurrentApplication()
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_WRITTEN_REPRESENTATION_DATE);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(writtenRepresentationConcurrentOrderGenerator)
                .generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getJudicialWrittenRepDocPreview())
                .isEqualTo(PDFBuilder.WRITTEN_REPRESENTATION_CONCURRENT_DOCUMENT.getDocumentLink());
        }

        @Test
        void shouldGenerateSequentialApplicationDocument() {
            CaseData caseData = CaseDataBuilder.builder().writtenRepresentationSequentialApplication()
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_WRITTEN_REPRESENTATION_DATE);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(writtenRepresentationSequentailOrderGenerator)
                .generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getJudicialWrittenRepDocPreview())
                .isEqualTo(PDFBuilder.WRITTEN_REPRESENTATION_SEQUENTIAL_DOCUMENT.getDocumentLink());
        }
    }

    @Nested
    class MidEventForListingForHearingPdfGeneration {

        private static final String VALIDATE_HEARING_ORDER_SCREEN = "validate-hearing-order-screen";

        @Test
        void shouldGenerateListingForHearingDocument() {
            CaseData caseData = CaseDataBuilder.builder().hearingOrderApplication(YesOrNo.NO, YesOrNo.NO)
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, VALIDATE_HEARING_ORDER_SCREEN);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            verify(hearingOrderGenerator).generate(any(CaseData.class), eq("BEARER_TOKEN"));

            CaseData updatedData = mapper.convertValue(response.getData(), CaseData.class);

            assertThat(updatedData.getJudicialListHearingDocPreview())
                .isEqualTo(PDFBuilder.HEARING_ORDER_DOCUMENT.getDocumentLink());
        }
    }
}
