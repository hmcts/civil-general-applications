package uk.gov.hmcts.reform.civil.handler.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.TaskHandlerHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.GeneralAppParentCaseLink;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.utils.ElementUtils;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private CaseDetailsConverter caseDetailsConverter;
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
}
