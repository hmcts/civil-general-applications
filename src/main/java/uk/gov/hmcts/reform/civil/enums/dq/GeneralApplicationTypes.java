package uk.gov.hmcts.reform.civil.enums.dq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GeneralApplicationTypes {
    STRIKE_OUT("Strike out"),
    SUMMARY_JUDGEMENT("Summary judgment"),
    STAY_THE_CLAIM("Stay the claim"),
    EXTEND_TIME("Extend time");

    private final String displayedValue;
}
