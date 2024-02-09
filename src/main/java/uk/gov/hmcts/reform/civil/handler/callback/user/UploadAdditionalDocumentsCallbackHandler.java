package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.genapplication.UploadDocumentByType;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;
import uk.gov.hmcts.reform.civil.utils.ElementUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPLOAD_ADDL_DOCUMENTS;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadAdditionalDocumentsCallbackHandler extends CallbackHandler {
    private static final String CONFIRMATION_MESSAGE = "### File has been uploaded successfully.";
    private static final List<CaseEvent> EVENTS = Collections.singletonList(UPLOAD_ADDL_DOCUMENTS);
    private final ObjectMapper objectMapper;
    private final AssignCategoryId assignCategoryId;
    private final CaseDetailsConverter caseDetailsConverter;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::submitDocuments,
                      callbackKey(SUBMITTED), this::submittedConfirmation
        );
    }

    private CallbackResponse submitDocuments(CallbackParams callbackParams) {
        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        addAdditionalDoc(caseDataBuilder, caseData);
        CaseData updatedCaseData = caseDataBuilder.build();
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(updatedCaseData.toMap(objectMapper))
            .build();
    }

    private void addAdditionalDoc(CaseData.CaseDataBuilder caseDataBuilder, CaseData caseData) {
        List<Element<UploadDocumentByType>> additionalDocuments = caseData.getUploadDocument();
        List<Element<CaseDocument>> newList = caseData.getGaAddlDoc();
        if (Objects.isNull(newList)) {
            newList = new ArrayList<>();
        }
        if (null == additionalDocuments) {
            return;
        }
        List<Element<CaseDocument>> finalNewList = newList;
        additionalDocuments.forEach(uploadDocumentByTypeElement -> {
            if (null != uploadDocumentByTypeElement.getValue().getAdditionalDocument()) {
                finalNewList.add(ElementUtils.element(CaseDocument.builder()
                                                     .documentLink(uploadDocumentByTypeElement.getValue().getAdditionalDocument())
                                                          .documentType(uploadDocumentByTypeElement.getValue().getDocumentType().getDisplayedValue())
                                                     .documentName(uploadDocumentByTypeElement.getValue().getAdditionalDocument().getDocumentFileName())
                                                     .createdDatetime(LocalDateTime.now()).build()));
                assignCategoryId.assignCategoryIdToCollection(finalNewList,
                                                              document -> document.getValue().getDocumentLink(),
                                                              AssignCategoryId.APPLICATIONS);
            }
        });

        caseDataBuilder.gaAddlDoc(finalNewList);
    }

    private CallbackResponse submittedConfirmation(CallbackParams callbackParams) {
        return SubmittedCallbackResponse.builder()
            .confirmationHeader(CONFIRMATION_MESSAGE)
            .build();
    }


    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

}
