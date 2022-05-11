package uk.gov.hmcts.reform.civil.service.docmosis.directionorder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.directionorder.DirectionOrder;
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

import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.DIRECTION_ORDER;

@Service
@RequiredArgsConstructor
public class DirectionOrderGenerator implements TemplateDataGenerator<DirectionOrder> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;

    public CaseDocument generate(CaseData caseData, String authorisation) {
        DirectionOrder templateData = getTemplateData(caseData);

        DocmosisTemplates docmosisTemplate = getDocmosisTemplate(caseData);

        DocmosisDocument docmosisDocument = documentGeneratorService.generateDocmosisDocument(
            templateData,
            docmosisTemplate
        );

        return documentManagementService.uploadDocument(
            authorisation,
            new PDF(getFileName(docmosisTemplate, caseData), docmosisDocument.getBytes(),
                    DocumentType.DIRECTION_ORDER)
        );
    }

    private String getFileName(DocmosisTemplates docmosisTemplate, CaseData caseData) {
        return String.format(docmosisTemplate.getDocumentTitle(), caseData.getLegacyCaseReference());
    }

    @Override
    public DirectionOrder getTemplateData(CaseData caseData) {
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

        DirectionOrder.DirectionOrderBuilder directionOrderBuilder =
            DirectionOrder.builder()
                .claimNumber(caseData.getCcdCaseReference().toString())
                .applicationType(collect)
                .claimantName(claimantName)
                .defendantName(defendantName)
                .applicantName(caseData.getApplicantPartyName())
                .applicationDate(caseData.getCreatedDate().toLocalDate())
                .judgeDirection(caseData.getJudicialDecisionMakeOrder().getDirectionsText())
                .reasonForDecision(caseData.getJudicialDecisionMakeOrder().getReasonForDecisionText())
                .submittedOn(LocalDate.now());

        return directionOrderBuilder.build();
    }

    private DocmosisTemplates getDocmosisTemplate(CaseData caseData) {
        return DIRECTION_ORDER;
    }
}
