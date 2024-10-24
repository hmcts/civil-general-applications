package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocument;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentUploadException;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;
import uk.gov.hmcts.reform.civil.utils.DocUploadUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
@Service
public class UploadTranslatedDocumentService {

    private final FeatureToggleService featureToggleService;
    private final AssignCategoryId assignCategoryId;

    public CaseData.CaseDataBuilder processTranslatedDocument(CaseData caseData) {
        List<Element<TranslatedDocument>> translatedDocuments = caseData.getTranslatedDocuments();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        if (Objects.nonNull(translatedDocuments)) {
            Map<DocumentType, List<Element<CaseDocument>>>
                categorizedDocuments = processTranslatedDocuments(translatedDocuments);

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
        caseDataBuilder.translatedDocuments(null);
        return caseDataBuilder;
    }

    private Map<DocumentType, List<Element<CaseDocument>>> processTranslatedDocuments(
        List<Element<TranslatedDocument>> translatedDocuments) {
        Map<DocumentType, List<Element<CaseDocument>>> categorizedDocuments = new HashMap<>();

        for (Element<TranslatedDocument> translatedDocumentElement : translatedDocuments) {
            TranslatedDocument translatedDocument = translatedDocumentElement.getValue();
            DocumentType documentType =
                translatedDocument.getCorrespondingDocumentType(translatedDocument.getDocumentType());
            CaseDocument caseDocument = CaseDocument.toCaseDocument(
                translatedDocument.getFile(),
                documentType
            );

            categorizedDocuments.computeIfAbsent(documentType, k -> new ArrayList<>())
                .add(Element.<CaseDocument>builder().value(caseDocument).build());
        }

        return categorizedDocuments;
    }

    private List<Element<CaseDocument>> getExistingDocumentsByType(CaseData caseData, DocumentType documentType) {
        switch (documentType) {
            case REQUEST_FOR_INFORMATION:
            case SEND_APP_TO_OTHER_PARTY:
                return ofNullable(caseData.getRequestForInformationDocument()).orElse(new ArrayList<>());
            case DIRECTION_ORDER:
                return ofNullable(caseData.getDirectionOrderDocument()).orElse(new ArrayList<>());
            case GENERAL_ORDER:
                return ofNullable(caseData.getGeneralOrderDocument()).orElse(new ArrayList<>());
            case HEARING_ORDER:
                return ofNullable(caseData.getHearingOrderDocument()).orElse(new ArrayList<>());
            case DISMISSAL_ORDER:
                return ofNullable(caseData.getDismissalOrderDocument()).orElse(new ArrayList<>());
            case WRITTEN_REPRESENTATION_CONCURRENT:
                return ofNullable(caseData.getWrittenRepConcurrentDocument()).orElse(new ArrayList<>());
            case WRITTEN_REPRESENTATION_SEQUENTIAL:
                return ofNullable(caseData.getWrittenRepSequentialDocument()).orElse(new ArrayList<>());
            case GENERAL_APPLICATION_DRAFT:
                return ofNullable(caseData.getGaDraftDocument()).orElse(new ArrayList<>());
            default:
                return new ArrayList<>();
        }
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
}
