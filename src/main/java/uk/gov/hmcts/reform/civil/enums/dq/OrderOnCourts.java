package uk.gov.hmcts.reform.civil.enums.dq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderOnCourts {
    ORDER_ON_COURT_INITIATIVE("Order on court's own initiative"),
    ORDER_WITHOUT_NOTICE("Order without notice"),
    NONE("None");

    private final String displayedValue;
}
