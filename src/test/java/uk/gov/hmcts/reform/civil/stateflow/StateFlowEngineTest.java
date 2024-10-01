package uk.gov.hmcts.reform.civil.stateflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.civil.enums.PaymentStatus;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption;
import uk.gov.hmcts.reform.civil.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.civil.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.PaymentDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GAInformOtherParty;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudgesHearingListGAspec;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAPbaDetails;
import uk.gov.hmcts.reform.civil.model.genapplication.GARespondentOrderAgreement;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.flowstate.StateFlowEngine;
import uk.gov.hmcts.reform.civil.stateflow.model.State;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.enums.YesOrNo.YES;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.LIST_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_AN_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.ADDITIONAL_INFO;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.APPLICATION_SUBMITTED;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.APPLICATION_SUBMITTED_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.DRAFT;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.JUDGE_DIRECTIONS;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.JUDGE_WRITTEN_REPRESENTATION;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.LISTED_FOR_HEARING;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.PROCEED_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.civil.service.flowstate.FlowState.Main.ORDER_MADE;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    StateFlowEngine.class
})

public class StateFlowEngineTest {

    @Autowired
    private StateFlowEngine stateFlowEngine;

    @MockBean
    private FeatureToggleService featureToggleService;

    @Test
    void shouldReturnApplicationSubmittedWhenPBAPaymentIsFailed() {
        CaseData caseData = CaseDataBuilder.builder().buildPaymentFailureCaseData();
        StateFlow stateFlow = stateFlowEngine.evaluate(caseData);

        assertThat(stateFlow.getState()).extracting(State::getName).isNotNull()
            .isEqualTo(APPLICATION_SUBMITTED.fullName());

        assertThat(stateFlow.getStateHistory()).hasSize(2)
            .extracting(State::getName)
            .containsExactly(DRAFT.fullName(), APPLICATION_SUBMITTED.fullName());
    }

    @Test
    void shouldReturnApplicationSubmittedWhenPBAPaymentIsSuccess() {
        CaseData caseData = CaseDataBuilder.builder().withNoticeCaseData();
        StateFlow stateFlow = stateFlowEngine.evaluate(caseData);

        assertThat(stateFlow.getState()).extracting(State::getName).isNotNull()
            .isEqualTo(PROCEED_GENERAL_APPLICATION.fullName());

        assertThat(stateFlow.getStateHistory()).hasSize(3)
            .extracting(State::getName)
            .containsExactly(DRAFT.fullName(), APPLICATION_SUBMITTED.fullName(),
                             PROCEED_GENERAL_APPLICATION.fullName());
    }

    @Test
    void shouldReturn_ApplicationSubmitted_JudicialDecision_WhenJudgeMadeDecision() {
        CaseData caseData = CaseDataBuilder.builder()
            .generalOrderApplication()
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .paymentDetails(PaymentDetails.builder()
                                        .status(PaymentStatus.SUCCESS)
                                        .build()).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YES).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                               .hasAgreed(YES).build()).build();
        StateFlow stateFlow = stateFlowEngine.evaluate(caseData);

        assertThat(stateFlow.getState()).extracting(State::getName).isNotNull()
            .isEqualTo(ORDER_MADE.fullName());

        assertThat(stateFlow.getStateHistory()).hasSize(5)
            .extracting(State::getName)
            .containsExactly(DRAFT.fullName(), APPLICATION_SUBMITTED.fullName(),
                             PROCEED_GENERAL_APPLICATION.fullName(),
                             APPLICATION_SUBMITTED_JUDICIAL_DECISION.fullName(),
                             ORDER_MADE.fullName());
    }

    @Test
    void shouldReturn_ApplicationSubmitted_JudicialDecision_WhenJudgeMadeDecisionFreeformOrder() {
        CaseData caseData = CaseDataBuilder.builder()
            .generalOrderFreeFormApplication()
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .paymentDetails(PaymentDetails.builder()
                                        .status(PaymentStatus.SUCCESS)
                                        .build()).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YES).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                               .hasAgreed(YES).build()).build();
        StateFlow stateFlow = stateFlowEngine.evaluate(caseData);

        assertThat(stateFlow.getState()).extracting(State::getName).isNotNull()
            .isEqualTo(ORDER_MADE.fullName());

        assertThat(stateFlow.getStateHistory()).hasSize(5)
            .extracting(State::getName)
            .containsExactly(DRAFT.fullName(), APPLICATION_SUBMITTED.fullName(),
                             PROCEED_GENERAL_APPLICATION.fullName(),
                             APPLICATION_SUBMITTED_JUDICIAL_DECISION.fullName(),
                             ORDER_MADE.fullName());
    }

    @Test
    void shouldReturn_WhenJudgeMadeFinalOrder() {
        CaseData caseData = CaseDataBuilder.builder()
            .judgeFinalOrderApplication()
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .paymentDetails(PaymentDetails.builder()
                                        .status(PaymentStatus.SUCCESS)
                                        .build()).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YES).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                               .hasAgreed(YES).build()).build();
        StateFlow stateFlow = stateFlowEngine.evaluate(caseData);

        assertThat(stateFlow.getState()).extracting(State::getName).isNotNull()
            .isEqualTo(ORDER_MADE.fullName());

        assertThat(stateFlow.getStateHistory()).hasSize(5)
            .extracting(State::getName)
            .containsExactly(DRAFT.fullName(), APPLICATION_SUBMITTED.fullName(),
                             PROCEED_GENERAL_APPLICATION.fullName(),
                             APPLICATION_SUBMITTED_JUDICIAL_DECISION.fullName(),
                             ORDER_MADE.fullName());
    }

    @Test
    void shouldReturn_Judge_Written_Rep_WhenJudgeMadeDecision() {
        CaseData caseData = CaseDataBuilder.builder()
            .writtenRepresentationSequentialApplication()
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .paymentDetails(PaymentDetails.builder()
                                        .status(PaymentStatus.SUCCESS)
                                        .build()).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YES).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                               .hasAgreed(YES).build()).build();
        StateFlow stateFlow = stateFlowEngine.evaluate(caseData);

        assertThat(stateFlow.getState()).extracting(State::getName).isNotNull()
            .isEqualTo(JUDGE_WRITTEN_REPRESENTATION.fullName());

        assertThat(stateFlow.getStateHistory()).hasSize(5)
            .extracting(State::getName)
            .containsExactly(DRAFT.fullName(), APPLICATION_SUBMITTED.fullName(),
                             PROCEED_GENERAL_APPLICATION.fullName(),
                             APPLICATION_SUBMITTED_JUDICIAL_DECISION.fullName(),
                             JUDGE_WRITTEN_REPRESENTATION.fullName());
    }

    @Test
    void shouldReturn_Judge_Order_Made_WhenJudgeMadeDecision() {
        CaseData caseData = CaseDataBuilder.builder()
            .approveApplication()
            .generalAppPBADetails(
                GAPbaDetails.builder()
                    .paymentDetails(PaymentDetails.builder()
                                        .status(PaymentStatus.SUCCESS)
                                        .build()).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YES).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                               .hasAgreed(YES).build()).build();
        StateFlow stateFlow = stateFlowEngine.evaluate(caseData);

        assertThat(stateFlow.getState()).extracting(State::getName).isNotNull()
            .isEqualTo(ORDER_MADE.fullName());

        assertThat(stateFlow.getStateHistory()).hasSize(5)
            .extracting(State::getName)
            .containsExactly(DRAFT.fullName(), APPLICATION_SUBMITTED.fullName(),
                             PROCEED_GENERAL_APPLICATION.fullName(),
                             APPLICATION_SUBMITTED_JUDICIAL_DECISION.fullName(),
                             ORDER_MADE.fullName());
    }

    @Test
    void shouldReturn_Listed_For_Hearing_WhenJudgeMadeDecision() {
        CaseData caseData = CaseDataBuilder.builder().buildPaymentSuccessfulCaseData().toBuilder()
            .judicialDecision(GAJudicialDecision.builder().decision(LIST_FOR_A_HEARING).build())
            .judicialListForHearing(GAJudgesHearingListGAspec.builder().build())
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YES).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                               .hasAgreed(YES).build()).build();
        StateFlow stateFlow = stateFlowEngine.evaluate(caseData);

        assertThat(stateFlow.getState()).extracting(State::getName).isNotNull()
            .isEqualTo(LISTED_FOR_HEARING.fullName());

        assertThat(stateFlow.getStateHistory()).hasSize(5)
            .extracting(State::getName)
            .containsExactly(DRAFT.fullName(), APPLICATION_SUBMITTED.fullName(),
                             PROCEED_GENERAL_APPLICATION.fullName(),
                             APPLICATION_SUBMITTED_JUDICIAL_DECISION.fullName(),
                             LISTED_FOR_HEARING.fullName());
    }

    @Test
    void shouldReturn_Additional_Info_WhenJudgeMadeDecision() {
        CaseData caseData = CaseDataBuilder.builder().buildPaymentSuccessfulCaseData().toBuilder()
            .judicialDecision(GAJudicialDecision.builder().decision(REQUEST_MORE_INFO).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YES).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                               .hasAgreed(YES).build()).build();
        StateFlow stateFlow = stateFlowEngine.evaluate(caseData);

        assertThat(stateFlow.getState()).extracting(State::getName).isNotNull()
            .isEqualTo(ADDITIONAL_INFO.fullName());

        assertThat(stateFlow.getStateHistory()).hasSize(5)
            .extracting(State::getName)
            .containsExactly(DRAFT.fullName(), APPLICATION_SUBMITTED.fullName(),
                             PROCEED_GENERAL_APPLICATION.fullName(),
                             APPLICATION_SUBMITTED_JUDICIAL_DECISION.fullName(),
                             ADDITIONAL_INFO.fullName());
    }

    @Test
    void shouldReturn_Judge_Directions_WhenJudgeMadeDecision() {
        CaseData caseData = CaseDataBuilder.builder().buildPaymentSuccessfulCaseData().toBuilder()
            .judicialDecision(GAJudicialDecision.builder().decision(MAKE_AN_ORDER).build())
            .judicialDecisionMakeOrder(
                GAJudicialMakeAnOrder.builder().makeAnOrder(
                    GAJudgeMakeAnOrderOption.GIVE_DIRECTIONS_WITHOUT_HEARING).build())
            .generalAppInformOtherParty(GAInformOtherParty.builder()
                                            .isWithNotice(YES).build())
            .generalAppRespondentAgreement(GARespondentOrderAgreement.builder()
                                               .hasAgreed(YES).build()).build();
        StateFlow stateFlow = stateFlowEngine.evaluate(caseData);

        assertThat(stateFlow.getState()).extracting(State::getName).isNotNull()
            .isEqualTo(JUDGE_DIRECTIONS.fullName());

        assertThat(stateFlow.getStateHistory()).hasSize(5)
            .extracting(State::getName)
            .containsExactly(DRAFT.fullName(), APPLICATION_SUBMITTED.fullName(),
                             PROCEED_GENERAL_APPLICATION.fullName(),
                             APPLICATION_SUBMITTED_JUDICIAL_DECISION.fullName(),
                             JUDGE_DIRECTIONS.fullName());
    }
}
