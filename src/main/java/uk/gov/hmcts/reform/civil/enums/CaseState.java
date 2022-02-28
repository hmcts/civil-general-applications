package uk.gov.hmcts.reform.civil.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CaseState {
    PENDING_CASE_ISSUED("Pending case issued"),
    PENDING_APPLICATION_ISSUED("General Application Issue Pending"),
    AWAITING_RESPONDENT_RESPONSE("Awaiting Respondent Response"),
    APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION("Application Submitted - Awaiting Judicial Decision"),
    ADDITIONAL_RESPONSE_TIME_EXPIRED("Additional Response Time Expired"),
    ADDITIONAL_RESPONSE_TIME_PROVIDED("Additional Response Time Provided"),
    AWAITING_DIRECTIONS_ORDER_DOCS("Directions Order Made"),
    ORDER_MADE("Order Made"),
    LISTING_FOR_A_HEARING("Listed for a Hearing"),
    AWAITING_WRITTEN_REPRESENTATIONS("Written Representations Required"),
    AWAITING_ADDITIONAL_INFORMATION("Additional Information Required"),
    APPLICATION_DISMISSED("Application Dismissed"),
    APPLICATION_CLOSED("Application Closed");

    private final String displayedValue;
}

