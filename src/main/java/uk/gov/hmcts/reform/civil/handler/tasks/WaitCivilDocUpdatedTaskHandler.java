package uk.gov.hmcts.reform.civil.handler.tasks;

import static uk.gov.hmcts.reform.civil.helpers.ExponentialRetryTimeoutHelper.calculateExponentialRetryTimeout;

import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.TaskHandlerHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.data.ExternalTaskInput;

import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitCivilDocUpdatedTaskHandler implements BaseExternalTaskHandler {

    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final TaskHandlerHelper taskHandlerHelper;
    private final ObjectMapper mapper;

    @Override
    public void handleTask(ExternalTask externalTask) throws Exception {
        ExternalTaskInput externalTaskInput = mapper.convertValue(externalTask.getAllVariables(),
                ExternalTaskInput.class);
        String caseId = externalTaskInput.getCaseId();
        CaseEvent eventType = externalTaskInput.getCaseEvent();
        CaseData gaCaseData = caseDetailsConverter
                .toCaseData(coreCaseDataService.getCase(Long.valueOf(caseId)));
        if (!checkCivilDocUpdated(gaCaseData)) {
            log.error("Civil draft document update not complete, event {}", eventType.name());
            throw new Exception("Civil draft document update not complete");
        }
    }

    @Override
    public void handleFailure(ExternalTask externalTask, ExternalTaskService externalTaskServ, Exception e) {
        taskHandlerHelper.updateEventToFailedState(externalTask, getMaxAttempts());
        handleFailureToExternalTaskService(externalTask, externalTaskServ, e);
    }

    protected boolean checkCivilDocUpdated(CaseData gaCaseData) {
        if (Objects.isNull(gaCaseData.getGaDraftDocument())
                || gaCaseData.getGaDraftDocument().isEmpty()) {
            return true;
        }
        CaseData civilCaseData = caseDetailsConverter.toCaseData(
                coreCaseDataService.getCase(
                        Long.valueOf(gaCaseData.getGeneralAppParentCaseLink().getCaseReference())));
        if (Objects.nonNull(gaCaseData.getGaDraftDocument())
                && !gaCaseData.getGaDraftDocument().isEmpty()
                && Objects.nonNull(civilCaseData.getGaDraftDocStaff())
                && !civilCaseData.getGaDraftDocStaff().isEmpty()) {
            String gaDocName = gaCaseData.getGaDraftDocument().get(0).getValue().getDocumentName();
            for (Element<CaseDocument> civilDocEle : civilCaseData.getGaDraftDocStaff()) {
                if (civilDocEle.getValue().getDocumentName().equals(gaDocName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
