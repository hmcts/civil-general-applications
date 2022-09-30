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
    RESPOND_TO_APPLICATION(USER),

    MAKE_DECISION(USER),
    MAKE_AN_ORDER(USER),
    RESPOND_TO_JUDGE_WRITTEN_REPRESENTATION(USER),
    RESPOND_TO_JUDGE_DIRECTIONS(USER),
    RESPOND_TO_JUDGE_ADDITIONAL_INFO(USER),
    REFER_TO_JUDGE(USER),
    REFER_TO_LEGAL_ADVISOR(USER),

    CREATE_GENERAL_APPLICATION_CASE(CAMUNDA),
    GENERAL_APPLICATION_CREATION(CAMUNDA),
    LINK_GENERAL_APPLICATION_CASE_TO_PARENT_CASE(CAMUNDA),

    START_NOTIFICATION_PROCESS_MAKE_DECISION(CAMUNDA),
    START_BUSINESS_PROCESS_MAKE_DECISION(CAMUNDA),
    CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION(CAMUNDA),
    CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED(CAMUNDA),
    DISPATCH_BUSINESS_PROCESS_GASPEC(CAMUNDA),
    END_BUSINESS_PROCESS_GASPEC(CAMUNDA),
    END_JUDGE_BUSINESS_PROCESS_GASPEC(CAMUNDA),

    ASSIGN_GA_ROLES(CAMUNDA),

    MAKE_PAYMENT_SERVICE_REQ_GASPEC(CAMUNDA),

    MAKE_PBA_PAYMENT_GASPEC(CAMUNDA),

    PBA_PAYMENT_FAILED(CAMUNDA),

    VALIDATE_FEE_GASPEC(CAMUNDA),
    OBTAIN_ADDITIONAL_PAYMENT_REF(CAMUNDA),
    OBTAIN_ADDITIONAL_FEE_VALUE(CAMUNDA),
    UPDATE_CASE_DATA(TESTING_SUPPORT),

    NOTIFY_GA_RESPONDENT(CAMUNDA),

    NOTIFY_GENERAL_APPLICATION_RESPONDENT(CAMUNDA),

    GENERATE_JUDGES_FORM(CAMUNDA),

    UPDATE_CASE_WITH_GA_STATE(CAMUNDA),
    ADDITIONAL_PAYMENT_SUCCESS_CALLBACK(CAMUNDA),
    MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID(CAMUNDA);
    private final UserType userType;

    public boolean isCamundaEvent() {
        return this.getUserType() == CAMUNDA;
    }
}
