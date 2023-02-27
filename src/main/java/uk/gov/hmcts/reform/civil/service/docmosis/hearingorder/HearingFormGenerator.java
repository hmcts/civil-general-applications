package uk.gov.hmcts.reform.civil.service.docmosis.hearingorder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingDuration;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.HearingForm;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.model.genapplication.GADetailsRespondentSol;
import uk.gov.hmcts.reform.civil.model.genapplication.GeneralApplicationsDetails;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.Long.parseLong;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.HEARING_APPLICATION;

@Service
@RequiredArgsConstructor
public class HearingFormGenerator implements TemplateDataGenerator<HearingForm> {

    private final CaseDetailsConverter caseDetailsConverter;
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
                        DocumentType.HEARING_NOTICE
                )
        );
    }

    @Override
    public HearingForm getTemplateData(CaseData caseData) {
        CaseDetails parentCase = coreCaseDataService
                .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference()));
        CaseData parentCaseData = caseDetailsConverter.toCaseData(parentCase);
        boolean claimant1exists = canViewClaimant(parentCaseData, caseData);
        boolean claimant2exists = claimant1exists && nonNull(caseData.getClaimant2PartyName());
        boolean defendant1exists = canViewResp(parentCaseData, caseData, "1");
        boolean defendant2exists = canViewResp(parentCaseData, caseData, "2")
                && nonNull(caseData.getDefendant2PartyName());

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
                .claimant1exists(claimant1exists)
                .defendant1exists(defendant1exists)
                .claimant2exists(claimant2exists)
                .defendant2exists(defendant2exists)
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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

    protected static boolean canViewClaimant(CaseData civilCaseData, CaseData generalAppCaseData) {
        List<Element<GeneralApplicationsDetails>> gaAppDetails = civilCaseData.getClaimantGaAppDetails();
        if (Objects.isNull(gaAppDetails)) {
            return false;
        }
        return gaAppDetails.stream()
                .anyMatch(civilGaData -> generalAppCaseData.getCcdCaseReference()
                        .equals(parseLong(civilGaData.getValue().getCaseLink().getCaseReference())));
    }

    protected static boolean canViewResp(CaseData civilCaseData, CaseData generalAppCaseData, String respondent) {
        List<Element<GADetailsRespondentSol>> gaAppDetails;
        if (respondent.equals("2")) {
            gaAppDetails = civilCaseData.getRespondentSolTwoGaAppDetails();
        } else {
            gaAppDetails = civilCaseData.getRespondentSolGaAppDetails();
        }
        if (Objects.isNull(gaAppDetails)) {
            return false;
        }
        return gaAppDetails.stream()
                .anyMatch(civilGaData -> generalAppCaseData.getCcdCaseReference()
                        .equals(parseLong(civilGaData.getValue().getCaseLink().getCaseReference())));
    }

    protected DocmosisTemplates getTemplate(CaseData caseData) {
        return HEARING_APPLICATION;
    }
}
