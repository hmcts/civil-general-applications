package uk.gov.hmcts.reform.civil.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.Document;

import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssignCategoryId {

    private final FeatureToggleService featureToggleService;

    public static final String ORDER_DOCUMENTS = "orderDocuments";

    public <T> void assignCategoryIdToCollection(List<Element<T>> documentUpload, Function<Element<T>, Document> documentExtractor, String theID) {
        if (!featureToggleService.isCaseFileViewEnabled()) {
            return;
        }

        if (documentUpload == null) {
            return;
        }
        documentUpload.forEach(document -> documentExtractor.apply(document).setCategoryID(theID));
    }

    public void assignCategoryIdToCaseDocument(CaseDocument documentUpload, String theID) {
        if (!featureToggleService.isCaseFileViewEnabled()) {
            return;
        }
        documentUpload.getDocumentLink().setCategoryID(theID);
    }

    public void assignCategoryIdToDocument(Document documentUpload, String theID) {

        if (!featureToggleService.isCaseFileViewEnabled()) {
            return;
        }
        documentUpload.setCategoryID(theID);
    }
}
