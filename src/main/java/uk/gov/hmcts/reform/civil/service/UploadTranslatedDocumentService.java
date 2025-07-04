package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocument;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentUploadException;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;
import uk.gov.hmcts.reform.civil.utils.DocUploadUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.UPLOAD_TRANSLATED_DOCUMENT_GA_LIP;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.APPLICATION_SUMMARY_DOCUMENT_RESPONDED;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.APPLICATION_SUMMARY_DOCUMENT;

@RequiredArgsConstructor
@Service
public class UploadTranslatedDocumentService {

    private final AssignCategoryId assignCategoryId;

    public CaseData.CaseDataBuilder processTranslatedDocument(CaseData caseData, String translator) {
        List<Element<TranslatedDocument>> translatedDocuments = caseData.getTranslatedDocuments();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        if (Objects.nonNull(translatedDocuments)) {
            Map<DocumentType, List<Element<CaseDocument>>>
                categorizedDocuments = processTranslatedDocuments(translatedDocuments, translator);

            categorizedDocuments.forEach((documentType, caseDocuments) -> {
                if (!caseDocuments.isEmpty()) {
                    List<Element<CaseDocument>> existingDocuments = getExistingDocumentsByType(caseData, documentType);
                    existingDocuments.addAll(caseDocuments);
                    assignCategoryId.assignCategoryIdToCollection(
                        existingDocuments,
                        document -> document.getValue().getDocumentLink(),
                        AssignCategoryId.APPLICATIONS
                    );
                    updateCaseDataBuilderByType(caseData, caseDataBuilder, documentType, existingDocuments);
                }
            });
        }
        // Copy to different field so XUI data can be cleared
        caseDataBuilder.translatedDocumentsBulkPrint(caseData.getTranslatedDocuments());
        caseDataBuilder.translatedDocuments(null);
        return caseDataBuilder;
    }

    private Map<DocumentType, List<Element<CaseDocument>>> processTranslatedDocuments(
        List<Element<TranslatedDocument>> translatedDocuments, String translator) {
        Map<DocumentType, List<Element<CaseDocument>>> categorizedDocuments = new HashMap<>();

        for (Element<TranslatedDocument> translatedDocumentElement : translatedDocuments) {
            TranslatedDocument translatedDocument = translatedDocumentElement.getValue();
            DocumentType documentType =
                translatedDocument.getCorrespondingDocumentType(translatedDocument.getDocumentType());
            CaseDocument caseDocument = CaseDocument.toCaseDocument(
                translatedDocument.getFile(),
                documentType,
                translator
            );

            categorizedDocuments.computeIfAbsent(documentType, k -> new ArrayList<>())
                .add(Element.<CaseDocument>builder().value(caseDocument).build());
        }

        return categorizedDocuments;
    }

    private List<Element<CaseDocument>> getExistingDocumentsByType(CaseData caseData, DocumentType documentType) {
        return switch (documentType) {
            case REQUEST_FOR_INFORMATION, SEND_APP_TO_OTHER_PARTY ->
                ofNullable(caseData.getRequestForInformationDocument()).orElse(new ArrayList<>());
            case DIRECTION_ORDER -> ofNullable(caseData.getDirectionOrderDocument()).orElse(new ArrayList<>());
            case GENERAL_ORDER -> ofNullable(caseData.getGeneralOrderDocument()).orElse(new ArrayList<>());
            case HEARING_ORDER -> ofNullable(caseData.getHearingOrderDocument()).orElse(new ArrayList<>());
            case HEARING_NOTICE -> ofNullable(caseData.getHearingNoticeDocument()).orElse(new ArrayList<>());
            case DISMISSAL_ORDER -> ofNullable(caseData.getDismissalOrderDocument()).orElse(new ArrayList<>());
            case WRITTEN_REPRESENTATION_CONCURRENT ->
                ofNullable(caseData.getWrittenRepConcurrentDocument()).orElse(new ArrayList<>());
            case WRITTEN_REPRESENTATION_SEQUENTIAL ->
                ofNullable(caseData.getWrittenRepSequentialDocument()).orElse(new ArrayList<>());
            case GENERAL_APPLICATION_DRAFT -> ofNullable(caseData.getGaDraftDocument()).orElse(new ArrayList<>());
            default -> new ArrayList<>();
        };
    }

    private void updateCaseDataBuilderByType(CaseData caseData, CaseData.CaseDataBuilder caseDataBuilder,
                                             DocumentType documentType,
                                             List<Element<CaseDocument>> documents) {
        switch (documentType) {
            case DIRECTION_ORDER:
                caseDataBuilder.directionOrderDocument(documents);
                break;
            case DISMISSAL_ORDER:
                caseDataBuilder.dismissalOrderDocument(documents);
                break;
            case REQUEST_FOR_INFORMATION:
            case SEND_APP_TO_OTHER_PARTY:
                caseDataBuilder.requestForInformationDocument(documents);
                break;
            case GENERAL_ORDER:
                caseDataBuilder.generalOrderDocument(documents);
                break;
            case HEARING_ORDER:
                caseDataBuilder.hearingOrderDocument(documents);
                break;
            case HEARING_NOTICE:
                caseDataBuilder.hearingNoticeDocument(documents);
                break;
            case WRITTEN_REPRESENTATION_CONCURRENT:
                caseDataBuilder.writtenRepConcurrentDocument(documents);
                break;
            case WRITTEN_REPRESENTATION_SEQUENTIAL:
                caseDataBuilder.writtenRepSequentialDocument(documents);
                break;
            case GENERAL_APPLICATION_DRAFT:
                caseDataBuilder.gaDraftDocument(documents);
                break;
            case REQUEST_MORE_INFORMATION_APPLICANT_TRANSLATED:
            case JUDGES_DIRECTIONS_APPLICANT_TRANSLATED:
            case WRITTEN_REPRESENTATION_APPLICANT_TRANSLATED:
            case UPLOADED_DOCUMENT_APPLICANT:
                DocUploadUtils.addToAddl(caseData, caseDataBuilder, documents, DocUploadUtils.APPLICANT, false);
                break;
            case REQUEST_MORE_INFORMATION_RESPONDENT_TRANSLATED:
            case JUDGES_DIRECTIONS_RESPONDENT_TRANSLATED:
            case WRITTEN_REPRESENTATION_RESPONDENT_TRANSLATED:
            case UPLOADED_DOCUMENT_RESPONDENT:
                DocUploadUtils.addToAddl(caseData, caseDataBuilder, documents, DocUploadUtils.RESPONDENT_ONE, false);
                break;
            default:
                throw new DocumentUploadException("No document file type found for Translated document");
        }
    }

    public void updateGADocumentsWithOriginalDocuments(CaseData.CaseDataBuilder caseDataBuilder) {
        List<Element<TranslatedDocument>> translatedDocuments = caseDataBuilder.build().getTranslatedDocuments();
        List<Element<CaseDocument>> preTranslationGaDocuments = caseDataBuilder.build().getPreTranslationGaDocuments();
        List<Element<CaseDocument>> gaDraftDocument;
        if (Objects.isNull(caseDataBuilder.build().getGaDraftDocument())) {
            gaDraftDocument = newArrayList();
        } else {
            gaDraftDocument = caseDataBuilder.build().getGaDraftDocument();
        }

        if (Objects.nonNull(translatedDocuments)) {
            translatedDocuments.forEach(document -> {
                if (document.getValue().getDocumentType().equals(APPLICATION_SUMMARY_DOCUMENT)
                    || document.getValue().getDocumentType().equals(APPLICATION_SUMMARY_DOCUMENT_RESPONDED)) {
                    if (Objects.nonNull(preTranslationGaDocuments)) {
                        Optional<Element<CaseDocument>> preTranslationGADraftDocument = preTranslationGaDocuments.stream()
                            .filter(item -> item.getValue().getDocumentType() == DocumentType.GENERAL_APPLICATION_DRAFT)
                            .findFirst();
                        preTranslationGADraftDocument.ifPresent(preTranslationGaDocuments::remove);
                        preTranslationGADraftDocument.ifPresent(gaDraftDocument::add);
                        caseDataBuilder.gaDraftDocument(gaDraftDocument);
                    }
                }
            });
        }
    }

    public CaseEvent getBusinessProcessEvent(CaseData caseData) {
        List<Element<TranslatedDocument>> translatedDocuments = caseData.getTranslatedDocuments();

        if (Objects.nonNull(translatedDocuments)
            && translatedDocuments.get(0).getValue().getDocumentType().equals(APPLICATION_SUMMARY_DOCUMENT)) {
            return CaseEvent.UPLOAD_TRANSLATED_DOCUMENT_GA_SUMMARY_DOC;
        } else if (Objects.nonNull(translatedDocuments)
            && translatedDocuments.get(0).getValue().getDocumentType().equals(APPLICATION_SUMMARY_DOCUMENT_RESPONDED)) {
            return CaseEvent.UPLOAD_TRANSLATED_DOCUMENT_GA_SUMMARY_RESPONSE_DOC;
        }
        return UPLOAD_TRANSLATED_DOCUMENT_GA_LIP;
    }
}
