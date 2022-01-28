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
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplication;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CREATE_GENERAL_APPLICATION_CASE;

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
