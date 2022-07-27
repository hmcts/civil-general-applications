package uk.gov.hmcts.reform.civil.handler.callback.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.REFER_TO_JUDGE;

@Service
@RequiredArgsConstructor
public class ReferToJudgeHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(REFER_TO_JUDGE);

    private static final String REFER_TO_LEGAL_ADVISOR_MESSAGE = "Refer to Judge";
    private static final String JUDGE_CONFIRMATION_SUMMARY = "Test refer to Judge";

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_START), this::courtValidation,
            callbackKey(ABOUT_TO_SUBMIT), this::submitReferToJudge,
            callbackKey(SUBMITTED), this::buildResponseConfirmation
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private AboutToStartOrSubmitCallbackResponse courtValidation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(courtAssignedValidation(callbackParams))
            .build();
    }

    private CallbackResponse submitReferToJudge(CallbackParams callbackParams) {

        return AboutToStartOrSubmitCallbackResponse.builder()
            .build();
    }

    private SubmittedCallbackResponse buildResponseConfirmation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();

        return SubmittedCallbackResponse.builder()
            .confirmationHeader(REFER_TO_LEGAL_ADVISOR_MESSAGE)
            .confirmationBody(buildConfirmationSummary(caseData))
            .build();
    }

    public List<String> courtAssignedValidation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        List<String> errors = new ArrayList<>();

        return errors;
    }

    private String buildConfirmationSummary(CaseData caseData) {
        return format(
            JUDGE_CONFIRMATION_SUMMARY
        );
    }
}
