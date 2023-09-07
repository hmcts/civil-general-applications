package uk.gov.hmcts.reform.civil.service.docmosis.generalorder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.civil.enums.dq.FinalOrderShowToggle;
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.ListGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.GENERAL_ORDER;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.DATE_FORMATTER;
import static uk.gov.hmcts.reform.civil.utils.DateFormatterUtil.getFormattedDate;

@Service
@RequiredArgsConstructor
public class GeneralOrderGenerator implements TemplateDataGenerator<JudgeDecisionPdfDocument> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final ListGeneratorService listGeneratorService;

    public CaseDocument generate(CaseData caseData, String authorisation) {
        JudgeDecisionPdfDocument templateData = getTemplateData(caseData);

        DocmosisTemplates docmosisTemplate = getDocmosisTemplate();

        DocmosisDocument docmosisDocument = documentGeneratorService.generateDocmosisDocument(
            templateData,
            docmosisTemplate
        );

        return documentManagementService.uploadDocument(
            authorisation,
            new PDF(getFileName(docmosisTemplate), docmosisDocument.getBytes(),
                    DocumentType.GENERAL_ORDER)
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

        String collect = listGeneratorService.applicationType(caseData);

        JudgeDecisionPdfDocument.JudgeDecisionPdfDocumentBuilder judgeDecisionPdfDocumentBuilder =
            JudgeDecisionPdfDocument.builder()
                .claimNumber(caseData.getCcdCaseReference().toString())
                .applicationType(collect)
                .claimantName(claimantName)
                .defendantName(defendantName)
                .applicationDate(caseData.getCreatedDate().toLocalDate())
                .judgeRecital(showRecital(caseData) ? caseData.getJudicialDecisionMakeOrder().getJudgeRecitalText() : null)
                .generalOrder(caseData.getJudicialDecisionMakeOrder().getOrderText())
                .submittedOn(getFormattedDate(new Date()))
                .reasonForDecision(populateJudgeReasonForDecisionText(caseData))
                .judicialByCourtsInitiative(populateJudicialByCourtsInitiative(caseData))
                .locationName(caseData.getLocationName());

        return judgeDecisionPdfDocumentBuilder.build();
    }

    private String populateJudgeReasonForDecisionText(CaseData caseData) {
        return caseData.getJudicialDecisionMakeOrder().getReasonForDecisionText() != null
            ? "Reasons for decision: \n" + caseData.getJudicialDecisionMakeOrder().getReasonForDecisionText()
            : "";
    }

    private String populateJudicialByCourtsInitiative(CaseData caseData) {

        if (caseData.getJudicialDecisionMakeOrder().getJudicialByCourtsInitiative().equals(GAByCourtsInitiativeGAspec
                                                                                               .OPTION_3)) {
            return StringUtils.EMPTY;
        }

        if (caseData.getJudicialDecisionMakeOrder().getJudicialByCourtsInitiative()
            .equals(GAByCourtsInitiativeGAspec.OPTION_1)) {
            return caseData.getJudicialDecisionMakeOrder().getOrderCourtOwnInitiative() + " "
                .concat(caseData.getJudicialDecisionMakeOrder().getOrderCourtOwnInitiativeDate()
                            .format(DATE_FORMATTER));
        } else {
            return caseData.getJudicialDecisionMakeOrder().getOrderWithoutNotice() + " "
                .concat(caseData.getJudicialDecisionMakeOrder().getOrderWithoutNoticeDate()
                            .format(DATE_FORMATTER));
        }
    }

    private DocmosisTemplates getDocmosisTemplate() {
        return GENERAL_ORDER;
    }

    public static boolean showRecital(CaseData caseData) {
        GAJudicialMakeAnOrder judicialDecisionMakeOrder = caseData.getJudicialDecisionMakeOrder();
        return Objects.nonNull(judicialDecisionMakeOrder)
                && Objects.nonNull(judicialDecisionMakeOrder.getShowJudgeRecitalText())
                && Objects.nonNull(judicialDecisionMakeOrder.getShowJudgeRecitalText().get(0))
                && judicialDecisionMakeOrder.getShowJudgeRecitalText().get(0).equals(FinalOrderShowToggle.SHOW);
    }
}
