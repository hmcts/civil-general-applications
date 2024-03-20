package uk.gov.hmcts.reform.civil.service.docmosis.consentorder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.ConsentOrderForm;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
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

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.CONSENT_ORDER_FORM;

@Service
@RequiredArgsConstructor
public class ConsentOrderGenerator implements TemplateDataGenerator<ConsentOrderForm> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final DocmosisService docmosisService;

    @Override
    public ConsentOrderForm getTemplateData(CaseData caseData, String authorisation)  {

        ConsentOrderForm.ConsentOrderFormBuilder consentOrderFormBuilder =
            ConsentOrderForm.builder()
                .claimNumber(caseData.getCcdCaseReference().toString())
                .isMultiParty(caseData.getIsMultiParty())
                .claimant1Name(caseData.getClaimant1PartyName())
                .claimant2Name(caseData.getClaimant2PartyName() != null ? caseData.getClaimant2PartyName() : null)
                .defendant1Name(caseData.getDefendant1PartyName())
                .defendant2Name(caseData.getDefendant2PartyName() != null ? caseData.getDefendant2PartyName() : null)
                .orderDate(LocalDate.now())
                .courtName(docmosisService.getCaseManagementLocationVenueName(caseData, authorisation).getVenueName())
                .siteName(caseData.getCaseManagementLocation().getSiteName())
                .address(caseData.getCaseManagementLocation().getAddress())
                .postcode(caseData.getCaseManagementLocation().getPostcode())
                .consentOrder(caseData.getApproveConsentOrder()
                                  .getConsentOrderDescription());

        return consentOrderFormBuilder.build();
    }

    protected String getDateFormatted(LocalDate date) {
        if (isNull(date)) {
            return null;
        }
        return DateFormatHelper.formatLocalDate(date, " d MMMM yyyy");
    }

    public CaseDocument generate(CaseData caseData, String authorisation) {
        ConsentOrderForm templateData = getTemplateData(caseData, authorisation);

        DocmosisTemplates docmosisTemplate = getDocmosisTemplate();

        DocmosisDocument docmosisDocument = documentGeneratorService.generateDocmosisDocument(
            templateData,
            docmosisTemplate
        );

        return documentManagementService.uploadDocument(
            authorisation,
            new PDF(getFileName(docmosisTemplate), docmosisDocument.getBytes(),
                    DocumentType.CONSENT_ORDER)
        );
    }

    private String getFileName(DocmosisTemplates docmosisTemplate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(docmosisTemplate.getDocumentTitle(), LocalDateTime.now().format(formatter));
    }

    private DocmosisTemplates getDocmosisTemplate() {
        return CONSENT_ORDER_FORM;
    }

}
