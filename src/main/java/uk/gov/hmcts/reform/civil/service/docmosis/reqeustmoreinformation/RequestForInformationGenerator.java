package uk.gov.hmcts.reform.civil.service.docmosis.reqeustmoreinformation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.requestforinformation.RequestForInformation;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;

import java.time.LocalDate;

import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.REQUEST_FOR_INFORMATION;

@Service
@RequiredArgsConstructor
public class RequestForInformationGenerator implements TemplateDataGenerator<RequestForInformation> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;

    public CaseDocument generate(CaseData caseData, String authorisation) {
        RequestForInformation templateData = getTemplateData(caseData);

        DocmosisTemplates docmosisTemplate = getDocmosisTemplate(caseData);

        DocmosisDocument docmosisDocument = documentGeneratorService.generateDocmosisDocument(
            templateData,
            docmosisTemplate
        );

        return documentManagementService.uploadDocument(
            authorisation,
            new PDF(getFileName(docmosisTemplate, caseData), docmosisDocument.getBytes(),
                    DocumentType.REQUEST_FOR_INFORMATION)
        );
    }

    private String getFileName(DocmosisTemplates docmosisTemplate, CaseData caseData) {
        return String.format(docmosisTemplate.getDocumentTitle(), caseData.getLegacyCaseReference());
    }

    @Override
    public RequestForInformation getTemplateData(CaseData caseData) {

        RequestForInformation.RequestForInformationBuilder requestForInformationBuilderBuilder =
            RequestForInformation.builder()
            .claimNumber("TestClaimNumber")
            .applicationType("TestApplicationType")
            .claimantName("TestClaimantName")
            .defendantName("TestDefendantName")
            .issueDate(LocalDate.now())
            .judgeComments("TestJudgeComment")
            .submittedOn(LocalDate.now().plusDays(14));

        return requestForInformationBuilderBuilder.build();
    }

    private DocmosisTemplates getDocmosisTemplate(CaseData caseData) {
        return REQUEST_FOR_INFORMATION;
    }
}
