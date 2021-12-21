package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.INITIATE_GENERAL_APPLICATION;

@Service
@RequiredArgsConstructor
public class CreateGeneralApplicationHandler extends CallbackHandler {

    public static final String CONFIRMATION_SUMMARY = "<br/>Your Court will make a decision on [this application]"
        + " Summary judgement"
        + "You have marked this application as urgent"
        + " The other [ party's / [arties'] legal representative has been notified"
        + "[not been notified] that you have submitted this application.]";
    private static final List<CaseEvent> EVENTS = Collections.singletonList(INITIATE_GENERAL_APPLICATION);
    private final ObjectMapper objectMapper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START), this::emptyCallbackResponse,
            callbackKey(ABOUT_TO_SUBMIT), this::submitClaim,
            callbackKey(SUBMITTED), this::buildConfirmation
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CaseData.CaseDataBuilder getSharedData(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        // second idam call is workaround for null pointer when hiding field in getIdamEmail callback
        CaseData.CaseDataBuilder dataBuilder = caseData.toBuilder();

        dataBuilder.businessProcess(BusinessProcess.ready(INITIATE_GENERAL_APPLICATION));

        return dataBuilder;
    }

    private CallbackResponse submitClaim(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        // second idam call is workaround for null pointer when hiding field in getIdamEmail callback
        CaseData.CaseDataBuilder dataBuilder = getSharedData(callbackParams);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(dataBuilder.build().toMap(objectMapper))
            .build();
    }

    private SubmittedCallbackResponse buildConfirmation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();

        String body = format(
            CONFIRMATION_SUMMARY);

        return SubmittedCallbackResponse.builder()
            .confirmationHeader(String.format(
                "You have made an application"
            ))
            .confirmationBody(body)
            .build();
    }

    /**
     * To be used to return empty callback response, will be used in overriding classes.
     *
     * @param callbackParams This parameter is required as this is passed as reference for execute method in CallBack
     * @return empty callback response
     */
    protected CallbackResponse emptyCallbackResponse(CallbackParams callbackParams) {
        return AboutToStartOrSubmitCallbackResponse.builder().build();
    }

    /**
     * Returns empty submitted callback response. Used by events that set business process to ready, but doesn't have
     * any submitted callback logic (making callback is still required to trigger EventEmitterAspect)
     *
     * @param callbackParams This parameter is required as this is passed as reference for execute method in CallBack
     * @return empty submitted callback response
     */
    protected CallbackResponse emptySubmittedCallbackResponse(CallbackParams callbackParams) {
        return SubmittedCallbackResponse.builder().build();
    }
}
