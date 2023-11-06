package uk.gov.hmcts.reform.civil.service.docmosis.finalorder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.FreeFormOrder;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.civil.enums.dq.OrderOnCourts.ORDER_ON_COURT_INITIATIVE;
import static uk.gov.hmcts.reform.civil.enums.dq.OrderOnCourts.ORDER_WITHOUT_NOTICE;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.FREE_FORM_ORDER;

@Service
@RequiredArgsConstructor
public class FreeFormOrderGenerator implements TemplateDataGenerator<FreeFormOrder> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final IdamClient idamClient;
    private String judgeNameTitle;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(" d MMMM yyyy");
    private static final String FILE_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public CaseDocument generate(CaseData caseData, String authorisation) {
        UserDetails userDetails = idamClient.getUserDetails(authorisation);
        judgeNameTitle = userDetails.getFullName();

        FreeFormOrder templateData = getTemplateData(caseData);
        DocmosisTemplates template = getTemplate();
        DocmosisDocument document =
                documentGeneratorService.generateDocmosisDocument(templateData, template);
        return documentManagementService.uploadDocument(
                authorisation,
                new PDF(
                        getFileName(template),
                        document.getBytes(),
                        DocumentType.GENERAL_ORDER
                )
        );
    }

    @Override
    public FreeFormOrder getTemplateData(CaseData caseData) {

        return FreeFormOrder.builder()
            .judgeNameTitle(judgeNameTitle)
            .caseNumber(getCaseNumberFormatted(caseData))
            .caseName(caseData.getCaseNameHmctsInternal())
            .receivedDate(getDateFormatted(LocalDate.now()))
            .freeFormRecitalText(caseData.getFreeFormRecitalText())
            .freeFormOrderedText(caseData.getFreeFormOrderedText())
            .freeFormOrderValue(getFreeFormOrderValue(caseData))
            .courtName(caseData.getLocationName())
            .isMultiParty(caseData.getIsMultiParty())
            .claimant1Name(caseData.getClaimant1PartyName())
            .claimant2Name(caseData.getClaimant2PartyName() != null ? caseData.getClaimant2PartyName() : null)
            .defendant1Name(caseData.getDefendant1PartyName())
            .defendant2Name(caseData.getDefendant2PartyName() != null ? caseData.getDefendant2PartyName() : null)
            .build();
    }

    protected String getFreeFormOrderValue(CaseData caseData) {
        StringBuilder orderValueBuilder = new StringBuilder();
        if (caseData.getOrderOnCourtsList().equals(ORDER_ON_COURT_INITIATIVE)) {
            orderValueBuilder.append(caseData
                    .getOrderOnCourtInitiative().getOnInitiativeSelectionTextArea());
            orderValueBuilder.append(DATE_FORMATTER.format(caseData
                    .getOrderOnCourtInitiative().getOnInitiativeSelectionDate()));
            orderValueBuilder.append(".");
        } else if (caseData.getOrderOnCourtsList().equals(ORDER_WITHOUT_NOTICE)) {
            orderValueBuilder.append(caseData
                    .getOrderWithoutNotice().getWithoutNoticeSelectionTextArea());
            orderValueBuilder.append(DATE_FORMATTER.format(caseData
                    .getOrderWithoutNotice().getWithoutNoticeSelectionDate()));
            orderValueBuilder.append(".");
        }
        return orderValueBuilder.toString();
    }

    protected String getFileName(DocmosisTemplates template) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FILE_TIMESTAMP_FORMAT);
        return String.format(template.getDocumentTitle(),
                LocalDateTime.now().format(formatter));
    }

    protected String getCaseNumberFormatted(CaseData caseData) {
        String[] parts = caseData.getCcdCaseReference().toString().split("(?<=\\G.{4})");
        return String.join("-", parts);
    }

    protected String getDateFormatted(LocalDate date) {
        if (isNull(date)) {
            return null;
        }
        return DateFormatHelper.formatLocalDate(date, " d MMMM yyyy");
    }

    protected DocmosisTemplates getTemplate() {
        return FREE_FORM_ORDER;
    }
}
