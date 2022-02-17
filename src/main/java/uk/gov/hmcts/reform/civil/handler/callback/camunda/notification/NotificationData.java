package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.Map;

public interface NotificationData {

    String CASE_REFERENCE = "claimReferenceNumber";
    String GA_NOTIFICATION_DEADLINE = "notificationDeadLine";

    Map<String, String> addProperties(CaseData caseData);

}
