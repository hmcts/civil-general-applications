package uk.gov.hmcts.reform.civil.handler.callback.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.utils.CaseMigrationUtil;

import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.migrateCase;

@Slf4j
@Service
@RequiredArgsConstructor
public class MigrateGaCaseDataCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(migrateCase);

    private static final String MIGRATION_ID_VALUE = "GACaseProgressionMigration";
    private final ObjectMapper objectMapper;

    private final CaseMigrationUtil caseMigrationUtil;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::migrateCaseData
        );
    }

    private CallbackResponse migrateCaseData(CallbackParams callbackParams) {

        CaseData oldCaseData = callbackParams.getCaseData();
        log.info("Migrating data for case: {}", oldCaseData.getCcdCaseReference());
        CaseData.CaseDataBuilder caseDataBuilder = oldCaseData.toBuilder();
        if(oldCaseData.getIsCaseProgressionEnabled() == null) {
            caseDataBuilder.migrationId(MIGRATION_ID_VALUE);
            caseMigrationUtil.migrateGaCaseProgression(
                caseDataBuilder,
                YesOrNo.YES
            );
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

}
