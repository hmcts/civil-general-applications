package uk.gov.hmcts.reform.civil.service.docmosis.writtenrepresentationconcurrentorder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.WRITTEN_REPRESENTATION_CONCURRENT;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.DATE_FORMATTER;

@Service
@RequiredArgsConstructor
public class WrittenRepresentationConcurrentOrderGenerator implements TemplateDataGenerator<JudgeDecisionPdfDocument> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final ListGeneratorService listGeneratorService;

    private final DocmosisService docmosisService;

    public CaseDocument generate(CaseData caseData, String authorisation) {

        JudgeDecisionPdfDocument templateData = getTemplateData(caseData, authorisation);

        DocmosisTemplates docmosisTemplate = getDocmosisTemplate();

        DocmosisDocument docmosisDocument = documentGeneratorService.generateDocmosisDocument(
            templateData,
            docmosisTemplate
        );

        return documentManagementService.uploadDocument(
            authorisation,
            new PDF(getFileName(docmosisTemplate), docmosisDocument.getBytes(),
                    DocumentType.WRITTEN_REPRESENTATION_CONCURRENT)
        );
    }

    private String getFileName(DocmosisTemplates docmosisTemplate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(docmosisTemplate.getDocumentTitle(), LocalDateTime.now().format(formatter));
    }

    @Override
    public JudgeDecisionPdfDocument getTemplateData(CaseData caseData, String authorisation) {
        String collect = listGeneratorService.applicationType(caseData);

        JudgeDecisionPdfDocument.JudgeDecisionPdfDocumentBuilder judgeDecisionPdfDocumentBuilder =
            JudgeDecisionPdfDocument.builder()
                .judgeNameTitle(caseData.getJudgeTitle())
                .claimNumber(caseData.getCcdCaseReference().toString())
                .applicationType(collect)
                .isMultiParty(caseData.getIsMultiParty())
                .claimant1Name(caseData.getClaimant1PartyName())
                .claimant2Name(caseData.getClaimant2PartyName() != null ? caseData.getClaimant2PartyName() : null)
                .defendant1Name(caseData.getDefendant1PartyName())
                .defendant2Name(caseData.getDefendant2PartyName() != null ? caseData.getDefendant2PartyName() : null)
                .judgeRecital(caseData.getJudgeRecitalText())
                .writtenOrder(caseData.getDirectionInRelationToHearingText())
                .uploadDeadlineDate(caseData.getJudicialDecisionMakeAnOrderForWrittenRepresentations()
                                        .getWrittenConcurrentRepresentationsBy())
                .submittedOn(LocalDate.now())
                .courtName(docmosisService.getCaseManagementLocationVenueName(caseData, authorisation).getVenueName())
                .siteName(caseData.getCaseManagementLocation().getSiteName())
                .address(caseData.getCaseManagementLocation().getAddress())
                .postcode(caseData.getCaseManagementLocation().getPostcode())
                .judicialByCourtsInitiativeForWrittenRep(populateJudicialByCourtsInitiative(caseData));

        return judgeDecisionPdfDocumentBuilder.build();
    }

    private String populateJudicialByCourtsInitiative(CaseData caseData) {

        if (caseData.getJudicialByCourtsInitiativeForWrittenRep().equals(GAByCourtsInitiativeGAspec
                                                                              .OPTION_3)) {
            return StringUtils.EMPTY;
        }

        if (caseData.getJudicialByCourtsInitiativeForWrittenRep()
            .equals(GAByCourtsInitiativeGAspec.OPTION_1)) {
            return caseData.getOrderCourtOwnInitiativeForWrittenRep().getOrderCourtOwnInitiative() + " "
                .concat(caseData.getOrderCourtOwnInitiativeForWrittenRep().getOrderCourtOwnInitiativeDate()
                            .format(DATE_FORMATTER));
        } else {
            return caseData.getOrderWithoutNoticeForWrittenRep().getOrderWithoutNotice() + " "
                .concat(caseData.getOrderWithoutNoticeForWrittenRep().getOrderWithoutNoticeDate()
                            .format(DATE_FORMATTER));
        }
    }

    private DocmosisTemplates getDocmosisTemplate() {
        return WRITTEN_REPRESENTATION_CONCURRENT;
    }
}
