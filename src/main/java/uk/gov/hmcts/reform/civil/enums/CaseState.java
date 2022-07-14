package uk.gov.hmcts.reform.civil.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CaseState {
    //Parent Case states needed to create Pojo at application start
    PENDING_CASE_ISSUED("Pending case issued"),
    CASE_ISSUED("Case issued"),
    AWAITING_CASE_DETAILS_NOTIFICATION("Awaiting case details notofication"),
    AWAITING_RESPONDENT_ACKNOWLEDGEMENT("Awaiting respondent acknowledgement"),
    CASE_DISMISSED("Case Dismissed"),
    AWAITING_APPLICANT_INTENTION("Awaiting applicant intention"),
    PROCEEDS_IN_HERITAGE_SYSTEM("Proceeds in heritage system"),
    PROCEEDS_IN_HERITAGE("Proceeds in heritage system"),

    //General Application states
    PENDING_APPLICATION_ISSUED("General Application Issue Pending"),
    AWAITING_RESPONDENT_RESPONSE("Awaiting Respondent Response"),
    APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION("Application Submitted - Awaiting Judicial Decision"),
    ADDITIONAL_RESPONSE_TIME_EXPIRED("Additional Response Time Expired"),
    ADDITIONAL_RESPONSE_TIME_PROVIDED("Additional Response Time Provided"),
    AWAITING_DIRECTIONS_ORDER_DOCS("Directions Order Made"),
    ORDER_MADE("Order Made"),
    LISTING_FOR_A_HEARING("Listed for a Hearing"),
    AWAITING_WRITTEN_REPRESENTATIONS("Awaiting Written Representations"),
    AWAITING_ADDITIONAL_INFORMATION("Additional Information Required"),
    APPLICATION_DISMISSED("Application Dismissed"),
    APPLICATION_CLOSED("Application Closed"),
    RESPOND_TO_JUDGE_WRITTEN_REPRESENTATION("Respond to judge for Written Representations");

    private final String displayedValue;
}

