package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.citizenui.TranslatedDocument;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.utils.AssignCategoryId;
import uk.gov.hmcts.reform.civil.utils.DocUploadUtils;


import java.util.ArrayList;
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
            // Process all translated documents for multiple types
            Map<DocumentType, List<Element<CaseDocument>>>
                categorizedDocuments = processTranslatedDocuments(translatedDocuments);

            // Update case data with categorized documents
            categorizedDocuments.forEach((documentType, caseDocuments) -> {
                if (!caseDocuments.isEmpty()) {
                    // Retrieve and update the appropriate document list based on the document type
                    List<Element<CaseDocument>> existingDocuments = getExistingDocumentsByType(caseData, documentType);
                    existingDocuments.addAll(caseDocuments);

                    // Assign category IDs to the documents
                    assignCategoryId.assignCategoryIdToCollection(
                        existingDocuments,
                        document -> document.getValue().getDocumentLink(),
                        AssignCategoryId.APPLICATIONS
                    );

                    // Update the case data builder with the new documents based on type
                    updateCaseDataBuilderByType(caseData, caseDataBuilder, documentType, existingDocuments);
                }
            });
        }

        return caseDataBuilder;
    }

    // Helper method to process translated documents and categorize them by document type
    private Map<DocumentType, List<Element<CaseDocument>>> processTranslatedDocuments(
        List<Element<TranslatedDocument>> translatedDocuments) {
        Map<DocumentType, List<Element<CaseDocument>>> categorizedDocuments = new HashMap<>();

        for (Element<TranslatedDocument> translatedDocumentElement : translatedDocuments) {
            TranslatedDocument translatedDocument = translatedDocumentElement.getValue();
            CaseDocument caseDocument = CaseDocument.toCaseDocument(
                translatedDocument.getFile(),
                translatedDocument.getCorrespondingDocumentType(translatedDocument.getDocumentType())
            );

            // Add the document to the correct category based on its type
            DocumentType documentType = caseDocument.getDocumentType();
            categorizedDocuments.computeIfAbsent(documentType, k -> new ArrayList<>())
                .add(Element.<CaseDocument>builder().value(caseDocument).build());
        }

        return categorizedDocuments;
    }

    // Helper method to retrieve existing documents by type from case data
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
            default:
                return new ArrayList<>();
        }
    }

    // Helper method to update the case data builder based on document type
    private void updateCaseDataBuilderByType(CaseData caseData, CaseData.CaseDataBuilder caseDataBuilder,
                                             DocumentType documentType,
                                             List<Element<CaseDocument>> documents) {
        switch (documentType) {
            case DIRECTION_ORDER:
                caseDataBuilder.directionOrderDocument(documents);
                break;
            case DISMISSAL_ORDER:
                caseDataBuilder.dismissalOrderDocument(documents);
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
            case REQUEST_MORE_INFORMATION_APPLICANT_TRANSLATED:
            case JUDGES_DIRECTIONS_APPLICANT_TRANSLATED:
                DocUploadUtils.addToAddl(caseData, caseDataBuilder, documents, DocUploadUtils.APPLICANT, false);
                break;
            case REQUEST_MORE_INFORMATION_RESPONDENT_TRANSLATED:
            case JUDGES_DIRECTIONS_RESPONDENT_TRANSLATED:
                DocUploadUtils.addToAddl(caseData, caseDataBuilder, documents, DocUploadUtils.RESPONDENT_ONE, false);
                break;
//                caseDataBuilder.orderDocuments(documents);
//                break;
            // Add more cases for other document types
        }
    }
}

//    public CaseData.CaseDataBuilder processTranslatedDocument(CaseData caseData) {
//        List<Element<TranslatedDocument>> translatedDocuments = caseData.getTranslatedDocuments();
//        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
//        if (Objects.nonNull(translatedDocuments)) {
//            for (Element<TranslatedDocument> translateDocument : translatedDocuments) {
//                CaseDocument caseDocument = CaseDocument.toCaseDocument(
//                    translateDocument.getValue().getFile(),
//                    translateDocument.getValue()
//                        .getCorrespondingDocumentType(translateDocument.getValue().getDocumentType())
//                );
//                if (caseDocument.getDocumentType() == DocumentType.REQUEST_FOR_INFORMATION) {
//
//                    requestForInformationOrderTranslatedDocs.add(Element.builder().value(caseDocument).build());
//                }
//
//            }
//        }
//
//        if (requestForInformationOrderTranslatedDocs != null) {
//            List<Element<CaseDocument>> newRequestForInfoDocumentList =
//                ofNullable(caseData.getRequestForInformationDocument()).orElse(newArrayList());
//            newRequestForInfoDocumentList.addAll(requestForInformationOrderTranslatedDocs);
//            assignCategoryId.assignCategoryIdToCollection(
//                newRequestForInfoDocumentList,
//                document -> document.getValue().getDocumentLink(),
//                AssignCategoryId.APPLICATIONS
//            );
//
//            caseDataBuilder.requestForInformationDocument(newRequestForInfoDocumentList);
//
//        }
//        return caseDataBuilder;
//    }
//}
//newRequestForInfoDocumentList.addAll(wrapElements(caseDocument));
//
//                    assignCategoryId.assignCategoryIdToCollection(
//                        newRequestForInfoDocumentList,
//                        document -> document.getValue().getDocumentLink(),
//                        AssignCategoryId.APPLICATIONS
//                    );
//
//                    caseDataBuilder.requestForInformationDocument(newRequestForInfoDocumentList);

