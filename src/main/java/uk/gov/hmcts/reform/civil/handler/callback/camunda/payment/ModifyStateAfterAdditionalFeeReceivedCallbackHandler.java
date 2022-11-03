package uk.gov.hmcts.reform.civil.handler.callback.camunda.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.civil.callback.Callback;
import uk.gov.hmcts.reform.civil.callback.CallbackHandler;
import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GASolicitorDetailsGAspec;
import uk.gov.hmcts.reform.civil.service.CoreCaseUserService;
import uk.gov.hmcts.reform.civil.service.ParentCaseUpdateHelper;
import uk.gov.hmcts.reform.civil.service.StateGeneratorService;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORONE;
import static uk.gov.hmcts.reform.civil.enums.CaseRole.RESPONDENTSOLICITORTWO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModifyStateAfterAdditionalFeeReceivedCallbackHandler extends CallbackHandler {

    private final ParentCaseUpdateHelper parentCaseUpdateHelper;
    private final StateGeneratorService stateGeneratorService;
    private final CoreCaseUserService coreCaseUserService;

    private static final int FIRST_SOLICITOR = 0;
    private static final int SECOND_SOLICITOR = 1;

    private static final List<CaseEvent> EVENTS = singletonList(MODIFY_STATE_AFTER_ADDITIONAL_FEE_PAID);

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(
                callbackKey(ABOUT_TO_SUBMIT), this::changeApplicationState,
                callbackKey(SUBMITTED), this::changeGADetailsStatusInParent
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse changeApplicationState(CallbackParams callbackParams) {
        Long caseId = callbackParams.getCaseData().getCcdCaseReference();
        CaseData caseData = callbackParams.getCaseData();
        String newCaseState = stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData).toString();
        log.info("Changing state to {} for caseId: {}", newCaseState, caseId);

        if (caseData.getMakeAppVisibleToRespondents() != null
            || isApplicationUncloakedForRequestMoreInformation(caseData).equals(YES)) {

            if (!CollectionUtils.isEmpty(caseData.getGeneralAppRespondentSolicitors())) {
                GASolicitorDetailsGAspec respondentSolicitor1 =
                    caseData.getGeneralAppRespondentSolicitors().get(FIRST_SOLICITOR).getValue();

                coreCaseUserService
                    .assignCase(caseId.toString(), respondentSolicitor1.getId(),
                                respondentSolicitor1.getOrganisationIdentifier(), RESPONDENTSOLICITORONE);

                if (caseData.getGeneralAppRespondentSolicitors().size() > 1) {

                    GASolicitorDetailsGAspec respondentSolicitor2 =
                        caseData.getGeneralAppRespondentSolicitors().get(SECOND_SOLICITOR).getValue();

                    coreCaseUserService
                        .assignCase(caseId.toString(), respondentSolicitor2.getId(),
                                    respondentSolicitor2.getOrganisationIdentifier(),
                                    RESPONDENTSOLICITORTWO);
                }
            }
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
                .state(newCaseState)
                .build();
    }

    private YesOrNo isApplicationUncloakedForRequestMoreInformation(CaseData caseData) {
        if (caseData.getJudicialDecisionRequestMoreInfo() != null
            && caseData.getJudicialDecisionRequestMoreInfo().getRequestMoreInfoOption() != null
            && caseData.getJudicialDecisionRequestMoreInfo()
            .getRequestMoreInfoOption().equals(SEND_APP_TO_OTHER_PARTY)) {
            return YES;
        }
        return NO;
    }

    private CallbackResponse changeGADetailsStatusInParent(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        String newCaseState = stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData)
            .getDisplayedValue();
        log.info("Updating parent with latest state {} of application-caseId: {}",
                 newCaseState, caseData.getCcdCaseReference());
        parentCaseUpdateHelper.updateParentApplicationVisibilityWithNewState(
                caseData,
                newCaseState
        );

        return SubmittedCallbackResponse.builder().build();
    }

}
