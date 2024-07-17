package uk.gov.hmcts.reform.civil.service;

import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.civil.helpers.DateFormatHelper.formatLocalDate;

import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.civil.enums.FeeType;
import uk.gov.hmcts.reform.civil.enums.HwFMoreInfoRequiredDocuments;
import uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.HelpWithFeesMoreInformation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HwfNotificationService implements NotificationData {

    private final NotificationService notificationService;
    private final NotificationsProperties notificationsProperties;
    private final CaseDetailsConverter caseDetailsConverter;
    private final CoreCaseDataService coreCaseDataService;
    private final SolicitorEmailValidation solicitorEmailValidation;
    private Map<CaseEvent, String> emailTemplates;

    private static final String ERROR_HWF_EVENT = "Hwf Event not support";

    public void sendNotification(CaseData caseData) throws NotificationException {
        CaseData civilCaseData = caseDetailsConverter
                .toCaseData(coreCaseDataService
                        .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));

        caseData = solicitorEmailValidation.validateSolicitorEmail(civilCaseData, caseData);
        CaseEvent event = getEvent(caseData);

        notificationService.sendMail(
                caseData.getGeneralAppApplnSolicitor().getEmail(),
                getTemplate(event),
                addProperties(caseData),
                caseData.getGeneralAppParentCaseLink().getCaseReference()
        );
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        Map<String, String> commonProperties = getCommonProperties(caseData);
        Map<String, String> furtherProperties = getFurtherProperties(caseData);
        return Collections.unmodifiableMap(
                Stream.concat(commonProperties.entrySet().stream(), furtherProperties.entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private Map<String, String> getCommonProperties(CaseData caseData) {
        return Map.of(
                CASE_REFERENCE, caseData.getCcdCaseReference().toString(),
                CLAIMANT_NAME, caseData.getApplicantPartyName(),
                TYPE_OF_FEE, caseData.getHwfFeeType().getLabel(),
                HWF_REFERENCE_NUMBER, caseData.getGeneralAppHelpWithFees().getHelpWithFeesReferenceNumber()
        );
    }

    private Map<String, String> getFurtherProperties(CaseData caseData) {
        return switch (getEvent(caseData)) {
            //case NO_REMISSION_HWF -> getNoRemissionProperties(caseData);
            case MORE_INFORMATION_HWF_GA -> getMoreInformationProperties(caseData);
            //case PARTIAL_REMISSION_HWF_GRANTED -> getPartialRemissionProperties(caseData);
            case INVALID_HWF_REFERENCE_GA, UPDATE_HELP_WITH_FEE_NUMBER_GA -> Collections.emptyMap();
            default -> throw new NotificationException(new Exception(ERROR_HWF_EVENT));
        };
    }

    private CaseEvent getEvent(CaseData caseData) {
        if (caseData.getHwfFeeType().equals(FeeType.APPLICATION)) {
            return caseData.getGaHwfDetails().getHwfCaseEvent();
        } else if (caseData.getHwfFeeType().equals(FeeType.ADDITIONAL)) {
            return caseData.getAdditionalHwfDetails().getHwfCaseEvent();
        } else {
            throw new NotificationException(new Exception(ERROR_HWF_EVENT));
        }
    }

    private String getTemplate(CaseEvent hwfEvent) {
        if (emailTemplates == null) {
            emailTemplates = Map.of(
                CaseEvent.MORE_INFORMATION_HWF_GA,
                notificationsProperties.getNotifyApplicantForHwFMoreInformationNeeded(),
                CaseEvent.INVALID_HWF_REFERENCE_GA,
                notificationsProperties.getNotifyApplicantForHwfInvalidRefNumber(),
                CaseEvent.UPDATE_HELP_WITH_FEE_NUMBER_GA,
                notificationsProperties.getNotifyApplicantForHwfUpdateRefNumber()
            );
        }
        return emailTemplates.get(hwfEvent);
    }

    private Map<String, String> getMoreInformationProperties(CaseData caseData) {
        HelpWithFeesMoreInformation moreInformation =
                null != caseData.getHelpWithFeesMoreInformationGa()
                        ? caseData.getHelpWithFeesMoreInformationGa()
                        : caseData.getHelpWithFeesMoreInformationAdditional();
        return Map.of(
                HWF_MORE_INFO_DATE, formatLocalDate(moreInformation.getHwFMoreInfoDocumentDate(), DATE),
                HWF_MORE_INFO_DOCUMENTS, getMoreInformationDocumentList(
                        moreInformation.getHwFMoreInfoRequiredDocuments()
                )
        );
    }

    private String getMoreInformationDocumentList(List<HwFMoreInfoRequiredDocuments> list) {
        StringBuilder documentList = new StringBuilder();
        for (HwFMoreInfoRequiredDocuments doc : list) {
            documentList.append(doc.getName());
            if (!doc.getDescription().isEmpty()) {
                documentList.append(" - ");
                documentList.append(doc.getDescription());
            }
            documentList.append("\n");
            documentList.append("\n");
        }
        return documentList.toString();
    }
}
