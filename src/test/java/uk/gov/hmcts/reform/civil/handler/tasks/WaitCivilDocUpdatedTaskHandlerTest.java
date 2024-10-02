package uk.gov.hmcts.reform.civil.handler.tasks;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.WAIT_GA_DRAFT;

import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.GaForLipService;
import uk.gov.hmcts.reform.civil.service.data.ExternalTaskInput;
import uk.gov.hmcts.reform.civil.utils.ElementUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    WaitCivilDocUpdatedTaskHandler.class
})
public class WaitCivilDocUpdatedTaskHandlerTest {

    @MockBean
    private ExternalTask externalTask;

    @MockBean
    private ExternalTaskService externalTaskService;
    @MockBean
    private CoreCaseDataService coreCaseDataService;
    @MockBean
    private ObjectMapper mapper;
    @MockBean
    private CaseDetailsConverter caseDetailsConverter;
    @MockBean
    private GaForLipService gaForLipService;
    @Mock
    private ExternalTask mockTask;

    @Autowired
    private WaitCivilDocUpdatedTaskHandler waitCivilDocUpdatedTaskHandler;

    private CaseData gaCaseData;
    private CaseData civilCaseDataEmpty;
    private CaseData civilCaseDataOld;
    private CaseData civilCaseDataNow;
    private static final String CASE_ID = "1644495739087775L";

    @BeforeEach
    void init() {
        when(gaForLipService.isGaForLip(any())).thenReturn(false);
        CaseDocument caseDocumentNow = CaseDocument.builder().documentName("current")
                .documentLink(Document.builder().documentUrl("url")
                        .documentFileName("filename").documentHash("hash")
                        .documentBinaryUrl("binaryUrl").build())
                .documentType(DocumentType.GENERAL_APPLICATION_DRAFT).documentSize(12L).build();
        CaseDocument caseDocumentOld = CaseDocument.builder().documentName("old")
                .documentLink(Document.builder().documentUrl("url")
                        .documentFileName("filename").documentHash("hash")
                        .documentBinaryUrl("binaryUrl").build())
                .documentType(DocumentType.GENERAL_APPLICATION_DRAFT).documentSize(12L).build();
        gaCaseData = CaseData.builder()
                .generalAppParentCaseLink(
                        GeneralAppParentCaseLink.builder().caseReference("123").build())
                .gaDraftDocument(ElementUtils.wrapElements(caseDocumentNow))
                .build();
        civilCaseDataEmpty = CaseData.builder().build();
        civilCaseDataOld = CaseData.builder()
                .gaDraftDocStaff(ElementUtils.wrapElements(caseDocumentOld))
                .build();
        civilCaseDataNow = CaseData.builder()
                .gaDraftDocStaff(ElementUtils.wrapElements(caseDocumentNow))
                .build();
    }

    @Test
    void should_handle_task_pass() {
        ExternalTaskInput externalTaskInput = ExternalTaskInput.builder().caseId("1")
                .caseEvent(WAIT_GA_DRAFT).build();
        when(mapper.convertValue(any(), eq(ExternalTaskInput.class))).thenReturn(externalTaskInput);
        CaseDetails caseDetails = CaseDetailsBuilder.builder().id(1L).data(gaCaseData).build();
        StartEventResponse startEventResponse = StartEventResponse.builder().caseDetails(caseDetails).build();

        when(coreCaseDataService.startGaUpdate("1L", WAIT_GA_DRAFT))
            .thenReturn(startEventResponse);
        when(caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails())).thenReturn(gaCaseData);
        CaseDetails civil = CaseDetails.builder().id(123L).build();
        when(coreCaseDataService.getCase(123L)).thenReturn(civil);
        when(caseDetailsConverter.toCaseData(civil)).thenReturn(civilCaseDataNow);

        waitCivilDocUpdatedTaskHandler.execute(externalTask, externalTaskService);

    }

    @Test
    void should_handle_task_fail() {
        ExternalTaskInput externalTaskInput = ExternalTaskInput.builder().caseId("1")
                .caseEvent(WAIT_GA_DRAFT).build();
        when(mapper.convertValue(any(), eq(ExternalTaskInput.class))).thenReturn(externalTaskInput);
        CaseDetails caseDetails = CaseDetailsBuilder.builder().id(1L).data(gaCaseData).build();
        StartEventResponse startEventResponse = StartEventResponse.builder().caseDetails(caseDetails).build();

        when(coreCaseDataService.startGaUpdate("1L", WAIT_GA_DRAFT))
            .thenReturn(startEventResponse);
        when(caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails())).thenReturn(gaCaseData);
        CaseDetails civil = CaseDetails.builder().id(123L).build();
        when(coreCaseDataService.getCase(123L)).thenReturn(civil);
        when(caseDetailsConverter.toCaseData(civil)).thenReturn(civilCaseDataOld);
        WaitCivilDocUpdatedTaskHandler.maxWait = 1;
        WaitCivilDocUpdatedTaskHandler.waitGap = 1;
        waitCivilDocUpdatedTaskHandler.execute(externalTask, externalTaskService);
        WaitCivilDocUpdatedTaskHandler.maxWait = 10;
        WaitCivilDocUpdatedTaskHandler.waitGap = 6;
    }

    @Test
    void updated_should_success_ga_has_no_doc() {
        CaseData emptyCaseData = CaseData.builder().build();
        assertThat(waitCivilDocUpdatedTaskHandler.checkCivilDocUpdated(emptyCaseData)).isTrue();
    }

    @Test
    void updated_should_fail_civil_doc_is_empty() {
        when(caseDetailsConverter.toCaseData(any())).thenReturn(civilCaseDataEmpty);
        assertThat(waitCivilDocUpdatedTaskHandler.checkCivilDocUpdated(gaCaseData)).isFalse();
    }

    @Test
    void updated_should_fail_civil_doc_is_old() {
        when(caseDetailsConverter.toCaseData(any())).thenReturn(civilCaseDataOld);
        assertThat(waitCivilDocUpdatedTaskHandler.checkCivilDocUpdated(gaCaseData)).isFalse();
    }

    @Test
    void updated_should_success_civil_doc_is_updated() {
        when(caseDetailsConverter.toCaseData(any())).thenReturn(civilCaseDataNow);
        assertThat(waitCivilDocUpdatedTaskHandler.checkCivilDocUpdated(gaCaseData)).isTrue();
    }

    @Test
    void shouldCallHandleFailureMethod_whenFeignExceptionFromBusinessLogic() {
        String errorMessage = "there was an error";
        int status = 422;
        Request.HttpMethod requestType = Request.HttpMethod.POST;
        String exampleUrl = "example url";
        ExternalTaskInput externalTaskInput = ExternalTaskInput.builder().caseId("1")
                .caseEvent(WAIT_GA_DRAFT).build();
        when(mapper.convertValue(any(), eq(ExternalTaskInput.class))).thenReturn(externalTaskInput);
        CaseDetails ga = CaseDetails.builder().id(1L).build();
        StartEventResponse startEventResponse = StartEventResponse.builder().caseDetails(ga).build();

        when(coreCaseDataService.startGaUpdate("1L", WAIT_GA_DRAFT))
            .thenReturn(startEventResponse);
        when(caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails())).thenReturn(gaCaseData);
        when(mockTask.getRetries()).thenReturn(null);
        when(caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails()))
                .thenAnswer(invocation -> {
                    throw FeignException.errorStatus(errorMessage, Response.builder()
                            .request(
                                    Request.create(
                                            requestType,
                                            exampleUrl,
                                            new HashMap<>(), //this field is required for construtor//
                                            null,
                                            null,
                                            null
                                    ))
                            .status(status)
                            .build());
                });

        waitCivilDocUpdatedTaskHandler.execute(mockTask, externalTaskService);

        verify(externalTaskService, never()).complete(mockTask);
    }

    @Test
    void shouldUpdateGaDraftList_whenHandlerIsExecuted() {
        String uid1 = "f000aa01-0451-4000-b000-000000000000";
        ExternalTaskInput externalTaskInput = ExternalTaskInput.builder().caseId(CASE_ID)
            .caseEvent(WAIT_GA_DRAFT).build();
        when(mapper.convertValue(any(), eq(ExternalTaskInput.class))).thenReturn(externalTaskInput);
        when(gaForLipService.isGaForLip(any())).thenReturn(true);

        CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft().withNoticeDraftAppCaseData().toBuilder()
            .businessProcess(BusinessProcess.builder().status(BusinessProcessStatus.READY).build())
            .build();

        CaseData updatedCaseData =  CaseDataBuilder.builder().atStateClaimDraft().withNoticeDraftAppCaseData().toBuilder()
            .gaDraftDocument(singletonList(
            Element.<CaseDocument>builder().id(UUID.fromString(uid1))
                .value(pdfDocument).build())).build();
        CaseDetails caseDetails = CaseDetailsBuilder.builder().data(caseData).build();
        StartEventResponse startEventResponse = StartEventResponse.builder().caseDetails(caseDetails).build();

        when(coreCaseDataService.startGaUpdate(CASE_ID, WAIT_GA_DRAFT))
            .thenReturn(startEventResponse);
        when(caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails())).thenReturn(caseData);

        when(coreCaseDataService.submitGaUpdate(anyString(), any(CaseDataContent.class))).thenReturn(updatedCaseData);

        waitCivilDocUpdatedTaskHandler.execute(mockTask, externalTaskService);

        verify(coreCaseDataService).startGaUpdate(CASE_ID, WAIT_GA_DRAFT);
        verify(coreCaseDataService).submitGaUpdate(eq(CASE_ID), any(CaseDataContent.class));
    }

    public final CaseDocument pdfDocument = CaseDocument.builder()
        .createdBy("John")
        .documentName("documentName")
        .documentSize(0L)
        .documentType(DocumentType.GENERAL_APPLICATION_DRAFT)
        .createdDatetime(LocalDateTime.now())
        .documentLink(Document.builder()
                          .documentUrl("fake-url")
                          .documentFileName("file-name")
                          .documentBinaryUrl("binary-url")
                          .build())
        .build();
}
