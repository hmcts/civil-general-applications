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
    RESPOND_TO_APPLICATION_URGENT_LIP(USER),
    MAKE_DECISION(USER),

    HEARING_SCHEDULED_GA(USER),
    GENERATE_DIRECTIONS_ORDER(USER),
    RESPOND_TO_JUDGE_WRITTEN_REPRESENTATION(USER),
    RESPOND_TO_JUDGE_DIRECTIONS(USER),
    RESPOND_TO_JUDGE_ADDITIONAL_INFO(USER),
    REFER_TO_JUDGE(USER),
    REFER_TO_LEGAL_ADVISOR(USER),
    MAIN_CASE_CLOSED(USER),
    APPLICATION_PROCEEDS_IN_HERITAGE(USER),

    CREATE_GENERAL_APPLICATION_CASE(CAMUNDA),
    GENERAL_APPLICATION_CREATION(CAMUNDA),
    LINK_GENERAL_APPLICATION_CASE_TO_PARENT_CASE(CAMUNDA),

    START_APPLICANT_NOTIFICATION_PROCESS_MAKE_DECISION(CAMUNDA),
    START_RESPONDENT_NOTIFICATION_PROCESS_MAKE_DECISION(CAMUNDA),
    START_GA_BUSINESS_PROCESS(CAMUNDA),
    START_HEARING_SCHEDULED_BUSINESS_PROCESS(CAMUNDA),
    CHANGE_STATE_TO_AWAITING_JUDICIAL_DECISION(CAMUNDA),
    CHANGE_STATE_TO_ADDITIONAL_RESPONSE_TIME_EXPIRED(CAMUNDA),
    END_SCHEDULER_CHECK_STAY_ORDER_DEADLINE(CAMUNDA),
    DISPATCH_BUSINESS_PROCESS_GASPEC(CAMUNDA),
    END_BUSINESS_PROCESS_GASPEC(CAMUNDA),
    END_JUDGE_BUSINESS_PROCESS_GASPEC(CAMUNDA),
    END_HEARING_SCHEDULED_PROCESS_GASPEC(CAMUNDA),
    GA_EVIDENCE_UPLOAD_CHECK(CAMUNDA),
    END_GA_HWF_NOTIFY_PROCESS(CAMUNDA),
    END_DOC_UPLOAD_BUSINESS_PROCESS_GASPEC(CAMUNDA),

    ASSIGN_GA_ROLES(CAMUNDA),

    MAKE_PAYMENT_SERVICE_REQ_GASPEC(CAMUNDA),

    MAKE_PBA_PAYMENT_GASPEC(CAMUNDA),

    PBA_PAYMENT_FAILED(CAMUNDA),

    VALIDATE_FEE_GASPEC(CAMUNDA),
    OBTAIN_ADDITIONAL_PAYMENT_REF(CAMUNDA),
    OBTAIN_ADDITIONAL_FEE_VALUE(CAMUNDA),
    UPDATE_CASE_DATA(TESTING_SUPPORT),
    UPDATE_GA_CASE_DATA(TESTING_SUPPORT),
    GENERATE_DRAFT_DOCUMENT(CAMUNDA),
    WAIT_GA_DRAFT(CAMUNDA),

    NOTIFY_GA_RESPONDENT(CAMUNDA),

    NOTIFY_GENERAL_APPLICATION_RESPONDENT(CAMUNDA),

    GENERATE_JUDGES_FORM(CAMUNDA),

    UPDATE_CASE_WITH_GA_STATE(CAMUNDA),
    ADDITIONAL_PAYMENT_SUCCESS_CALLBACK(CAMUNDA),
    MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID(CAMUNDA),
    GENERATE_HEARING_NOTICE_DOCUMENT(CAMUNDA),
    NOTIFY_HEARING_NOTICE_CLAIMANT(CAMUNDA),
    NOTIFY_HEARING_NOTICE_DEFENDANT(CAMUNDA),
    END_SCHEDULER_CHECK_UNLESS_ORDER_DEADLINE(CAMUNDA),
    INITIATE_GENERAL_APPLICATION_AFTER_PAYMENT(USER),
    migrateCase(CAMUNDA), // NOSONAR
    APPROVE_CONSENT_ORDER(USER),
    TRIGGER_LOCATION_UPDATE(USER),
    TRIGGER_TASK_RECONFIG(USER),

    UPLOAD_ADDL_DOCUMENTS(USER),
    CREATE_SERVICE_REQUEST_CUI_GENERAL_APP(CAMUNDA),
    CITIZEN_GENERAL_APP_PAYMENT(USER),
    INVALID_HWF_REFERENCE_GA(USER),
    NO_REMISSION_HWF_GA(USER),
    MORE_INFORMATION_HWF_GA(USER),
    FULL_REMISSION_HWF_GA(USER),
    PARTIAL_REMISSION_HWF_GA(USER),
    UPDATE_HELP_WITH_FEE_NUMBER_GA(USER),
    FEE_PAYMENT_OUTCOME_GA(USER),
    UPDATE_CLAIMANT_TASK_LIST_GA_CREATED(CAMUNDA),
    UPDATE_RESPONDENT_TASK_LIST_GA_CREATED(CAMUNDA),
    UPDATE_CLAIMANT_TASK_LIST_GA_COMPLETE(CAMUNDA),
    UPDATE_RESPONDENT_TASK_LIST_GA_COMPLETE(CAMUNDA),
    UPLOAD_TRANSLATED_DOCUMENT_GA_LIP(CAMUNDA),
    NOTIFY_RESPONDENT_TRANSLATED_DOCUMENT_UPLOADED_GA(CAMUNDA),
    NOTIFY_APPLICANT_TRANSLATED_DOCUMENT_UPLOADED_GA(CAMUNDA),
    NOTIFY_APPLICANT_LIP_HWF(CAMUNDA),
    NOTIFY_HELP_WITH_FEE(USER),
    UPDATE_GA_ADD_HWF(CAMUNDA),
    CREATE_APPLICATION_SUBMITTED_DASHBOARD_NOTIFICATION_FOR_RESPONDENT(CAMUNDA),
    RESPONDENT_RESPONSE_DEADLINE_CHECK(USER),
    CREATE_DASHBOARD_NOTIFICATION_FOR_GA_APPLICANT(CAMUNDA),
    UPDATE_GA_DASHBOARD_NOTIFICATION(CAMUNDA),
    CREATE_APPLICANT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION(CAMUNDA),
    CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_FOR_MAKE_DECISION(CAMUNDA),
    APPLICANT_LIP_HWF_DASHBOARD_NOTIFICATION(CAMUNDA),
    UPLOAD_TRANSLATED_DOCUMENT(USER);

    private final UserType userType;

    public boolean isCamundaEvent() {
        return this.getUserType() == CAMUNDA;
    }
}
