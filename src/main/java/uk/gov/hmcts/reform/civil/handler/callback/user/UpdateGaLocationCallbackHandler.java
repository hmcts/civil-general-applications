package uk.gov.hmcts.reform.civil.handler.callback.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GACaseLocation;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.TRIGGER_LOCATION_UPDATE;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateGaLocationCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(TRIGGER_LOCATION_UPDATE);
    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final ObjectMapper objectMapper;

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::updateCaseManagementLocation
        );
    }

    private CallbackResponse updateCaseManagementLocation(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData civilCaseData = caseDetailsConverter
            .toCaseData(coreCaseDataService
                            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();
        caseDataBuilder
            .businessProcess(
                BusinessProcess.builder()
                    .camundaEvent(TRIGGER_LOCATION_UPDATE.name())
                    .status(BusinessProcessStatus.FINISHED)
                    .build())
            .isCcmccLocation(YesOrNo.NO)
            .caseManagementLocation(GACaseLocation.builder().baseLocation(civilCaseData.getCaseManagementLocation()
                                                                              .getBaseLocation())
                                        .region(civilCaseData.getCaseManagementLocation().getRegion())
                                        .siteName(civilCaseData.getCaseManagementLocation().getSiteName())
                                        .build())
            .locationName(civilCaseData.getCaseManagementLocation().getSiteName());
        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }
}
