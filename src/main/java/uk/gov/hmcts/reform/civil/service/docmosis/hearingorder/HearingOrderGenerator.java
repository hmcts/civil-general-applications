package uk.gov.hmcts.reform.civil.service.docmosis.hearingorder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.GAJudicialHearingType;
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
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.HEARING_ORDER;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.DATE_FORMATTER;

@Service
@RequiredArgsConstructor
public class HearingOrderGenerator implements TemplateDataGenerator<JudgeDecisionPdfDocument> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
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
                    DocumentType.HEARING_ORDER)
        );
    }

    private String getFileName(DocmosisTemplates docmosisTemplate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return String.format(docmosisTemplate.getDocumentTitle(), LocalDateTime.now().format(formatter));
    }

    @Override
    public JudgeDecisionPdfDocument getTemplateData(CaseData caseData, String authorisation) {

        JudgeDecisionPdfDocument.JudgeDecisionPdfDocumentBuilder judgeDecisionPdfDocumentBuilder =
            JudgeDecisionPdfDocument.builder()
                .judgeNameTitle(caseData.getJudgeTitle())
                .claimNumber(caseData.getCcdCaseReference().toString())
                .isMultiParty(caseData.getIsMultiParty())
                .claimant1Name(caseData.getClaimant1PartyName())
                .claimant2Name(caseData.getClaimant2PartyName() != null ? caseData.getClaimant2PartyName() : null)
                .defendant1Name(caseData.getDefendant1PartyName())
                .defendant2Name(caseData.getDefendant2PartyName() != null ? caseData.getDefendant2PartyName() : null)
                .judgeRecital(caseData.getJudicialGeneralHearingOrderRecital())
                .hearingOrder(caseData.getJudicialGOHearingDirections())
                .hearingPrefType(caseData.getJudicialListForHearing()
                                     .getHearingPreferencesPreferredType().getDisplayedValue())
                .estimatedHearingLength(caseData.getJudicialListForHearing()
                                            .getJudicialTimeEstimate().getDisplayedValue())
                .submittedOn(LocalDate.now())
                .courtName(docmosisService.getCaseManagementLocationVenueName(caseData, authorisation).getVenueName())
                .judgeHearingLocation(caseData.getJudicialListForHearing()
                                          .getHearingPreferencesPreferredType() == GAJudicialHearingType.IN_PERSON
                                          ? caseData.getJudicialListForHearing()
                    .getHearingPreferredLocation().getValue().getLabel() : null)
                .siteName(caseData.getCaseManagementLocation().getSiteName())
                .address(caseData.getCaseManagementLocation().getAddress())
                .postcode(caseData.getCaseManagementLocation().getPostcode())
                .judicialByCourtsInitiativeListForHearing(populateJudicialByCourtsInitiative(caseData));

        return judgeDecisionPdfDocumentBuilder.build();
    }

    private String populateJudicialByCourtsInitiative(CaseData caseData) {

        if (caseData.getJudicialByCourtsInitiativeListForHearing().equals(GAByCourtsInitiativeGAspec
                                                                                               .OPTION_3)) {
            return StringUtils.EMPTY;
        }

        if (caseData.getJudicialByCourtsInitiativeListForHearing()
            .equals(GAByCourtsInitiativeGAspec.OPTION_1)) {
            return caseData.getOrderCourtOwnInitiativeListForHearing().getOrderCourtOwnInitiative() + " "
                .concat(caseData.getOrderCourtOwnInitiativeListForHearing().getOrderCourtOwnInitiativeDate()
                            .format(DATE_FORMATTER));
        } else {
            return caseData.getOrderWithoutNoticeListForHearing().getOrderWithoutNotice() + " "
                .concat(caseData.getOrderWithoutNoticeListForHearing().getOrderWithoutNoticeDate()
                            .format(DATE_FORMATTER));
        }
    }

    private DocmosisTemplates getDocmosisTemplate() {
        return HEARING_ORDER;
    }
}
