package uk.gov.hmcts.reform.civil.enums;

import lombok.Getter;

@Getter
public enum CaseRole {
    CREATOR,
    APPLICANTSOLICITORONE,
    RESPONDENTSOLICITORONE,
    RESPONDENTSOLICITORTWO,
    APPLICANT,
    RESPONDENT;

    private String formattedName;

    CaseRole() {
        this.formattedName = String.format("[%s]", name());
    }
}
