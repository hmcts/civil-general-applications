package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import lombok.Getter;

@Getter
public enum DashboardScenarios {

    SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_CLAIMANT("Scenario.AAA6.GeneralApplication.Created.Claimant"),
    SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_DEFENDANT("Scenario.AAA6.GeneralApplication.Created.Defendant"),
    SCENARIO_AAA6_GENERAL_APPLICATION_COMPLETE_CLAIMANT("Scenario.AAA6.GeneralApplication.Complete.Claimant"),
    SCENARIO_AAA6_GENERAL_APPLICATION_COMPLETE_DEFENDANT("Scenario.AAA6.GeneralApplication.Complete.Defendant"),
    SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_APPLICANT("Scenario.AAA6.GeneralApps.ApplicationSubmitted.Applicant"),
    SCENARIO_OTHER_PARTY_UPLOADED_DOC_APPLICANT("Scenario.AAA6.GeneralApps.OtherPartyUploadedDocuments.Applicant"),
    SCENARIO_OTHER_PARTY_UPLOADED_DOC_RESPONDENT("Scenario.AAA6.GeneralApps.OtherPartyUploadedDocuments.Respondent");

    private final String scenario;

    DashboardScenarios(String scenario) {
        this.scenario = scenario;
    }

    public String getScenario() {
        return scenario;
    }
}
