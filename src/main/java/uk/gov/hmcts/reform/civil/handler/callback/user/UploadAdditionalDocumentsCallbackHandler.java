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
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.genapplication.UploadDocumentByType;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;
import uk.gov.hmcts.reform.civil.utils.ElementUtils;
import uk.gov.hmcts.reform.civil.utils.JudicialDecisionNotificationUtil;
import uk.gov.hmcts.reform.idam.client.IdamClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
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

    private final IdamClient idamClient;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::submitDocuments,
                      callbackKey(SUBMITTED), this::submittedConfirmation
        );
    }

    private CallbackResponse submitDocuments(CallbackParams callbackParams) {
        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        String userId = idamClient.getUserInfo(callbackParams.getParams().get(BEARER_TOKEN).toString()).getUid();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        if (JudicialDecisionNotificationUtil.isWithNotice(caseData) || JudicialDecisionNotificationUtil.isNonUrgent(caseData)
            || JudicialDecisionNotificationUtil.isGeneralAppConsentOrder(caseData)) {
            caseDataBuilder.isDocumentVisible(YesOrNo.YES);
        }
        if (caseData.getParentClaimantIsApplicant().equals(YesOrNo.YES)
            || (caseData.getParentClaimantIsApplicant().equals(YesOrNo.NO) && caseData.getGeneralAppApplnSolicitor().getId().equals(userId))) {
            caseDataBuilder.gaAddlDocClaimant(addAdditionalDocsToCollection(caseData, caseData.getGaAddlDocClaimant(), "Applicant"));
            addAdditionalDocToStaff(caseDataBuilder, caseData, "Applicant");
            caseDataBuilder.caseDocumentUploadDate(LocalDateTime.now());
        } else if (caseData.getIsMultiParty().equals(YesOrNo.NO)
            && (caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getId().equals(userId))
            || (caseData.getIsMultiParty().equals(YesOrNo.YES)
            && (caseData.getGeneralAppRespondentSolicitors().get(0).getValue().getId().equals(userId))))  {
            caseDataBuilder.gaAddlDocRespondentSol(addAdditionalDocsToCollection(caseData, caseData.getGaAddlDocRespondentSol(), "Respondent One"));
            addAdditionalDocToStaff(caseDataBuilder, caseData, "Respondent One");
            caseDataBuilder.caseDocumentUploadDateRes(LocalDateTime.now());
        } else if (caseData.getIsMultiParty().equals(YesOrNo.YES)
            && (caseData.getGeneralAppRespondentSolicitors().get(1).getValue().getId().equals(userId))) {
            caseDataBuilder.gaAddlDocRespondentSolTwo(addAdditionalDocsToCollection(caseData, caseData.getGaAddlDocRespondentSolTwo(), "Respondent Two"));
            addAdditionalDocToStaff(caseDataBuilder, caseData, "Respondent Two");
            caseDataBuilder.caseDocumentUploadDateRes(LocalDateTime.now());
        }

        caseDataBuilder.uploadDocument(null);
        caseDataBuilder.businessProcess(BusinessProcess.ready(UPLOAD_ADDL_DOCUMENTS)).build();
        CaseData updatedCaseData = caseDataBuilder.build();
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(updatedCaseData.toMap(objectMapper))
            .build();
    }

    private List<Element<CaseDocument>> addAdditionalDocsToCollection(CaseData caseData,
                                                List<Element<CaseDocument>> documentToBeAddedToCollection, String role) {
        List<Element<UploadDocumentByType>> addlDocumentsList = caseData.getUploadDocument();

        if (Objects.isNull(documentToBeAddedToCollection)) {
            documentToBeAddedToCollection = new ArrayList<>();
        }
        if (null == addlDocumentsList) {
            return new ArrayList<>();
        }
        return addDocument(documentToBeAddedToCollection, addlDocumentsList, role);
    }

    private List<Element<CaseDocument>> addDocument(List<Element<CaseDocument>> documentToBeAddedToCollection, List<Element<UploadDocumentByType>> addlDocumentsList, String role) {

        addlDocumentsList.forEach(uploadDocumentByTypeElement -> {
            if (null != uploadDocumentByTypeElement.getValue().getAdditionalDocument()) {
                documentToBeAddedToCollection.add(ElementUtils.element(CaseDocument.builder()
                                                          .documentLink(uploadDocumentByTypeElement.getValue().getAdditionalDocument())
                                                          .documentType(uploadDocumentByTypeElement.getValue().getDocumentType().getDisplayedValue())
                                                          .documentName(uploadDocumentByTypeElement.getValue().getAdditionalDocument().getDocumentFileName())
                                                          .createdBy(role)
                                                          .createdDatetime(LocalDateTime.now()).build()));
                assignCategoryId.assignCategoryIdToCollection(
                    documentToBeAddedToCollection,
                                                              document -> document.getValue().getDocumentLink(),
                    AssignCategoryId.APPLICATIONS);
            }
        });
        return documentToBeAddedToCollection;
    }

    private void addAdditionalDocToStaff(CaseData.CaseDataBuilder caseDataBuilder, CaseData caseData, String role) {
        List<Element<CaseDocument>> addlDocumentsForStaff = addAdditionalDocsToCollection(caseData, caseData.getGaAddlDoc(), role);
        caseDataBuilder.gaAddlDoc(addlDocumentsForStaff);
        caseDataBuilder.gaAddlDocStaff(addlDocumentsForStaff);

    }

    private CallbackResponse submittedConfirmation(CallbackParams callbackParams) {
        String body = "<br/> <br/>";
        return SubmittedCallbackResponse.builder()
            .confirmationHeader(CONFIRMATION_MESSAGE)
            .confirmationBody(body)
            .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

}
