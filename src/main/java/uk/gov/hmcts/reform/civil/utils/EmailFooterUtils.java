package uk.gov.hmcts.reform.civil.utils;

import uk.gov.hmcts.reform.civil.config.NotificationsSignatureConfiguration;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.reform.civil.enums.CaseState.CASE_DISMISSED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.CLOSED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PENDING_CASE_ISSUED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PROCEEDS_IN_HERITAGE_SYSTEM;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.HMCTS_SIGNATURE;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.SPEC_CONTACT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.OPENING_HOURS;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.PHONE_CONTACT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.SPEC_UNSPEC_CONTACT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.WELSH_HMCTS_SIGNATURE;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.WELSH_CONTACT;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.WELSH_OPENING_HOURS;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.notification.NotificationData.WELSH_PHONE_CONTACT;

public class EmailFooterUtils {

    private EmailFooterUtils() {
        //NO-OP
    }

    public static String RAISE_QUERY_LR = "Contact us about your claim by selecting "
        + "Raise a query from the next steps menu in case file view.";

    public static String RAISE_QUERY_LIP = "To contact the court, select contact or "
        + "apply to the court on your dashboard.";

    public static String RAISE_QUERY_LIP_WELSH = "I gysylltu â’r llys, dewiswch "
        + "‘contact or apply to the court’ ar eich dangosfwrdd.";

    public static final Set<CaseState> qmNotAllowedStates = EnumSet.of(PENDING_CASE_ISSUED, CLOSED,
                                                                       PROCEEDS_IN_HERITAGE_SYSTEM, CASE_DISMISSED);

    private static boolean queryNotAllowedCaseStates(CaseData caseData) {
        return qmNotAllowedStates.contains(caseData.getCcdState());
    }

    public static Map<String, String> addAllFooterItems(CaseData caseData, Map<String, String> properties,
                                 NotificationsSignatureConfiguration configuration,
                                 boolean isLRQmEnabled, boolean isLipQMEnabled) {
        addCommonFooterSignature(properties, configuration);
        addCommonFooterSignatureWelsh(properties, configuration);
        addSpecAndUnspecContact(caseData, properties, configuration,
                                isLRQmEnabled);
        addLipContact(caseData, properties, configuration,
                      isLRQmEnabled,
                      isLipQMEnabled);
        addWelshLipContact(caseData, properties, configuration,
                           isLRQmEnabled,
                           isLipQMEnabled);
        return properties;
    }

    public static void addCommonFooterSignature(Map<String, String> properties,
                                                NotificationsSignatureConfiguration configuration) {
        properties.putAll(Map.of(HMCTS_SIGNATURE, configuration.getHmctsSignature(),
                                 PHONE_CONTACT, configuration.getPhoneContact(),
                                 OPENING_HOURS, configuration.getOpeningHours()));
    }

    public static void addCommonFooterSignatureWelsh(Map<String, String> properties,
                                                     NotificationsSignatureConfiguration configuration) {
        properties.putAll(Map.of(WELSH_HMCTS_SIGNATURE, configuration.getWelshHmctsSignature(),
                                 WELSH_PHONE_CONTACT, configuration.getWelshPhoneContact(),
                                 WELSH_OPENING_HOURS, configuration.getWelshOpeningHours()));
    }

    public static void addSpecAndUnspecContact(CaseData caseData, Map<String, String> properties,
                                               NotificationsSignatureConfiguration configuration,
                                               boolean isLRQmEnabled) {
        if (isLRQmEnabled && !queryNotAllowedCaseStates(caseData) && !caseData.isLipCase()) {
            properties.put(SPEC_UNSPEC_CONTACT, RAISE_QUERY_LR);
        } else {
            properties.put(SPEC_UNSPEC_CONTACT, configuration.getSpecUnspecContact());
        }
    }

    public static void addLipContact(CaseData caseData, Map<String, String> properties,
                                     NotificationsSignatureConfiguration configuration,
                                     boolean isLRQmEnabled, boolean isLipQMEnabled) {
        if (isLRQmEnabled
            && !queryNotAllowedCaseStates(caseData)
            && caseData.isLipCase() && isLipQMEnabled) {
            properties.put(SPEC_CONTACT, RAISE_QUERY_LIP);
        } else {
            properties.put(SPEC_CONTACT, configuration.getSpecContact());
        }
    }

    public static void addWelshLipContact(CaseData caseData, Map<String, String> properties,
                                          NotificationsSignatureConfiguration configuration,
                                          boolean isLRQmEnabled, boolean isLipQMEnabled) {
        if (isLRQmEnabled
            && !queryNotAllowedCaseStates(caseData)
            && caseData.isLipCase() && isLipQMEnabled) {
            properties.put(WELSH_CONTACT, RAISE_QUERY_LIP_WELSH);
        } else {
            properties.put(WELSH_CONTACT, configuration.getWelshContact());
        }
    }
}
