package uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationsequentialorder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.writtenrepresentationsequentialorder.WrittenRepSequentialOrder;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.WRITTEN_REP_SEQUENTIAL;

@Service
@RequiredArgsConstructor
public class WrittenRepresentationSequentailOrderGenerator implements TemplateDataGenerator<WrittenRepSequentialOrder> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;

    public CaseDocument generate(CaseData caseData, String authorisation) {
        WrittenRepSequentialOrder templateData = getTemplateData(caseData);

        DocmosisTemplates docmosisTemplate = getDocmosisTemplate(caseData);

        DocmosisDocument docmosisDocument = documentGeneratorService.generateDocmosisDocument(
            templateData,
            docmosisTemplate
        );

        return documentManagementService.uploadDocument(
            authorisation,
            new PDF(getFileName(docmosisTemplate, caseData), docmosisDocument.getBytes(),
                    DocumentType.WRITTEN_REP_SEQUENTIAL)
        );
    }

    private String getFileName(DocmosisTemplates docmosisTemplate, CaseData caseData) {
        return String.format(docmosisTemplate.getDocumentTitle(), caseData.getLegacyCaseReference());
    }

    @Override
    public WrittenRepSequentialOrder getTemplateData(CaseData caseData) {
        List<String> claimantNames = new ArrayList<>();
        claimantNames.add(caseData.getClaimant1PartyName());
        if (caseData.getClaimant2PartyName() != null) {
            claimantNames.add(caseData.getClaimant2PartyName());
        }
        String claimantName = String.join(", ", claimantNames);

        List<String> defendentNames = new ArrayList<>();
        defendentNames.add(caseData.getDefendant1PartyName());
        if (caseData.getDefendant2PartyName() != null) {
            defendentNames.add(caseData.getDefendant2PartyName());
        }
        String defendantName = String.join(", ", defendentNames);

        List<GeneralApplicationTypes> types = caseData.getGeneralAppType().getTypes();
        String collect = types.stream()
            .map(GeneralApplicationTypes::getDisplayedValue).collect(Collectors.joining(", "));

        WrittenRepSequentialOrder.WrittenRepSequentialOrderBuilder writtenRepSequentialOrderBuilder =
            WrittenRepSequentialOrder.builder()
                .claimNumber(caseData.getCcdCaseReference().toString())
                .applicationType(collect)
                .claimantName(claimantName)
                .defendantName(defendantName)
                .applicantName(caseData.getApplicantPartyName())
                .applicationDate(caseData.getCreatedDate().toLocalDate())
                .uploadDeadlineDate(caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                        .getWrittenSequentailRepresentationsBy())
                .responseDeadlineDate(caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                          .getSequentialApplicantMustRespondWithin())
                .submittedOn(LocalDate.now());

        return writtenRepSequentialOrderBuilder.build();
    }

    private DocmosisTemplates getDocmosisTemplate(CaseData caseData) {
        return WRITTEN_REP_SEQUENTIAL;
    }
}
