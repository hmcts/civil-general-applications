package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.Map;

public interface NotificationData {

    String CLAIM_REFERENCE_NUMBER = "claimReferenceNumber";
    String CLAIM_DETAILS_NOTIFICATION_DEADLINE = "claimDetailsNotificationDeadline";
    Map<String, String> addProperties(CaseData caseData);

}
