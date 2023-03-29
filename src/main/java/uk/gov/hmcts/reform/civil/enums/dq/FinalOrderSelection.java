package uk.gov.hmcts.reform.civil.enums.dq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FinalOrderSelection {
    ASSISTED_ORDER("Assisted order"),
    FREE_FORM_ORDER("Free form order");

    private final String displayedValue;
}
