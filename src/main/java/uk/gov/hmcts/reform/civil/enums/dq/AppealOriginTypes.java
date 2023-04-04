package uk.gov.hmcts.reform.civil.enums.dq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AppealOriginTypes {

    CLAIMANT("Claimant's"),
    DEFENDANT("Defendant's"),
    OTHER("Other");

    private final String displayedValue;
}
