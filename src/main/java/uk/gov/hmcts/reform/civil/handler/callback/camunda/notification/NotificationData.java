package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.Map;

public interface NotificationData {

    String GENERAL_APPLICATION_REFERENCE = "claimReferenceNumber";
    String GENERAL_APPLICATION_RESPONDENT_NAME = "respondentName";
    String GENERAL_APPLICATION_RESPONDENT_EMAIL =  "respondentEmail";

    Map<String, String> addProperties(CaseData caseData);

}
