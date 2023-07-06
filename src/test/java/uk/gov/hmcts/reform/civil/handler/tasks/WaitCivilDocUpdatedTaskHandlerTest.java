package uk.gov.hmcts.reform.civil.handler.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.WAIT_GA_DRAFT;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.TaskHandlerHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.data.ExternalTaskInput;
import uk.gov.hmcts.reform.civil.utils.ElementUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private TaskHandlerHelper taskHandlerHelper;
    @MockBean
    private ObjectMapper mapper;
    @MockBean
    private CaseDetailsConverter caseDetailsConverter;
    @Mock
    private ExternalTask mockTask;
    @Autowired
    private WaitCivilDocUpdatedTaskHandler waitCivilDocUpdatedTaskHandler;

    private CaseData gaCaseData;
    private CaseData civilCaseDataEmpty;
    private CaseData civilCaseDataOld;
    private CaseData civilCaseDataNow;

    @BeforeEach
    void init() {
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
        CaseDetails ga = CaseDetails.builder().id(1L).build();
        when(coreCaseDataService.getCase(1L)).thenReturn(ga);
        when(caseDetailsConverter.toCaseData(ga)).thenReturn(gaCaseData);
        CaseDetails civil = CaseDetails.builder().id(123L).build();
        when(coreCaseDataService.getCase(123L)).thenReturn(civil);
        when(caseDetailsConverter.toCaseData(civil)).thenReturn(civilCaseDataNow);

        waitCivilDocUpdatedTaskHandler.execute(externalTask, externalTaskService);

        verify(coreCaseDataService, times(2)).getCase(any());
    }

    @Test
    void should_handle_task_fail() {
        ExternalTaskInput externalTaskInput = ExternalTaskInput.builder().caseId("1")
                .caseEvent(WAIT_GA_DRAFT).build();
        when(mapper.convertValue(any(), eq(ExternalTaskInput.class))).thenReturn(externalTaskInput);
        CaseDetails ga = CaseDetails.builder().id(1L).build();
        when(coreCaseDataService.getCase(1L)).thenReturn(ga);
        when(caseDetailsConverter.toCaseData(ga)).thenReturn(gaCaseData);
        CaseDetails civil = CaseDetails.builder().id(123L).build();
        when(coreCaseDataService.getCase(123L)).thenReturn(civil);
        when(caseDetailsConverter.toCaseData(civil)).thenReturn(civilCaseDataOld);
        WaitCivilDocUpdatedTaskHandler.maxWait = 1;
        WaitCivilDocUpdatedTaskHandler.waitGap = 1;
        waitCivilDocUpdatedTaskHandler.execute(externalTask, externalTaskService);
        WaitCivilDocUpdatedTaskHandler.maxWait = 10;
        WaitCivilDocUpdatedTaskHandler.waitGap = 6;
        verify(coreCaseDataService, times(3)).getCase(any());
    }

    @Test
    void updated_should_fail_civil_doc_is_empty() {
        when(caseDetailsConverter.toCaseData(any())).thenReturn(civilCaseDataEmpty);
        assertThat(waitCivilDocUpdatedTaskHandler.updated(gaCaseData)).isFalse();
    }

    @Test
    void updated_should_fail_civil_doc_is_old() {
        when(caseDetailsConverter.toCaseData(any())).thenReturn(civilCaseDataOld);
        assertThat(waitCivilDocUpdatedTaskHandler.updated(gaCaseData)).isFalse();
    }

    @Test
    void updated_should_success_civil_doc_is_updated() {
        when(caseDetailsConverter.toCaseData(any())).thenReturn(civilCaseDataNow);
        assertThat(waitCivilDocUpdatedTaskHandler.updated(gaCaseData)).isTrue();
    }

    @Test
    void shouldHandleFailure() {

        ExternalTaskInput externalTaskInput = ExternalTaskInput.builder().caseId("1")
                .caseEvent(WAIT_GA_DRAFT).build();
        when(mapper.convertValue(any(), eq(ExternalTaskInput.class))).thenReturn(externalTaskInput);
        CaseDetails ga = CaseDetails.builder().id(1L).build();
        when(coreCaseDataService.getCase(1L)).thenAnswer(invocation -> {
            throw new Exception("there was an error");
        });

        waitCivilDocUpdatedTaskHandler.execute(mockTask, externalTaskService);

        verify(taskHandlerHelper, times(1)).updateEventToFailedState(mockTask, 3);
    }
}
