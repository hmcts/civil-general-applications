package uk.gov.hmcts.reform.civil.handler.callback.user;

import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.model.CaseData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GENERATE_DIRECTIONS_ORDER;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudicialFinalDecisionHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(GENERATE_DIRECTIONS_ORDER);
    private final ObjectMapper objectMapper;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
                callbackKey(ABOUT_TO_START), this::setCaseName,
                callbackKey(ABOUT_TO_SUBMIT), this::emptyCallbackResponse
        );
    }

    private CallbackResponse setCaseName(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder()
                .caseNameHmctsInternal(getAllPartyNames(caseData));
        return AboutToStartOrSubmitCallbackResponse.builder()
                .data(caseDataBuilder.build().toMap(objectMapper))
                .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    public static String getAllPartyNames(CaseData caseData) {
        return format("%s V %s%s",
                caseData.getClaimant1PartyName(),
                caseData.getDefendant1PartyName(),
                Objects.nonNull(caseData.getDefendant2PartyName())
                        && NO.equals(caseData.getRespondent2SameLegalRepresentative())
                        ? ", " + caseData.getDefendant2PartyName() : "");
    }
}
