package uk.gov.hmcts.reform.civil.handler.tasks;

import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.ExternalTaskData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.GaForLipService;
import uk.gov.hmcts.reform.civil.service.data.ExternalTaskInput;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitCivilDocUpdatedTaskHandler extends BaseExternalTaskHandler {

    protected static int maxWait = 10;
    protected static int waitGap = 3;
    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final GaForLipService gaForLipService;
    private final ObjectMapper mapper;

    @Override
    public ExternalTaskData handleTask(ExternalTask externalTask) throws Exception {
        ExternalTaskInput externalTaskInput = mapper.convertValue(externalTask.getAllVariables(),
                ExternalTaskInput.class);
        String caseId = externalTaskInput.getCaseId();
        CaseEvent eventType = externalTaskInput.getCaseEvent();
        StartEventResponse startEventResponse = coreCaseDataService
            .startGaUpdate(caseId, eventType);
        CaseData gaCaseData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());

        if (!gaForLipService.isGaForLip(gaCaseData)) {
            boolean civilUpdated = checkCivilDocUpdated(gaCaseData);
            int wait = maxWait;
            log.info("Civil Doc update = {}, event {}, try {}", civilUpdated, eventType.name(), wait);
            while (!civilUpdated && wait > 0) {
                wait--;
                TimeUnit.SECONDS.sleep(waitGap);
                civilUpdated = checkCivilDocUpdated(gaCaseData);
                log.info("Civil Doc update = {}, event {}, try {}", civilUpdated, eventType.name(), wait);
            }
            if (!civilUpdated) {
                log.error("Civil draft document update wait time out");
                throw new BpmnError("ABORT");
            }
        } else {

            CaseDataContent caseDataContent = gaCaseDataContent(startEventResponse, gaCaseData);
            var caseData = coreCaseDataService.submitGaUpdate(caseId, caseDataContent);
            return ExternalTaskData.builder().caseData(caseData).build();
        }
        return ExternalTaskData.builder().build();
    }

    private Map<String, Object> getUpdatedCaseData(CaseData gaCaseData) {
        Map<String, Object> output = gaCaseData.toMap(mapper);
        try {
            if (gaForLipService.isGaForLip(gaCaseData) && (Objects.nonNull(gaCaseData.getGaDraftDocument()) && gaCaseData.getGaDraftDocument().size() > 1)) {
                gaCaseData.getGaDraftDocument().sort(Comparator.comparing(gaDocElement -> gaDocElement.getValue().getCreatedDatetime(), Comparator.reverseOrder()));
                gaCaseData.getGaDraftDocument().remove(1);
                List<Element<CaseDocument>> draftApplicationList = newArrayList();
                draftApplicationList.addAll(gaCaseData.getGaDraftDocument());
                output.put("gaDraftDocument", draftApplicationList);
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return output;
    }

    private CaseDataContent gaCaseDataContent(StartEventResponse startGaEventResponse, CaseData gaCaseData) {

        return CaseDataContent.builder()
            .eventToken(startGaEventResponse.getToken())
            .event(Event.builder().id(startGaEventResponse.getEventId())
                       .build())
            .data(getUpdatedCaseData(gaCaseData))
            .build();
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
