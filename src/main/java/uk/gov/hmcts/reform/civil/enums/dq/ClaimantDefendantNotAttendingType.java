package uk.gov.hmcts.reform.civil.enums.dq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClaimantDefendantNotAttendingType {

    SATISFIED_REASONABLE_TO_PROCEED("Satisfied reasonable to proceed"),
    SATISFIED_NOTICE_OF_TRIAL("Satisfied notice of trial received, not reasonable to ..."),
    NOT_SATISFIED_NOTICE_OF_TRIAL("Not satisfied notice of trial received, not reasonable to ...");

    private final String displayedValue;
}
