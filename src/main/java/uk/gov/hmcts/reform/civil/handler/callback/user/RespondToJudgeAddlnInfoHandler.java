package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.utils.DocUploadUtils;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_JUDGE_ADDITIONAL_INFO;

@Service
@RequiredArgsConstructor
public class RespondToJudgeAddlnInfoHandler extends CallbackHandler {

    private final ObjectMapper objectMapper;
    private final CaseDetailsConverter caseDetailsConverter;
    private final IdamClient idamClient;

    private static final List<CaseEvent> EVENTS = Collections.singletonList(RESPOND_TO_JUDGE_ADDITIONAL_INFO);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_START), this::emptyCallbackResponse,
                      callbackKey(ABOUT_TO_SUBMIT), this::submitClaim,
                      callbackKey(SUBMITTED), this::emptySubmittedCallbackResponse
        );
    }

    private CallbackResponse submitClaim(CallbackParams callbackParams) {

        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        String userId = idamClient.getUserInfo(callbackParams.getParams().get(BEARER_TOKEN).toString()).getUid();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        String role = DocUploadUtils.getUserRole(caseData, userId);
        if (Objects.nonNull(caseData.getGeneralAppAddlnInfoText())) {

        }
        DocUploadUtils.addDocumentToAddl(caseData, caseDataBuilder,
                caseData.getGeneralAppAddlnInfoUpload(), role, CaseEvent.RESPOND_TO_JUDGE_ADDITIONAL_INFO, false);
        caseDataBuilder.generalAppAddlnInfoUpload(Collections.emptyList());
        caseDataBuilder.businessProcess(BusinessProcess.ready(RESPOND_TO_JUDGE_ADDITIONAL_INFO)).build();
        caseDataBuilder.generalAppAddlnInfoText(null);
        CaseData updatedCaseData = caseDataBuilder.build();

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(updatedCaseData.toMap(objectMapper))
            .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }
}
