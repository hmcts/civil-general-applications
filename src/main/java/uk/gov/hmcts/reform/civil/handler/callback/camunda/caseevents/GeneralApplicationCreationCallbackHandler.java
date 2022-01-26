package uk.gov.hmcts.reform.civil.handler.callback.camunda.caseevents;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CREATE_GENERAL_APPLICATION_CASE;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_GA_CASE_DATA;

@Service
@RequiredArgsConstructor
public class GeneralApplicationCreationCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(CREATE_GENERAL_APPLICATION_CASE);
    private static final String TASK_ID = "CreateGeneralApplicationCase";
    private final CoreCaseDataService coreCaseDataService;
    private final ObjectMapper objectMapper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::createGeneralApplication
        );
    }

    @Override
    public String camundaActivityId(CallbackParams callbackParams) {
        return TASK_ID;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    public CallbackResponse createGeneralApplication(CallbackParams callbackParams) {

        CaseData caseData = callbackParams.getCaseData();
        List<Element<GeneralApplication>> generalApplications = caseData.getGeneralApplications();
        if (generalApplications != null) {
            var genApps = generalApplications.stream()
                .filter(app -> app.getValue() != null && app.getValue().getBusinessProcess() != null
                    && app.getValue().getBusinessProcess().getStatus() == BusinessProcessStatus.STARTED
                    && app.getValue().getBusinessProcess().getProcessInstanceId() != null).findFirst();
            if (genApps.isPresent()) {
                GeneralApplication generalApplication = genApps.get().getValue();
                coreCaseDataService.createGeneralAppCase(
                    generalApplication.toMap(objectMapper)
                );
                generalApplication.getBusinessProcess().setStatus(BusinessProcessStatus.FINISHED);
                coreCaseDataService.triggerEvent(
                    caseData.getCcdCaseReference(),
                    UPDATE_GA_CASE_DATA,
                    getUpdatedCaseData(callbackParams, generalApplications)
                );
            }
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
            .build();
    }

    private Map<String, Object> getUpdatedCaseData(CallbackParams callbackParams,
                                           List<Element<GeneralApplication>> generalApplications) {
        Map<String, Object> output = callbackParams.getRequest().getCaseDetails().getData();
        output.put("generalApplications", generalApplications);

        return output;
    }
}
