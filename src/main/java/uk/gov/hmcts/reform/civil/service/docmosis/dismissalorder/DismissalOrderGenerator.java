package uk.gov.hmcts.reform.civil.service.docmosis.dismissalorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GAByCourtsInitiativeGAspec;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.judgedecisionpdfdocument.JudgeDecisionPdfDocument;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.ListGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.DISMISSAL_ORDER;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService.DATE_FORMATTER;
import static uk.gov.hmcts.reform.civil.utils.DateFormatterUtil.getFormattedDate;

@Service
@RequiredArgsConstructor
public class DismissalOrderGenerator implements TemplateDataGenerator<JudgeDecisionPdfDocument> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final ListGeneratorService listGeneratorService;
    private final IdamClient idamClient;
    private final ObjectMapper mapper;

    public CaseDocument generate(CaseData caseData, String authorisation) {
        UserDetails userDetails = idamClient.getUserDetails(authorisation);
        String judgeNameTitle = userDetails.getFullName();
        JudgeDecisionPdfDocument templateData = getTemplateData(caseData);
        Map<String, Object> map = templateData.toMap(mapper);
        map.put("judgeNameTitle", judgeNameTitle);
        templateData = mapper.convertValue(map, JudgeDecisionPdfDocument.class);

        DocmosisTemplates docmosisTemplate = getDocmosisTemplate();

        DocmosisDocument docmosisDocument = documentGeneratorService.generateDocmosisDocument(
            templateData,
            docmosisTemplate
        );

        return documentManagementService.uploadDocument(
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
                .claimNumber(caseData.getCcdCaseReference().toString())
                .claimantName(claimantName)
                .defendantName(defendantName)
                .courtName(caseData.getLocationName())
                .judgeRecital(caseData.getJudicialDecisionMakeOrder().getJudgeRecitalText())
                .dismissalOrder(caseData.getJudicialDecisionMakeOrder().getDismissalOrderText())
                .submittedOn(getFormattedDate(new Date()))
                .reasonAvailable(reasonAvailable(caseData))
                .reasonForDecision(populateJudgeReasonForDismissal(caseData))
                .judicialByCourtsInitiative(populateJudicialByCourtsInitiative(caseData));

        return judgeDecisionPdfDocumentBuilder.build();
    }

    private String populateJudgeReasonForDismissal(CaseData caseData) {
        if (Objects.nonNull(caseData.getJudicialDecisionMakeOrder().getShowReasonForDecision())
            && caseData.getJudicialDecisionMakeOrder().getShowReasonForDecision().equals(YesOrNo.NO)) {
            return "";
        }
        return caseData.getJudicialDecisionMakeOrder().getReasonForDecisionText() != null
            ? caseData.getJudicialDecisionMakeOrder().getReasonForDecisionText()
            : "";
    }

    private YesOrNo reasonAvailable(CaseData caseData) {
        if (Objects.nonNull(caseData.getJudicialDecisionMakeOrder().getShowReasonForDecision())
            && caseData.getJudicialDecisionMakeOrder().getShowReasonForDecision().equals(YesOrNo.NO)) {
            return YesOrNo.NO;
        }
        return YesOrNo.YES;
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
        return DISMISSAL_ORDER;
    }
}
