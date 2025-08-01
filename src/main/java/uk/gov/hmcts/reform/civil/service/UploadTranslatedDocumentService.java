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

import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.APPLICATION_SUMMARY_DOCUMENT;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.APPLICATION_SUMMARY_DOCUMENT_RESPONDED;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.APPROVE_OR_EDIT_ORDER;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.GENERAL_ORDER;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.HEARING_NOTICE;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.WRITTEN_REPRESENTATIONS_ORDER_CONCURRENT;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.WRITTEN_REPRESENTATIONS_ORDER_SEQUENTIAL;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.REQUEST_FOR_MORE_INFORMATION_ORDER;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.HEARING_ORDER;
import static uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocumentType.DISMISSAL_ORDER;

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
        List<Element<CaseDocument>> bulkPrintOriginalDocuments = newArrayList();
        List<Element<TranslatedDocument>> translatedDocuments = caseDataBuilder.build().getTranslatedDocuments();
        List<Element<CaseDocument>> preTranslationGaDocuments = caseDataBuilder.build().getPreTranslationGaDocuments();
        List<Element<CaseDocument>> gaDraftDocument;
        if (Objects.isNull(caseDataBuilder.build().getGaDraftDocument())) {
            gaDraftDocument = newArrayList();
        } else {
            gaDraftDocument = caseDataBuilder.build().getGaDraftDocument();
        }

        List<Element<CaseDocument>> generalOrderDocs = Objects.isNull(caseDataBuilder.build().getGeneralOrderDocument())
            ? newArrayList() : caseDataBuilder.build().getGeneralOrderDocument();

        List<Element<CaseDocument>> writtenRepsSequentialDocs = Objects.isNull(caseDataBuilder.build().getWrittenRepSequentialDocument())
            ? newArrayList() : caseDataBuilder.build().getWrittenRepSequentialDocument();
        List<Element<CaseDocument>> writtenRepsConcurrentDocs = Objects.isNull(caseDataBuilder.build().getWrittenRepConcurrentDocument())
            ? newArrayList() : caseDataBuilder.build().getWrittenRepConcurrentDocument();

        List<Element<CaseDocument>> hearingNoticeDocs = Objects.isNull(caseDataBuilder.build().getHearingNoticeDocument())
            ? newArrayList() : caseDataBuilder.build().getHearingNoticeDocument();
        List<Element<CaseDocument>> requestMoreInformationDocs = Objects.isNull(caseDataBuilder.build().getRequestForInformationDocument())
            ? newArrayList() : caseDataBuilder.build().getRequestForInformationDocument();
        List<Element<CaseDocument>> dismissalOrderDocs = Objects.isNull(caseDataBuilder.build().getDismissalOrderDocument())
            ? newArrayList() : caseDataBuilder.build().getDismissalOrderDocument();
        List<Element<CaseDocument>> hearingOrders = Objects.isNull(caseDataBuilder.build().getHearingOrderDocument())
            ? newArrayList() : caseDataBuilder.build().getHearingOrderDocument();

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
                } else if (document.getValue().getDocumentType().equals(WRITTEN_REPRESENTATIONS_ORDER_SEQUENTIAL)) {
                    Optional<Element<CaseDocument>> preTranslationWrittenRepsSequential = preTranslationGaDocuments.stream()
                        .filter(item -> item.getValue().getDocumentType() == DocumentType.WRITTEN_REPRESENTATION_SEQUENTIAL)
                        .findFirst();
                    preTranslationWrittenRepsSequential.ifPresent(preTranslationGaDocuments::remove);
                    preTranslationWrittenRepsSequential.ifPresent(writtenRepsSequentialDocs::add);
                    preTranslationWrittenRepsSequential.ifPresent(bulkPrintOriginalDocuments::add);
                    caseDataBuilder.writtenRepSequentialDocument(writtenRepsSequentialDocs);
                    caseDataBuilder.originalDocumentsBulkPrint(bulkPrintOriginalDocuments);
                } else if (document.getValue().getDocumentType().equals(WRITTEN_REPRESENTATIONS_ORDER_CONCURRENT)) {
                    Optional<Element<CaseDocument>> preTranslationWrittenRepsConcurrent = preTranslationGaDocuments.stream()
                        .filter(item -> item.getValue().getDocumentType() == DocumentType.WRITTEN_REPRESENTATION_CONCURRENT)
                        .findFirst();
                    preTranslationWrittenRepsConcurrent.ifPresent(preTranslationGaDocuments::remove);
                    preTranslationWrittenRepsConcurrent.ifPresent(writtenRepsConcurrentDocs::add);
                    preTranslationWrittenRepsConcurrent.ifPresent(bulkPrintOriginalDocuments::add);
                    caseDataBuilder.writtenRepConcurrentDocument(writtenRepsConcurrentDocs);
                    caseDataBuilder.originalDocumentsBulkPrint(bulkPrintOriginalDocuments);
                } else if (document.getValue().getDocumentType().equals(GENERAL_ORDER)
                    || document.getValue().getDocumentType().equals(APPROVE_OR_EDIT_ORDER)) {
                    Optional<Element<CaseDocument>> preTranslationGeneralOrder = preTranslationGaDocuments.stream()
                        .filter(item -> item.getValue().getDocumentType() == DocumentType.GENERAL_ORDER)
                        .findFirst();
                    preTranslationGeneralOrder.ifPresent(preTranslationGaDocuments::remove);
                    preTranslationGeneralOrder.ifPresent(generalOrderDocs::add);
                    preTranslationGeneralOrder.ifPresent(bulkPrintOriginalDocuments::add);
                    caseDataBuilder.generalOrderDocument(generalOrderDocs);
                    caseDataBuilder.originalDocumentsBulkPrint(bulkPrintOriginalDocuments);
                } else if (document.getValue().getDocumentType().equals(HEARING_NOTICE)) {
                    Optional<Element<CaseDocument>> preTranslationHearingNotice = preTranslationGaDocuments.stream()
                        .filter(item -> item.getValue().getDocumentType() == DocumentType.HEARING_NOTICE)
                        .findFirst();
                    preTranslationHearingNotice.ifPresent(preTranslationGaDocuments::remove);
                    preTranslationHearingNotice.ifPresent(hearingNoticeDocs::add);
                    preTranslationHearingNotice.ifPresent(bulkPrintOriginalDocuments::add);
                    caseDataBuilder.hearingNoticeDocument(hearingNoticeDocs);
                    caseDataBuilder.originalDocumentsBulkPrint(bulkPrintOriginalDocuments);
                } else if (document.getValue().getDocumentType().equals(REQUEST_FOR_MORE_INFORMATION_ORDER)) {
                    Optional<Element<CaseDocument>> preTranslationRequestMoreInformation = preTranslationGaDocuments.stream()
                        .filter(item -> item.getValue().getDocumentType() == DocumentType.REQUEST_FOR_INFORMATION)
                        .findFirst();
                    preTranslationRequestMoreInformation.ifPresent(preTranslationGaDocuments::remove);
                    preTranslationRequestMoreInformation.ifPresent(requestMoreInformationDocs::add);
                    preTranslationRequestMoreInformation.ifPresent(bulkPrintOriginalDocuments::add);
                    caseDataBuilder.requestForInformationDocument(requestMoreInformationDocs);
                    caseDataBuilder.originalDocumentsBulkPrint(bulkPrintOriginalDocuments);
                } else if (document.getValue().getDocumentType().equals(HEARING_ORDER)) {
                    Optional<Element<CaseDocument>> preTranslationHearingOrder = preTranslationGaDocuments.stream()
                        .filter(item -> item.getValue().getDocumentType() == DocumentType.HEARING_ORDER)
                        .findFirst();
                    preTranslationHearingOrder.ifPresent(preTranslationGaDocuments::remove);
                    preTranslationHearingOrder.ifPresent(hearingOrders::add);
                    preTranslationHearingOrder.ifPresent(bulkPrintOriginalDocuments::add);
                    caseDataBuilder.hearingOrderDocument(hearingOrders);
                } else if (document.getValue().getDocumentType().equals(DISMISSAL_ORDER)) {
                    Optional<Element<CaseDocument>> preTranslationDismissalOrder = preTranslationGaDocuments.stream()
                        .filter(item -> item.getValue().getDocumentType() == DocumentType.DISMISSAL_ORDER)
                        .findFirst();
                    preTranslationDismissalOrder.ifPresent(preTranslationGaDocuments::remove);
                    preTranslationDismissalOrder.ifPresent(dismissalOrderDocs::add);
                    preTranslationDismissalOrder.ifPresent(bulkPrintOriginalDocuments::add);
                    caseDataBuilder.dismissalOrderDocument(dismissalOrderDocs);
                    caseDataBuilder.originalDocumentsBulkPrint(bulkPrintOriginalDocuments);
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
        } else if (Objects.nonNull(translatedDocuments)
            && (translatedDocuments.get(0).getValue().getDocumentType().equals(WRITTEN_REPRESENTATIONS_ORDER_SEQUENTIAL)
            || translatedDocuments.get(0).getValue().getDocumentType().equals(WRITTEN_REPRESENTATIONS_ORDER_CONCURRENT))
            || translatedDocuments.get(0).getValue().getDocumentType().equals(REQUEST_FOR_MORE_INFORMATION_ORDER)
            || translatedDocuments.get(0).getValue().getDocumentType().equals(DISMISSAL_ORDER)
            || ((translatedDocuments.get(0).getValue().getDocumentType().equals(GENERAL_ORDER)
            || translatedDocuments.get(0).getValue().getDocumentType().equals(APPROVE_OR_EDIT_ORDER)) && Objects.isNull(caseData.getFinalOrderSelection())
            || translatedDocuments.get(0).getValue().getDocumentType().equals(HEARING_ORDER))) {
            return CaseEvent.UPLOAD_TRANSLATED_DOCUMENT_JUDGE_DECISION;
        } else if (Objects.nonNull(translatedDocuments)
            && (translatedDocuments.get(0).getValue().getDocumentType().equals(HEARING_NOTICE))) {
            return CaseEvent.UPLOAD_TRANSLATED_DOCUMENT_HEARING_SCHEDULED;
        }
        return UPLOAD_TRANSLATED_DOCUMENT_GA_LIP;
    }
}
