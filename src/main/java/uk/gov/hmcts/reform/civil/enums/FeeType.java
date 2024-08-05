package uk.gov.hmcts.reform.civil.enums;

import lombok.Getter;

@Getter
public enum FeeType {
    APPLICATION("application", "application"),
    ADDITIONAL("additional", "additional");

    private final String label;
    private final String labelInWelsh;

    FeeType(String label, String labelInWelsh) {
        this.label = label;
        this.labelInWelsh = labelInWelsh;
    }
}
