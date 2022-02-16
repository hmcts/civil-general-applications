package uk.gov.hmcts.reform.civil.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CaseState {
    PENDING_CASE_ISSUED("Pending case issued"),
    CASE_ISSUED("Case issued"),
    AWAITING_CASE_DETAILS_NOTIFICATION("Awaiting case details notofication"),
    AWAITING_RESPONDENT_ACKNOWLEDGEMENT("Awaiting respondent acknowledgement"),
    CASE_DISMISSED("Case Dismissed"),
    AWAITING_APPLICANT_INTENTION("Awaiting applicant intention"),
    PROCEEDS_IN_HERITAGE_SYSTEM("Proceeds in heritage system");

    private final String displayedValue;
}

