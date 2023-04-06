package uk.gov.hmcts.reform.civil.enums.dq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionToAppealTypes {

    GRANTED("granted"),
    REFUSED("not granted");

    private final String displayedValue;
}
