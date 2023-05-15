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
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.RESPOND_TO_JUDGE_DIRECTIONS;

@Service
@RequiredArgsConstructor
public class ResponseToJudgeDirectionsOrder extends CallbackHandler {

    private final ObjectMapper objectMapper;
    private final CaseDetailsConverter caseDetailsConverter;
    private final AssignCategoryId assignCategoryId;
    private final ParentCaseUpdateHelper parentCaseUpdateHelper;

    private static final List<CaseEvent> EVENTS = Collections.singletonList(RESPOND_TO_JUDGE_DIRECTIONS);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_START), this::emptyCallbackResponse,
                      callbackKey(ABOUT_TO_SUBMIT), this::submitClaim,
                      callbackKey(SUBMITTED), this::emptySubmittedCallbackResponse
        );
    }

    protected CallbackResponse submitClaim(CallbackParams callbackParams) {

        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        List<Element<Document>> toBeAdded = addDirectionsOrderResponse(caseData);
        assignCategoryId.assignCategoryIdToCollection(toBeAdded, Element::getValue,
                AssignCategoryId.APPLICATIONS
        );
        caseDataBuilder.gaDirectionDocList(toBeAdded);
        if (!toBeAdded.isEmpty()) {
            List<Element<Document>> updatedGaRespDoc =
                    ofNullable(caseData.getGaRespDocument()).orElse(newArrayList());
            updatedGaRespDoc.addAll(toBeAdded);
            caseDataBuilder.gaRespDocument(updatedGaRespDoc);
        }
        caseDataBuilder.generalAppDirOrderUpload(Collections.emptyList());

        CaseData updatedCaseData = caseDataBuilder.build();

        parentCaseUpdateHelper.updateParentWithGAState(
                updatedCaseData,
                updatedCaseData.getCcdState().getDisplayedValue()
        );
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(updatedCaseData.toMap(objectMapper))
            .build();
    }

    private List<Element<Document>> addDirectionsOrderResponse(CaseData caseData) {
        List<Element<Document>> newDirectionOrderDocList =
            ofNullable(caseData.getGaDirectionDocList()).orElse(newArrayList());

        newDirectionOrderDocList.addAll(caseData.getGeneralAppDirOrderUpload());

        return newDirectionOrderDocList;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }
}
