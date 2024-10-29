package uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications;

import static uk.gov.hmcts.reform.civil.callback.CaseEvent.CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_TRANSLATED_DOC;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.handler.callback.camunda.dashboardnotifications.DashboardScenarios.SCENARIO_AAA6_GENERAL_APPS_TRANSLATED_DOCUMENT_UPLOADED_RESPONDENT;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.callback.DashboardCallbackHandler;
import uk.gov.hmcts.reform.civil.client.DashboardApiClient;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.service.DashboardNotificationsParamsMapper;

@Service
public class CreateDashboardNotificationUploadTranslatedDocumentRespondentHandler extends DashboardCallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(CREATE_RESPONDENT_DASHBOARD_NOTIFICATION_TRANSLATED_DOC);

    public CreateDashboardNotificationUploadTranslatedDocumentRespondentHandler(DashboardApiClient dashboardApiClient,
                                                                               DashboardNotificationsParamsMapper mapper,
                                                                               FeatureToggleService featureToggleService) {
        super(dashboardApiClient, mapper, featureToggleService);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    @Override
    public String getScenario(CaseData caseData) {
        return SCENARIO_AAA6_GENERAL_APPS_TRANSLATED_DOCUMENT_UPLOADED_RESPONDENT.getScenario();
    }

    @Override
    public boolean shouldRecordScenario(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        return (caseData.getIsGaRespondentOneLip() == YesOrNo.YES
            && ((caseData.getGeneralAppInformOtherParty() != null && caseData.getGeneralAppInformOtherParty().getIsWithNotice() == YES)
            || caseData.getGeneralAppConsentOrder() == YES));
    }
}
