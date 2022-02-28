package uk.gov.hmcts.reform.civil.handler.callback.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_APPLICATION;

@SuppressWarnings({"checkstyle:Indentation", "checkstyle:EmptyLineSeparator"})
@Service
@RequiredArgsConstructor
public class RespondToApplicationHandler extends CallbackHandler {

    private static final String RESPONSE_MESSAGE = "# You have responded to an application";
    private static final String JUDGES_REVIEW_MESSAGE =
        "<p> The application and your response will be reviewed by a Judge </p>";
    private static final String CONFIRMATION_SUMMARY = "<br/><p> You have responded to the following application: </p>"
        + "<ul> %s </ul>"
        + " %s ";
    private static final List<CaseEvent> EVENTS = Collections.singletonList(RESPOND_TO_APPLICATION);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(SUBMITTED), this::buildResponseConfirmation
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private SubmittedCallbackResponse buildResponseConfirmation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        String responseBody = null;

        boolean caseInProcessableState = caseData
            .getBusinessProcess().getStatus() == BusinessProcessStatus.STARTED
            && caseData.getBusinessProcess().getProcessInstanceId() != null
            && caseData.getBusinessProcess().getActivityId() != null;

        if (caseInProcessableState) {
            responseBody = buildConfirmationSummary(caseData);
        }

        return SubmittedCallbackResponse.builder()
            .confirmationHeader(RESPONSE_MESSAGE)
            .confirmationBody(responseBody)
            .build();
    }


    private String buildConfirmationSummary(CaseData caseData) {
        var genAppTypes = caseData.getGeneralAppType().getTypes();
        String appType = genAppTypes.stream().map(type -> "<li>" + type.getDisplayedValue() + "</li>")
            .collect(Collectors.joining());

        return format(
            CONFIRMATION_SUMMARY, appType, JUDGES_REVIEW_MESSAGE
        );
    }
}
