package uk.gov.hmcts.reform.civil.service.docmosis.hearingorder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingDuration;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.HearingForm;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.HEARING_APPLICATION;

@Service
@RequiredArgsConstructor
public class HearingFormGenerator implements TemplateDataGenerator<HearingForm> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final CoreCaseDataService coreCaseDataService;

    public CaseDocument generate(CaseData caseData, String authorisation) {

        HearingForm templateData = getTemplateData(caseData);
        DocmosisTemplates template = getTemplate(caseData);
        DocmosisDocument document =
                documentGeneratorService.generateDocmosisDocument(templateData, template);
        return documentManagementService.uploadDocument(
                authorisation,
                new PDF(
                        getFileName(caseData, template),
                        document.getBytes(),
                        DocumentType.HEARING_FORM
                )
        );
    }

    @Override
    public HearingForm getTemplateData(CaseData caseData) {
        CaseDetails parentCase = coreCaseDataService
                .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference()));
        return HearingForm.builder()
                .court(caseData.getGaHearingNoticeDetail().getHearingLocation().getValue().getLabel())
                .caseNumber(getCaseNumberFormatted(caseData))
                .creationDate(getDateFormatted(LocalDate.now()))
                .claimant(caseData.getClaimant1PartyName())
                .claimantReference(getReference(parentCase, "applicantSolicitor1Reference"))
                .defendant(caseData.getDefendant1PartyName())
                .defendantReference(getReference(parentCase, "respondentSolicitor1Reference"))
                .hearingDate(getDateFormatted(caseData.getGaHearingNoticeDetail().getHearingDate()))
                .hearingTime(getHearingTimeFormatted(caseData.getGaHearingNoticeDetail().getHearingTimeHourMinute()))
                .hearingType(caseData.getGaHearingNoticeDetail().getChannel().getDisplayedValue())
                .applicationDate(getDateFormatted(caseData
                        .getGaHearingNoticeApplication().getHearingNoticeApplicationDate()))
                .hearingDuration(getHearingDurationString(caseData))
                .additionalInfo(caseData.getGaHearingNoticeInformation())
                .applicant(caseData.getApplicantPartyName())
                .claimant2exists(nonNull(caseData.getClaimant2PartyName()))
                .defendant2exists(nonNull(caseData.getDefendant2PartyName()))
                .claimant2(nonNull(caseData.getClaimant2PartyName()) ? caseData.getClaimant2PartyName() : null)
                .defendant2(nonNull(caseData.getDefendant2PartyName()) ? caseData.getDefendant2PartyName() : null)
                .claimant2Reference(getReference(parentCase, "applicantSolicitor1Reference"))
                .defendant2Reference(getReference(parentCase, "respondentSolicitor2Reference"))
                .build();
    }

    protected String getCaseNumberFormatted(CaseData caseData) {
        String[] parts = caseData.getCcdCaseReference().toString().split("(?<=\\G.{4})");
        return String.join("-", parts);
    }

    protected String getFileName(CaseData caseData, DocmosisTemplates template) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyyHHmmss");
        return String.format(template.getDocumentTitle(),
                LocalDateTime.now().format(formatter));
    }

    protected String getDateFormatted(LocalDate date) {
        if (isNull(date)) {
            return null;
        }
        return DateFormatHelper.formatLocalDate(date, "dd/MMM/yyyy");
    }

    @SuppressWarnings("unchecked")
    protected String getReference(CaseDetails caseData, String refKey) {
        if (nonNull(caseData.getData().get("solicitorReferences"))) {
            return ((Map<String, String>) caseData.getData().get("solicitorReferences")).get(refKey);
        }
        return null;
    }

    protected static String getHearingTimeFormatted(String hearingTime) {
        if (isEmpty(hearingTime) || hearingTime.length() != 4 || !hearingTime.matches("[0-9]+")) {
            return null;
        }

        StringBuilder hearingTimeBuilder = new StringBuilder(hearingTime);
        hearingTimeBuilder.insert(2, ':');
        return hearingTimeBuilder.toString();
    }

    protected static String getHearingDurationString(CaseData caseData) {
        if (caseData.getGaHearingNoticeDetail().getHearingDuration().equals(GAHearingDuration.OTHER)) {
            return caseData.getGaHearingNoticeDetail().getHearingDurationOther();
        }
        return caseData.getGaHearingNoticeDetail().getHearingDuration().getDisplayedValue();
    }

    protected DocmosisTemplates getTemplate(CaseData caseData) {
        return HEARING_APPLICATION;
    }
}
