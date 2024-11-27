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
import uk.gov.hmcts.reform.civil.enums.CaseRole;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.BusinessProcess;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.model.genapplication.GACaseLocation;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.service.*;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.reform.civil.CaseDefinitionConstants.NON_LIVE_STATES;
import static uk.gov.hmcts.reform.civil.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.APPLICATION_UPDATE_AFTER_NOC;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PROCEEDS_IN_HERITAGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationUpdateAfterNocCallbackHandler extends CallbackHandler {

    private final ObjectMapper objectMapper;
    private final CoreCaseDataService coreCaseDataService;
    private final CoreCaseUserService coreCaseUserService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final GaForLipService gaForLipService;

    private static final List<CaseEvent> APPLICATION_UPDATE_AFTER_NOC_EVENTS =
        singletonList(APPLICATION_UPDATE_AFTER_NOC);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
            callbackKey(ABOUT_TO_SUBMIT), this::updateApplicationDetailsAfterNoc
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return APPLICATION_UPDATE_AFTER_NOC_EVENTS;
    }

    private CallbackResponse updateApplicationDetailsAfterNoc(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        String authToken = callbackParams.getParams().get(BEARER_TOKEN).toString();
        CaseData civilCaseData = caseDetailsConverter
            .toCaseData(coreCaseDataService
                            .getCase(Long.parseLong(caseData.getGeneralAppParentCaseLink().getCaseReference())));

        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        if(gaForLipService.isLipApp(caseData) && caseData.getParentClaimantIsApplicant()  == YesOrNo.YES) {
            if (civilCaseData.getApplicantSolicitor1UserDetails() != null) {
                coreCaseUserService.unassignCase(
                    caseData.getCcdCaseReference().toString(),
                    caseData.getClaimantUserDetails().getId(),
                    null,
                    CaseRole.CLAIMANT
                );

                caseDataBuilder.generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().email(civilCaseData.getApplicantSolicitor1UserDetails().getEmail())
                                                             .id(civilCaseData.getApplicantSolicitor1UserDetails().getId()).build());

            }
        } else if (gaForLipService.isLipApp(caseData) && caseData.getParentClaimantIsApplicant()  == YesOrNo.NO) {
            if (civilCaseData.getApplicantSolicitor1UserDetails() != null && ) {

                caseDataBuilder.generalAppApplnSolicitor(GASolicitorDetailsGAspec.builder().email(civilCaseData.getApplicantSolicitor1UserDetails().getEmail())
                                                             .id(civilCaseData.getApplicantSolicitor1UserDetails().getId()).build());

            }
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDataBuilder.build().toMap(objectMapper))
            .build();
    }
}
