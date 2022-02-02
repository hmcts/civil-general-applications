package uk.gov.hmcts.reform.civil.callback;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static uk.gov.hmcts.reform.civil.callback.UserType.CAMUNDA;
import static uk.gov.hmcts.reform.civil.callback.UserType.TESTING_SUPPORT;
import static uk.gov.hmcts.reform.civil.callback.UserType.USER;

@Getter
@RequiredArgsConstructor
public enum CaseEvent {
    INITIATE_GENERAL_APPLICATION(USER),

    CREATE_GENERAL_APPLICATION_CASE(CAMUNDA),
    GENERAL_APPLICATION_CREATION(CAMUNDA),
    LINK_GENERAL_APPLICATION_CASE_TO_PARENT_CASE(CAMUNDA),

    DISPATCH_BUSINESS_PROCESS_GASPEC(CAMUNDA),
    END_BUSINESS_PROCESS_GASPEC(CAMUNDA),

    MAKE_PBA_PAYMENT(CAMUNDA),

    UPDATE_CASE_DATA(TESTING_SUPPORT),

    NOTIFY_GENERAL_APPLICATION_RESPONDENT(CAMUNDA);

    private final UserType userType;

    public boolean isCamundaEvent() {
        return this.getUserType() == CAMUNDA;
    }
}
