package uk.gov.hmcts.reform.civil.enums.dq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FinalOrderConsideredToggle {
    CONSIDERED("The judge considered the papers");
    private final String displayedValue;
}
