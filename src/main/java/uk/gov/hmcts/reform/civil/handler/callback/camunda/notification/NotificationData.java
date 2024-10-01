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
    String GA_LIP_APPLICANT_NAME = "applicantName";
    String GA_LIP_RESP_NAME = "respondentName";
    String CASE_TITLE = "ClaimantvDefendant";
    String CLAIM_LEGAL_ORG_NAME_SPEC = "legalOrgName";

    //hwf
    String CLAIMANT_NAME = "claimantName";
    String APPLICANT_NAME = "applicantName";
    String HWF_REFERENCE_NUMBER = "hwFReferenceNumber";
    String TYPE_OF_FEE = "typeOfFee";
    String HWF_MORE_INFO_DATE = "date";
    String HWF_MORE_INFO_DOCUMENTS = "documents";
    String PART_AMOUNT = "partAmount";
    String REMAINING_AMOUNT = "remainingAmount";
    String FEE_AMOUNT = "amount";
    String NO_REMISSION_REASONS = "reasons";

    Map<String, String> addProperties(CaseData caseData);

}
