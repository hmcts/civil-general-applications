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
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.Document;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_JUDGE_ADDITIONAL_INFO;

@Service
@RequiredArgsConstructor
public class RespondToJudgeAddlnInfoHandler extends CallbackHandler {

    private final ObjectMapper objectMapper;
    private final CaseDetailsConverter caseDetailsConverter;
    private final AssignCategoryId assignCategoryId;
    private final ParentCaseUpdateHelper parentCaseUpdateHelper;

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
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        List<Element<Document>> addAddInfoResponseList = addAddlnInfoResponse(caseData);
        assignCategoryId.assignCategoryIdToCollection(addAddInfoResponseList, Element::getValue,
                AssignCategoryId.APPLICATIONS
        );
        caseDataBuilder.gaAddlnInfoList(addAddInfoResponseList);
        if (!addAddInfoResponseList.isEmpty()) {
            caseDataBuilder.gaRespDocument(addAddInfoResponseList);
        }
        caseDataBuilder.generalAppAddlnInfoUpload(Collections.emptyList());

        CaseData updatedCaseData = caseDataBuilder.build();

        parentCaseUpdateHelper.updateParentWithGAState(
                updatedCaseData,
                updatedCaseData.getCcdState().getDisplayedValue()
        );
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(updatedCaseData.toMap(objectMapper))
            .build();
    }

    private List<Element<Document>> addAddlnInfoResponse(CaseData caseData) {
        List<Element<Document>> newAddlnInfoDocList =
            ofNullable(caseData.getGaAddlnInfoList()).orElse(newArrayList());

        newAddlnInfoDocList.addAll(caseData.getGeneralAppAddlnInfoUpload());

        return newAddlnInfoDocList;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }
}
