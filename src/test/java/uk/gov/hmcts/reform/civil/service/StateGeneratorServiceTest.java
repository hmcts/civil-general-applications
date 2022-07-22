package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.enums.dq.GeneralApplicationTypes;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.model.genapplication.GAApplicationType;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialDecision;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialMakeAnOrder;
import uk.gov.hmcts.reform.civil.model.genapplication.GAJudicialRequestMoreInfo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_ADD_PAYMENT;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_DISMISSED;
import static uk.gov.hmcts.reform.civil.enums.CaseState.APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_ADDITIONAL_INFORMATION;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_DIRECTIONS_ORDER_DOCS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.CaseState.LISTING_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.CaseState.ORDER_MADE;
import static uk.gov.hmcts.reform.civil.enums.CaseState.PROCEEDS_IN_HERITAGE;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.LIST_FOR_A_HEARING;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_AN_ORDER;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeDecisionOption.REQUEST_MORE_INFO;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.APPROVE_OR_EDIT;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeMakeAnOrderOption.DISMISS_THE_APPLICATION;
import static uk.gov.hmcts.reform.civil.enums.dq.GAJudgeRequestMoreInfoOption.SEND_APP_TO_OTHER_PARTY;

@SpringBootTest(classes =
    StateGeneratorService.class
)
public class StateGeneratorServiceTest {

    @Autowired
    StateGeneratorService stateGeneratorService;

    @Test
    public void shouldReturnAwaiting_Addition_InformationWhenMoreInfoSelected() {
        CaseData caseData = CaseData.builder()
            .judicialDecision(new GAJudicialDecision(REQUEST_MORE_INFO))
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().orderText("test").build())
            .build();

        CaseState caseState = stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData);

        assertThat(caseState).isEqualTo(AWAITING_ADDITIONAL_INFORMATION);
    }

    @Test
    public void shouldReturn_Awaiting_Written_Representation_WhenMakeOrderForWrittenRepresentationsSelected() {
        CaseData caseData = CaseData.builder()
            .judicialDecision(new GAJudicialDecision(MAKE_ORDER_FOR_WRITTEN_REPRESENTATIONS))
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().orderText("test").build())
            .build();

        CaseState caseState = stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData);

        assertThat(caseState).isEqualTo(AWAITING_WRITTEN_REPRESENTATIONS);
    }

    @Test
    public void shouldReturn_Awaiting_Directions_Order_Docs_WhenMakeOrderSelectedAndTextProvided() {
        CaseData caseData = CaseData.builder()
            .ccdState(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .judicialDecision(new GAJudicialDecision(MAKE_AN_ORDER))
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().directionsText("test").build())
            .build();

        CaseState caseState = stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData);

        assertThat(caseState).isEqualTo(AWAITING_DIRECTIONS_ORDER_DOCS);
    }

    @Test
    public void shouldReturnCurrentStateWhenMakeOrderSelectedNoTextProvided() {
        CaseData caseData = CaseData.builder()
            .ccdState(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .judicialDecision(new GAJudicialDecision(MAKE_AN_ORDER))
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().directionsText("").build())

            .build();

        CaseState caseState = stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData);

        assertThat(caseState).isEqualTo(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION);
    }

    @Test
    public void shouldReturnCurrentStateWhenMakeOrderSelectedAndEmptyTextProvided() {
        CaseData caseData = CaseData.builder()
            .ccdState(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .judicialDecision(new GAJudicialDecision(MAKE_AN_ORDER))
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder().directionsText("   ").build())
            .build();

        CaseState caseState = stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData);

        assertThat(caseState).isEqualTo(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION);
    }

    @Test
    public void shouldReturnCurrentStateWhenMakeOrderSelectedAndNullTextProvided() {
        CaseData caseData = CaseData.builder()
            .ccdState(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .judicialDecision(new GAJudicialDecision(MAKE_AN_ORDER))
            .build();

        CaseState caseState = stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData);

        assertThat(caseState).isEqualTo(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION);
    }

    @Test
    public void shouldReturnCurrentStateWhenMakeOrderAndDismissed() {

        CaseData caseData = CaseData.builder()
            .ccdState(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .judicialDecision(new GAJudicialDecision(MAKE_AN_ORDER))
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .makeAnOrder(DISMISS_THE_APPLICATION)
                                           .build())
            .build();

        CaseState caseState = stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData);

        assertThat(caseState).isEqualTo(APPLICATION_DISMISSED);
    }

    @Test
    public void shouldReturnListingForHearingWhenTheDecisionHasBeenMade() {

        CaseData caseData = CaseData.builder()
            .ccdState(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .judicialDecision(new GAJudicialDecision(LIST_FOR_A_HEARING))
            .build();

        CaseState caseState = stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData);

        assertThat(caseState).isEqualTo(LISTING_FOR_A_HEARING);
    }

    @Test
    public void shouldReturnProceedsInHeritageSystemWhenTheDecisionHasBeenMade() {
        CaseData caseData = CaseData.builder()
            .ccdState(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .judicialDecision(new GAJudicialDecision(MAKE_AN_ORDER))
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .makeAnOrder(APPROVE_OR_EDIT)
                                           .build())
            .parentClaimantIsApplicant(YesOrNo.YES)
            .generalAppType(GAApplicationType.builder()
                                .types(applicationTypeJudgement()).build())
            .build();
        CaseState caseState = stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData);
        assertThat(caseState).isEqualTo(PROCEEDS_IN_HERITAGE);
    }

    @Test
    public void shouldReturnOrderMadeWhenTheDecisionHasBeenMade() {
        CaseData caseData = CaseData.builder()
            .ccdState(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .judicialDecision(new GAJudicialDecision(MAKE_AN_ORDER))
            .judicialDecisionMakeOrder(GAJudicialMakeAnOrder.builder()
                                           .makeAnOrder(APPROVE_OR_EDIT)
                                           .build())
            .build();
        CaseState caseState = stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData);
        assertThat(caseState).isEqualTo(ORDER_MADE);
    }

    @Test
    public void shouldReturnOrderAdditionalAddPayment_WhenJudgeRequestInformationWithNotice() {
        CaseData caseData = CaseData.builder()
            .ccdState(APPLICATION_SUBMITTED_AWAITING_JUDICIAL_DECISION)
            .judicialDecision(new GAJudicialDecision(REQUEST_MORE_INFO))
            .judicialDecisionRequestMoreInfo(GAJudicialRequestMoreInfo.builder()
                                                 .requestMoreInfoOption(SEND_APP_TO_OTHER_PARTY)
                                                 .build())
            .build();
        CaseState caseState = stateGeneratorService.getCaseStateForEndJudgeBusinessProcess(caseData);
        assertThat(caseState).isEqualTo(APPLICATION_ADD_PAYMENT);
    }

    private List<GeneralApplicationTypes> applicationTypeJudgement() {
        return List.of(
            GeneralApplicationTypes.STRIKE_OUT
        );
    }
}

