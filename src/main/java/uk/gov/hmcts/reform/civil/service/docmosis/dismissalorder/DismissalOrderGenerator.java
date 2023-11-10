package uk.gov.hmcts.reform.civil.service.docmosis.dismissalorder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.ListGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.DISMISSAL_ORDER;

@Service
@RequiredArgsConstructor
public class DismissalOrderGenerator implements TemplateDataGenerator<JudgeDecisionPdfDocument> {

    private final DocumentManagementService docManagementService;
    private final ListGeneratorService listGeneratorService;
    private final IdamClient idamClient;
    private final DocumentGeneratorService docGeneratorService;
    private final DocmosisService docmosisService;
    private String judgeNameTitle;

    public CaseDocument generate(CaseData caseData, String authorisation) {
        UserDetails userDetails = idamClient.getUserDetails(authorisation);
        judgeNameTitle = userDetails.getFullName();

        JudgeDecisionPdfDocument templateData = getTemplateData(caseData);

        DocmosisTemplates docmosisTemplate = getDocmosisTemplate();

        DocmosisDocument docmosisDocument = docGeneratorService.generateDocmosisDocument(
            templateData,
            docmosisTemplate
        );

        return docManagementService.uploadDocument(
            authorisation,
            new PDF(getFileName(docmosisTemplate), docmosisDocument.getBytes(),
                    DocumentType.DISMISSAL_ORDER)
        );
    }

    private String getFileName(DocmosisTemplates docmosisTemplate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return String.format(docmosisTemplate.getDocumentTitle(), LocalDateTime.now().format(formatter));
    }

    @Override
    public JudgeDecisionPdfDocument getTemplateData(CaseData caseData) {
        String claimantName = listGeneratorService.claimantsName(caseData);

        String defendantName = listGeneratorService.defendantsName(caseData);

        JudgeDecisionPdfDocument.JudgeDecisionPdfDocumentBuilder judgeDecisionPdfDocumentBuilder =
            JudgeDecisionPdfDocument.builder()
                .judgeNameTitle(judgeNameTitle)
                .claimNumber(caseData.getCcdCaseReference().toString())
                .claimantName(claimantName)
                .defendantName(defendantName)
                .courtName(caseData.getLocationName())
                .siteName(caseData.getCaseManagementLocation().getSiteName())
                .address(caseData.getCaseManagementLocation().getAddress())
                .postcode(caseData.getCaseManagementLocation().getPostcode())
                .judgeRecital(caseData.getJudicialDecisionMakeOrder().getJudgeRecitalText())
                .dismissalOrder(caseData.getJudicialDecisionMakeOrder().getDismissalOrderText())
                .submittedOn(LocalDate.now())
                .reasonAvailable(docmosisService.reasonAvailable(caseData))
                .reasonForDecision(docmosisService.populateJudgeReason(caseData))
                .judicialByCourtsInitiative(docmosisService.populateJudicialByCourtsInitiative(caseData));

        return judgeDecisionPdfDocumentBuilder.build();
    }

    private DocmosisTemplates getDocmosisTemplate() {
        return DISMISSAL_ORDER;
    }
}
