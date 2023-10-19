package uk.gov.hmcts.reform.civil.service.docmosis.directionorder;

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

import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.DIRECTION_ORDER;
import static uk.gov.hmcts.reform.civil.service.docmosis.generalorder.GeneralOrderGenerator.showRecital;

@Service
@RequiredArgsConstructor
public class DirectionOrderGenerator implements TemplateDataGenerator<JudgeDecisionPdfDocument> {

    private final ListGeneratorService listGenService;
    private final DocmosisService docmosisService;
    private final DocumentManagementService documentMangtService;
    private final DocumentGeneratorService documentGenService;
    private String judgeNameTitle;
    private final IdamClient idamClient;

    public CaseDocument generate(CaseData caseData, String authorisation) {

        UserDetails userDetails = idamClient.getUserDetails(authorisation);
        judgeNameTitle = userDetails.getFullName();

        JudgeDecisionPdfDocument templateData = getTemplateData(caseData);

        DocmosisTemplates docTemplate = getDocmosisTemplate();

        DocmosisDocument docDocument = documentGenService.generateDocmosisDocument(
            templateData,
            docTemplate
        );

        return documentMangtService.uploadDocument(
            authorisation,
            new PDF(getFileName(docTemplate), docDocument.getBytes(),
                    DocumentType.DIRECTION_ORDER)
        );
    }

    private String getFileName(DocmosisTemplates docmosisTemplate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return String.format(docmosisTemplate.getDocumentTitle(), LocalDateTime.now().format(formatter));
    }

    @Override
    public JudgeDecisionPdfDocument getTemplateData(CaseData caseData) {
        String claimantName = listGenService.claimantsName(caseData);

        String defendantName = listGenService.defendantsName(caseData);

        JudgeDecisionPdfDocument.JudgeDecisionPdfDocumentBuilder judgeDecisionPdfDocumentBuilder =
            JudgeDecisionPdfDocument.builder()
                .judgeNameTitle(judgeNameTitle)
                .claimNumber(caseData.getCcdCaseReference().toString())
                .claimantName(claimantName)
                .courtName(caseData.getLocationName())
                .defendantName(defendantName)
                    .judgeRecital(showRecital(caseData) ? caseData.getJudicialDecisionMakeOrder().getJudgeRecitalText() : null)
                .judgeDirection(caseData.getJudicialDecisionMakeOrder().getDirectionsText())
                .reasonForDecision(caseData.getJudicialDecisionMakeOrder().getReasonForDecisionText())
                .submittedOn(LocalDate.now())
                .reasonAvailable(docmosisService.reasonAvailable(caseData))
                .reasonForDecision(docmosisService.populateJudgeReason(caseData))
                .judicialByCourtsInitiative(docmosisService.populateJudicialByCourtsInitiative(caseData));

        return judgeDecisionPdfDocumentBuilder.build();
    }

    private DocmosisTemplates getDocmosisTemplate() {
        return DIRECTION_ORDER;
    }
}
