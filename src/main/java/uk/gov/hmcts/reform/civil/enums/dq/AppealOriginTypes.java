package uk.gov.hmcts.reform.civil.enums.dq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AppealOriginTypes {

    CLAIMANT("claimant"),
    DEFENDANT("defendant"),
    OTHER("other");

    private final String displayedValue;
}
