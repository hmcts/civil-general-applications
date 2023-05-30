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
import uk.gov.hmcts.reform.civil.service.docmosis.applicationdraft.GeneralApplicationDraftGenerator;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GENERATE_DRAFT_DOCUMENT;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.utils.ElementUtils.wrapElements;

@Service
@RequiredArgsConstructor
public class GenerateApplicationDraftCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(GENERATE_DRAFT_DOCUMENT);
    private static final String TASK_ID = "CreatePDFDocument";
    private final ObjectMapper objectMapper;
    private final GeneralApplicationDraftGenerator gaDraftGenerator;
    private final AssignCategoryId assignCategoryId;

    @Override
    public String camundaActivityId() {
        return TASK_ID;
    }

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::createPDFdocument);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse createPDFdocument(CallbackParams callbackParams) {

        CaseData caseData = callbackParams.getCaseData();

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        CaseDocument gaDraftDocument;
        if ((caseData.getGeneralAppInformOtherParty() != null
            && NO.equals(caseData.getGeneralAppInformOtherParty().getIsWithNotice()))
            ||
            (caseData.getGeneralAppUrgencyRequirement() != null
            && YES.equals(caseData.getGeneralAppUrgencyRequirement()
                              .getGeneralAppUrgency()))) {

            gaDraftDocument = gaDraftGenerator.generate(
                caseDataBuilder.build(),
                callbackParams.getParams().get(BEARER_TOKEN).toString()
            );

            List<Element<CaseDocument>> draftApplicationList = newArrayList();

            draftApplicationList.addAll(wrapElements(gaDraftDocument));

            assignCategoryId.assignCategoryIdToCollection(draftApplicationList,
                                                          document -> document.getValue().getDocumentLink(),
                                                          AssignCategoryId.APPLICATIONS);
            caseDataBuilder.gaDraftDocument(draftApplicationList);
        }
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }
}
