package uk.gov.hmcts.reform.civil.service.docmosis.requestmoreinformation;

import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.RESPOND_FOR_INFORMATION;

import uk.gov.hmcts.reform.civil.enums.CaseRole;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;
import uk.gov.hmcts.reform.civil.utils.DocUploadUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RespondForInformationGenerator implements TemplateDataGenerator<JudgeDecisionPdfDocument> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private String role;

    public CaseDocument generate(CaseData caseData, String authorisation, String role) {
        this.role = role;
        JudgeDecisionPdfDocument templateData = getTemplateData(caseData, authorisation);

        DocmosisTemplates docmosisTemplate = getTemplate();

        DocmosisDocument docmosisDocument = documentGeneratorService.generateDocmosisDocument(
                templateData,
                docmosisTemplate
        );

        return documentManagementService.uploadDocument(
                authorisation,
                new PDF(getFileName(docmosisTemplate), docmosisDocument.getBytes(),
                        DocumentType.REQUEST_FOR_INFORMATION)
        );
    }

    @Override
    public JudgeDecisionPdfDocument getTemplateData(CaseData caseData, String authorisation) {

        JudgeDecisionPdfDocument.JudgeDecisionPdfDocumentBuilder judgeDecisionPdfDocumentBuilder =
                JudgeDecisionPdfDocument.builder()
                        .claimNumber(caseData.getCcdCaseReference().toString())
                        .claimant1Name(caseData.getClaimant1PartyName())
                        .defendant1Name(caseData.getDefendant1PartyName())
                        .judgeComments(caseData.getGeneralAppAddlnInfoText())
                        .judgeNameTitle(getSubmittedBy(role, caseData))
                        .submittedOn(LocalDate.now());
        ;

        return judgeDecisionPdfDocumentBuilder.build();
    }

    private String getSubmittedBy(String role, CaseData caseData) {
        if (role.equals(DocUploadUtils.APPLICANT)) {
            return caseData.getApplicantPartyName();
        }
        return caseData.getClaimant1PartyName();
    }

    private String getFileName(DocmosisTemplates docmosisTemplate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(docmosisTemplate.getDocumentTitle(), LocalDateTime.now().format(formatter));
    }

    protected DocmosisTemplates getTemplate() {
        return RESPOND_FOR_INFORMATION;
    }
}
