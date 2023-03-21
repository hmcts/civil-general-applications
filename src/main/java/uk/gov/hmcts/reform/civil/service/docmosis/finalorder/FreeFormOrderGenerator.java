package uk.gov.hmcts.reform.civil.service.docmosis.finalorder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.civil.enums.dq.OrderOnCourts.ORDER_ON_COURT_INITIATIVE;
import static uk.gov.hmcts.reform.civil.enums.dq.OrderOnCourts.ORDER_WITHOUT_NOTICE;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.FREE_FORM_ORDER;

@Service
@RequiredArgsConstructor
public class FreeFormOrderGenerator implements TemplateDataGenerator<FreeFormOrder> {
    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final CoreCaseDataService coreCaseDataService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(" d MMMM yyyy");
    private static final String FILE_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String ON_COURTS_OWN = "This order is made on court’s own initiative.\n\n";
    private static final String WITHOUT_NOTICE = "This order is made without notice.\n\n";

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

        return FreeFormOrder.builder()
                .caseNumber(getCaseNumberFormatted(caseData))
                .caseName(caseData.getCaseNameHmctsInternal())
                .receivedDate(getDateFormatted(LocalDate.now()))
                .claimantReference(getReference(parentCase, "applicantSolicitor1Reference"))
                .defendantReference(getReference(parentCase, "respondentSolicitor1Reference"))
                .freeFormRecitalText(caseData.getFreeFormRecitalText())
                .freeFormRecordedText(caseData.getFreeFormRecordedText())
                .freeFormOrderedText(caseData.getFreeFormOrderedText())
                .freeFormOrderValue(getFreeFormOrderValue(caseData))
                .build();
    }

    private String getFreeFormOrderValue(CaseData caseData) {
        StringBuilder orderValueBuilder = new StringBuilder();
        if (caseData.getOrderOnCourtsList().equals(ORDER_ON_COURT_INITIATIVE)) {
            orderValueBuilder.append(ON_COURTS_OWN);
            orderValueBuilder.append(caseData
                    .getOrderOnCourtInitiative().getOnInitiativeSelectionTextArea());
            orderValueBuilder.append(DATE_FORMATTER.format(caseData
                    .getOrderOnCourtInitiative().getOnInitiativeSelectionDate()));
        } else if (caseData.getOrderOnCourtsList().equals(ORDER_WITHOUT_NOTICE)) {
            orderValueBuilder.append(WITHOUT_NOTICE);
            orderValueBuilder.append(caseData
                    .getOrderWithoutNotice().getWithoutNoticeSelectionTextArea());
            orderValueBuilder.append(DATE_FORMATTER.format(caseData
                    .getOrderWithoutNotice().getWithoutNoticeSelectionDate()));
        }
        return orderValueBuilder.toString();
    }

    protected String getFileName(CaseData caseData, DocmosisTemplates template) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FILE_TIMESTAMP_FORMAT);
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
