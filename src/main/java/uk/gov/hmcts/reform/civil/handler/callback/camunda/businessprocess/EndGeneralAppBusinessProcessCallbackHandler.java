package uk.gov.hmcts.reform.civil.handler.callback.camunda.businessprocess;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.List;
import java.util.Map;

import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.END_BUSINESS_PROCESS_GASPEC;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPDATE_CASE_WITH_GA_STATE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;
import static uk.gov.hmcts.reform.civil.utils.ApplicationNotificationUtil.isNotificationCriteriaSatisfied;
import static uk.gov.hmcts.reform.civil.utils.RespondentsResponsesUtil.isRespondentsResponesSatisfied;

@Service
@RequiredArgsConstructor
public class EndGeneralAppBusinessProcessCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(END_BUSINESS_PROCESS_GASPEC);

    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;
    private final ObjectMapper mapper;

    private static final String GENERAL_APPLICATIONS_DETAILS = "generalApplicationsDetails";

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::endGeneralApplicationBusinessProcess);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse endGeneralApplicationBusinessProcess(CallbackParams callbackParams) {
        CaseData data = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        CaseState newState = isNotificationCriteriaSatisfied(data)
                ? AWAITING_RESPONDENT_RESPONSE
                : APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
        updateParentWithGAState(data, newState.getDisplayedValue());
        return evaluateReady(callbackParams, newState);
    }

    private CallbackResponse evaluateReady(CallbackParams callbackParams,
                                           CaseState newState) {
        Map<String, Object> output = callbackParams.getRequest().getCaseDetails().getData();

        return AboutToStartOrSubmitCallbackResponse.builder()
                .state(newState.toString())
                .data(output)
                .build();
    }

    private void updateParentWithGAState(CaseData generalAppCaseData, String newState) {
        String applicationId = generalAppCaseData.getCcdCaseReference().toString();
        String parentCaseId = generalAppCaseData.getGeneralAppParentCaseLink().getCaseReference();

        StartEventResponse startEventResponse = coreCaseDataService.startUpdate(parentCaseId,
                UPDATE_CASE_WITH_GA_STATE);
        CaseData caseData = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());

        List<Element<GeneralApplicationsDetails>> generalApplications = caseData.getGeneralApplicationsDetails();
        if (!isEmpty(generalApplications)) {
            generalApplications.stream()
                    .filter(application -> applicationFilterCriteria(application, applicationId))
                    .findAny()
                    .orElseThrow(IllegalArgumentException::new)
                    .getValue().setCaseState(newState);
            coreCaseDataService.submitUpdate(parentCaseId, coreCaseDataService.caseDataContentFromStartEventResponse(
                    startEventResponse, getUpdatedCaseData(caseData, generalApplications)));
        }
    }

    private boolean applicationFilterCriteria(Element<GeneralApplicationsDetails> gaDetails, String applicationId) {
        return gaDetails.getValue() != null
                && gaDetails.getValue().getCaseLink() != null
                && applicationId.equals(gaDetails.getValue().getCaseLink().getCaseReference());
    }

    private Map<String, Object> getUpdatedCaseData(CaseData caseData,
                                                   List<Element<GeneralApplicationsDetails>>
                                                           generalApplicationsDetails) {
        Map<String, Object> output = caseData.toMap(mapper);
        output.put(GENERAL_APPLICATIONS_DETAILS, generalApplicationsDetails);
        return output;
    }

}
