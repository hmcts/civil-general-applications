package uk.gov.hmcts.reform.civil.service.docmosis.generalorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.dq.FinalOrderShowToggle;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
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
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.GENERAL_ORDER;

@Service
@RequiredArgsConstructor
public class GeneralOrderGenerator implements TemplateDataGenerator<JudgeDecisionPdfDocument> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final ListGeneratorService listGeneratorService;
    private final ObjectMapper mapper;
    private final DocmosisService docmosisService;
    private String judgeNameTitle;
    private final IdamClient idamClient;

    public CaseDocument generate(CaseData caseData, String authorisation) {
        UserDetails userDetails = idamClient.getUserDetails(authorisation);
        judgeNameTitle = userDetails.getFullName();
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

        JudgeDecisionPdfDocument.JudgeDecisionPdfDocumentBuilder judgeDecisionPdfDocumentBuilder =
            JudgeDecisionPdfDocument.builder()
                .judgeNameTitle(judgeNameTitle)
                .claimNumber(caseData.getCcdCaseReference().toString())
                .claimantName(claimantName)
                .courtName(caseData.getLocationName())
                .siteName(caseData.getCaseManagementLocation().getSiteName())
                .address(caseData.getCaseManagementLocation().getAddress())
                .postcode(caseData.getCaseManagementLocation().getPostcode())
                .defendantName(defendantName)
                .judgeRecital(showRecital(caseData) ? caseData.getJudicialDecisionMakeOrder().getJudgeRecitalText() : null)
                .generalOrder(caseData.getJudicialDecisionMakeOrder().getOrderText())
                .submittedOn(LocalDate.now())
                .reasonAvailable(docmosisService.reasonAvailable(caseData))
                .reasonForDecision(docmosisService.populateJudgeReason(caseData))
                .judicialByCourtsInitiative(docmosisService.populateJudicialByCourtsInitiative(caseData));

        return judgeDecisionPdfDocumentBuilder.build();
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
