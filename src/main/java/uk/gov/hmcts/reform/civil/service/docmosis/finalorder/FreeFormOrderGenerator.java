package uk.gov.hmcts.reform.civil.service.docmosis.finalorder;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.FreeFormOrder;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.FREE_FORM_ORDER;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FreeFormOrderGenerator implements TemplateDataGenerator<FreeFormOrder> {

    private final CaseDetailsConverter caseDetailsConverter;
    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final CoreCaseDataService coreCaseDataService;

    public CaseDocument generate(CaseData caseData, String authorisation) {

        FreeFormOrder templateData = getTemplateData(caseData);
        DocmosisTemplates template = getTemplate(caseData);
        DocmosisDocument document =
                documentGeneratorService.generateDocmosisDocument(templateData, template);
        return documentManagementService.uploadDocument(
                authorisation,
                new PDF(
                        getFileName(caseData, template),
                        document.getBytes(),
                        DocumentType.FREE_FORM_ORDER
                )
        );
    }

    @Override
    public FreeFormOrder getTemplateData(CaseData caseData) {
        CaseDetails parentCase = coreCaseDataService
                .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference()));
        CaseData parentCaseData = caseDetailsConverter.toCaseData(parentCase);

        return FreeFormOrder.builder()
                .caseNumber(getCaseNumberFormatted(caseData))
                .caseName(caseData.getCaseNameHmctsInternal())
                .receivedDate(getDateFormatted(LocalDate.now()))
                .claimantReference(getReference(parentCase, "applicantSolicitor1Reference"))
                .defendantReference(getReference(parentCase, "respondentSolicitor1Reference"))
                .build();
    }

    protected String getFileName(CaseData caseData, DocmosisTemplates template) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(template.getDocumentTitle(),
                LocalDateTime.now().format(formatter));
    }

    @SuppressWarnings("unchecked")
    protected String getReference(CaseDetails caseData, String refKey) {
        if (nonNull(caseData.getData().get("solicitorReferences"))) {
            return ((Map<String, String>) caseData.getData().get("solicitorReferences")).get(refKey);
        }
        return null;
    }

    protected String getCaseNumberFormatted(CaseData caseData) {
        String[] parts = caseData.getCcdCaseReference().toString().split("(?<=\\G.{4})");
        return String.join("-", parts);
    }

    protected String getDateFormatted(LocalDate date) {
        if (isNull(date)) {
            return null;
        }
        return DateFormatHelper.formatLocalDate(date, "dd/MMM/yyyy");
    }

    protected DocmosisTemplates getTemplate(CaseData caseData) {
        return FREE_FORM_ORDER;
    }
}
