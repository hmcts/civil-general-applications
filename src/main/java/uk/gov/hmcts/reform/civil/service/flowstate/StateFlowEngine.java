package uk.gov.hmcts.reform.civil.service.flowstate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.stateflow.StateFlow;
import uk.gov.hmcts.reform.civil.stateflow.StateFlowBuilder;
import uk.gov.hmcts.reform.civil.stateflow.model.State;

import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.judgeMadeDecision;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.judgeMadeDirections;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.judgeMadeListingForHearing;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.judgeMadeWrittenRep;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.judgeRequestAdditionalInfo;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.judgeMadeOrder;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.paymentSuccess;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.withNoticeApplication;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowPredicate.withOutNoticeApplication;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.ADDITIONAL_INFO;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.APPLICATION_SUBMITTED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.APPLICATION_SUBMITTED_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.DRAFT;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.FLOW_NAME;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.JUDGE_DIRECTIONS;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.JUDGE_WRITTEN_REPRESENTATION;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.LISTED_FOR_HEARING;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.PROCEED_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.ORDER_MADE;

@Component
@RequiredArgsConstructor
public class StateFlowEngine {

    private final CaseDetailsConverter caseDetailsConverter;

    public StateFlow build() {
        return StateFlowBuilder.<FlowState.Main>flow(FLOW_NAME)
            .initial(DRAFT)
            .transitionTo(APPLICATION_SUBMITTED)
                .onlyIf((withNoticeApplication.or(withOutNoticeApplication)))
            .state(APPLICATION_SUBMITTED)
                .transitionTo(PROCEED_GENERAL_APPLICATION)
                    .onlyIf(paymentSuccess)
            .state(PROCEED_GENERAL_APPLICATION)
                .transitionTo(APPLICATION_SUBMITTED_JUDICIAL_DECISION)
                    .onlyIf(judgeMadeDecision)
            .state(APPLICATION_SUBMITTED_JUDICIAL_DECISION)
                .transitionTo(LISTED_FOR_HEARING).onlyIf(judgeMadeListingForHearing)
                .transitionTo(ADDITIONAL_INFO).onlyIf(judgeRequestAdditionalInfo)
                .transitionTo(JUDGE_DIRECTIONS).onlyIf(judgeMadeDirections)
                .transitionTo(JUDGE_WRITTEN_REPRESENTATION).onlyIf(judgeMadeWrittenRep)
            .transitionTo(ORDER_MADE).onlyIf(judgeMadeOrder)
            .state(LISTED_FOR_HEARING)
            .state(ADDITIONAL_INFO)
            .state(JUDGE_DIRECTIONS)
            .state(JUDGE_WRITTEN_REPRESENTATION)
            .state(ORDER_MADE)
            .build();
    }

    public StateFlow evaluate(CaseDetails caseDetails) {
        return evaluate(caseDetailsConverter.toCaseData(caseDetails));
    }

    public StateFlow evaluate(CaseData caseData) {
        return build().evaluate(caseData);
    }

    public boolean hasTransitionedTo(CaseDetails caseDetails, FlowState.Main state) {
        return evaluate(caseDetails).getStateHistory().stream().map(State::getName)
            .anyMatch(name -> name.equals(state.fullName()));
    }

}
