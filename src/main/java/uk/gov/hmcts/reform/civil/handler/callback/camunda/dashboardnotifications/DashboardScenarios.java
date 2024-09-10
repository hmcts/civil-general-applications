package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import lombok.Getter;

@Getter
public enum DashboardScenarios {

    SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_CLAIMANT("Scenario.AAA6.GeneralApplication.Created.Claimant"),
    SCENARIO_AAA6_GENERAL_APPLICATION_CREATED_DEFENDANT("Scenario.AAA6.GeneralApplication.Created.Defendant"),
    SCENARIO_AAA6_GENERAL_APPLICATION_COMPLETE_CLAIMANT("Scenario.AAA6.GeneralApplication.Complete.Claimant"),
    SCENARIO_AAA6_GENERAL_APPLICATION_COMPLETE_DEFENDANT("Scenario.AAA6.GeneralApplication.Complete.Defendant"),
    SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_NONURGENT_RESPONDENT("Scenario.AAA6.GeneralApps.NonUrgentApplicationMade.Respondent"),
    SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_URGENT_RESPONDENT("Scenario.AAA6.GeneralApps.UrgentApplicationMade.Respondent"),
    SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_NONURGENT_UNCLOAKED_RESPONDENT("Scenario.AAA6.GeneralApps.NonUrgentApplicationUncloaked.Respondent"),
    SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_URGENT_UNCLOAKED_RESPONDENT("Scenario.AAA6.GeneralApps.UrgentApplicationUncloaked.Respondent"),
    SCENARIO_AAA6_GENERAL_APPLICATION_SUBMITTED_DELETE_RESPONDENT("Scenario.AAA6.GeneralApps.ApplicationMade.Delete.Respondent");

    private final String scenario;

    DashboardScenarios(String scenario) {
        this.scenario = scenario;
    }

    public String getScenario() {
        return scenario;
    }
}
