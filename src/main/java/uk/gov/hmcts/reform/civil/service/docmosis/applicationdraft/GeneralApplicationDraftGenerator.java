package uk.gov.hmcts.reform.civil.service.docmosis.applicationdraft;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.enums.dq.GAHearingSupportRequirements;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.helpers.DateFormatHelper;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.common.Element;
import uk.gov.hmcts.reform.civil.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.civil.model.docmosis.GADraftForm;
import uk.gov.hmcts.reform.civil.model.documents.CaseDocument;
import uk.gov.hmcts.reform.civil.model.documents.DocumentType;
import uk.gov.hmcts.reform.civil.model.documents.PDF;
import uk.gov.hmcts.reform.civil.model.genapplication.GAUnavailabilityDates;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;
import uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates;
import uk.gov.hmcts.reform.civil.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.ListGeneratorService;
import uk.gov.hmcts.reform.civil.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.civil.service.documentmanagement.DocumentManagementService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.service.docmosis.DocmosisTemplates.GENERAL_APPLICATION_DRAFT;

@Service
@RequiredArgsConstructor
public class GeneralApplicationDraftGenerator implements TemplateDataGenerator<GADraftForm> {

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final ListGeneratorService listGeneratorService;
    private final CoreCaseDataService coreCaseDataService;

    @Override
    public GADraftForm getTemplateData(CaseData caseData)  {

        CaseDetails civilMainCase = coreCaseDataService
            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference()));
        String claimantName = listGeneratorService.claimantsName(caseData);

        String defendantName = listGeneratorService.defendantsName(caseData);

        LocalDate dateFrom = null;
        LocalDate dateTo  = null;

        if (caseData.getGeneralAppHearingDetails() != null && caseData.getGeneralAppHearingDetails()
            .getGeneralAppUnavailableDates() != null) {

            List<Element<GAUnavailabilityDates>> datesUnavailableList = caseData.getGeneralAppHearingDetails()
                .getGeneralAppUnavailableDates();

            for (Element<GAUnavailabilityDates> dateRange : datesUnavailableList) {
                dateFrom = dateRange.getValue().getUnavailableTrialDateFrom();
                dateTo = dateRange.getValue().getUnavailableTrialDateTo();
            }
        }

        GADraftForm.GADraftFormBuilder gaDraftFormBuilder =
            GADraftForm.builder()
                .claimNumber(caseData.getCcdCaseReference().toString())
                .claimantName(claimantName)
                .defendantName(defendantName)
                .claimantReference(getReference(civilMainCase, "applicantSolicitor1Reference"))
                .defendantReference(getReference(civilMainCase, "respondentSolicitor1Reference"))
                .date(getDateFormatted(LocalDate.now()))
                .applicantPartyName(caseData.getApplicantPartyName())
                .hasAgreed(caseData.getGeneralAppRespondentAgreement().getHasAgreed())
                .isWithNotice(caseData.getGeneralAppInformOtherParty().getIsWithNotice())
                .reasonsForWithoutNotice(caseData.getGeneralAppInformOtherParty() != null ? caseData.getGeneralAppInformOtherParty()
                                             .getReasonsForWithoutNotice() : null)
                .generalAppUrgency(caseData.getGeneralAppUrgencyRequirement().getGeneralAppUrgency())
                .urgentAppConsiderationDate(getDateFormatted(caseData.getGeneralAppUrgencyRequirement()
                                                                 .getUrgentAppConsiderationDate()))
                .reasonsForUrgency(caseData.getGeneralAppUrgencyRequirement().getReasonsForUrgency())
                .generalAppType(caseData.getGeneralAppType().getTypes().stream()
                                    .map(GeneralApplicationTypes::getDisplayedValue)
                                    .collect(Collectors.joining(", ")))
                .generalAppDetailsOfOrder(caseData.getGeneralAppDetailsOfOrder())
                .generalAppReasonsOfOrder(caseData.getGeneralAppReasonsOfOrder())
                .hearingYesorNo(caseData.getGeneralAppHearingDetails().getHearingYesorNo())
                .hearingDate(getDateFormatted(caseData.getGeneralAppHearingDetails().getHearingDate()))
                .hearingPreferencesPreferredType(caseData.getGeneralAppHearingDetails()
                                                     .getHearingPreferencesPreferredType()
                                                     .getDisplayedValue())
                .reasonForPreferredHearingType(caseData.getGeneralAppHearingDetails()
                                                   .getReasonForPreferredHearingType())
                .hearingPreferredLocation(getHearingLocation(caseData))
                .hearingDetailsTelephoneNumber(caseData.getGeneralAppHearingDetails()
                                                   .getHearingDetailsTelephoneNumber())

                .hearingDetailsEmailId(caseData.getGeneralAppHearingDetails()
                                           .getHearingDetailsEmailID())
                .unavailableTrialRequiredYesOrNo(caseData.getGeneralAppHearingDetails()
                                                     .getUnavailableTrialRequiredYesOrNo())
                .unavailableTrialDateTo(getDateFormatted(dateTo))
                .unavailableTrialDateFrom(getDateFormatted(dateFrom))
                .vulnerabilityQuestionsYesOrNo(caseData.getGeneralAppHearingDetails().getVulnerabilityQuestionsYesOrNo())
                .supportRequirement(getGaSupportRequirement(caseData))
                .supportRequirementSignLanguage(caseData.getGeneralAppHearingDetails().getSupportRequirementSignLanguage())
                .supportRequirementLanguageInterpreter(caseData.getGeneralAppHearingDetails()
                                                           .getSupportRequirementLanguageInterpreter())
                .supportRequirementOther(caseData.getGeneralAppHearingDetails().getSupportRequirementOther())
                .name(caseData.getGeneralAppStatementOfTruth() != null ? caseData
                    .getGeneralAppStatementOfTruth().getName() : null)
                .date(getDateFormatted(LocalDate.now()));

        return gaDraftFormBuilder.build();
    }

    private String getGaSupportRequirement(CaseData caseData) {
        String gaSupportRequirement = null;
        if (caseData.getGeneralAppHearingDetails() != null
            && caseData.getGeneralAppHearingDetails().getSupportRequirement() != null) {
            gaSupportRequirement = caseData.getGeneralAppHearingDetails().getSupportRequirement().stream().map(
                GAHearingSupportRequirements::getDisplayedValue).collect(Collectors.joining(", "));
        }
        return  gaSupportRequirement;
    }

    private String getHearingLocation(CaseData caseData) {
        String preferredLocation = null;
        if (caseData.getGeneralAppHearingDetails() != null
            && caseData.getGeneralAppHearingDetails().getHearingPreferredLocation() != null) {
            preferredLocation = caseData.getGeneralAppHearingDetails().getHearingPreferredLocation()
                .getValue().getLabel();
        }
        return preferredLocation;
    }

    @SuppressWarnings("unchecked")
    protected String getReference(CaseDetails caseData, String refKey) {
        if (nonNull(caseData.getData().get("solicitorReferences"))) {
            return ((Map<String, String>) caseData.getData().get("solicitorReferences")).get(refKey);
        }
        return null;
    }

    protected String getDateFormatted(LocalDate date) {
        if (isNull(date)) {
            return null;
        }
        return DateFormatHelper.formatLocalDate(date, " d MMMM yyyy");
    }

    public CaseDocument generate(CaseData caseData, String authorisation) {
        GADraftForm templateData = getTemplateData(caseData);

        DocmosisTemplates docmosisTemplate = getDocmosisTemplate();

        DocmosisDocument docmosisDocument = documentGeneratorService.generateDocmosisDocument(
            templateData,
            docmosisTemplate
        );

        return documentManagementService.uploadDocument(
            authorisation,
            new PDF(getFileName(docmosisTemplate), docmosisDocument.getBytes(),
                    DocumentType.GENERAL_APPLICATION_DRAFT)
        );
    }

    private String getFileName(DocmosisTemplates docmosisTemplate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(docmosisTemplate.getDocumentTitle(), LocalDateTime.now().format(formatter));
    }

    private DocmosisTemplates getDocmosisTemplate() {
        return GENERAL_APPLICATION_DRAFT;
    }
}
