package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.Map;

public interface NotificationData {

    String CASE_REFERENCE = "claimReferenceNumber";
    String APPLICANT_REFERENCE = "claimantOrDefendant";
    String GA_NOTIFICATION_DEADLINE = "notificationDeadLine";
    String GA_APPLICATION_TYPE = "generalAppType";
    String GA_JUDICIAL_CONCURRENT_DATE_TEXT = "generalAppJudicialConcurrentDate";
    String GA_JUDICIAL_SEQUENTIAL_DATE_TEXT_RESPONDENT = "generalAppJudicialSequentialDateRespondent";
    String GA_REQUEST_FOR_INFORMATION_DEADLINE = "requestForInformationDeadline";
    String GA_HEARING_DATE = "hearingDate";
    String GA_HEARING_TIME = "hearingTime";
    String UPLOADED_DOCUMENTS = "uploadedDocuments";

    Map<String, String> addProperties(CaseData caseData);

}
