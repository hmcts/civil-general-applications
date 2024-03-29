package uk.gov.hmcts.reform.civil.handler.callback.camunda.docmosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.service.docmosis.hearingorder.HearingFormGenerator;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GENERATE_HEARING_NOTICE_DOCUMENT;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@Service
@RequiredArgsConstructor
public class GenerateHearingNoticeDocumentCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(GENERATE_HEARING_NOTICE_DOCUMENT);
    private static final String TASK_ID = "GenerateHearingNoticeDocument";
    private final HearingFormGenerator hearingFormGenerator;
    private final ObjectMapper objectMapper;
    private final AssignCategoryId assignCategoryId;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::generateHearingNoticeDocument);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    @Override
    public String camundaActivityId() {
        return TASK_ID;
    }

    private CallbackResponse generateHearingNoticeDocument(CallbackParams callbackParams) {

        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        buildDocument(callbackParams, caseDataBuilder, caseData);
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    private void buildDocument(CallbackParams callbackParams, CaseData.CaseDataBuilder caseDataBuilder,
                               CaseData caseData) {
        List<Element<CaseDocument>> documents = ofNullable(caseData.getHearingNoticeDocument())
                .orElse(newArrayList());
        documents.addAll(wrapElements(hearingFormGenerator.generate(
                callbackParams.getCaseData(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
        )));

        assignCategoryId.assignCategoryIdToCollection(documents, document -> document.getValue().getDocumentLink(),
                                                      AssignCategoryId.APPLICATIONS
        );

        caseDataBuilder.hearingNoticeDocument(documents);
    }
}
