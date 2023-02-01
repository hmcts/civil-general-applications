package uk.gov.hmcts.reform.civil.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GAJudicialHearingType {

    IN_PERSON("In person"),
    VIDEO("Video"),
    TELEPHONE("Telephone");

    private final String displayedValue;
}
