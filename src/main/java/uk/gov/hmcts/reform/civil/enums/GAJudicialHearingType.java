package uk.gov.hmcts.reform.civil.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GAJudicialHearingType {

    IN_PERSON("in person"),
    VIDEO("via video"),
    TELEPHONE("via telephone");

    private final String displayedValue;
}
