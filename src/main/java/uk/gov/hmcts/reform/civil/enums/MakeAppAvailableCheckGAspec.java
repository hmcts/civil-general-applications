package uk.gov.hmcts.reform.civil.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MakeAppAvailableCheckGAspec {
    ConsentAgreementCheckBox("Make application visible to all parties");

    private final String displayedValue;
}

