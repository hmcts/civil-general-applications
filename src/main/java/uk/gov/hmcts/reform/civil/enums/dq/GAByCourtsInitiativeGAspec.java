package uk.gov.hmcts.reform.civil.enums.dq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GAByCourtsInitiativeGAspec {

    OPTION_1("If you were not notified of the application and provided with a copy of the application "
                + "notice before the order was made, you may apply to set aside, vary or stay the order. "
                + "Such application must not be made more than 7 days after the date on which the order"
                + " was notified to the party."),
    OPTION_2("As this order was made on the court's own initiative, any party affected by the order may "
                + "apply to set aside, vary or stay the order. Such application must not be made more than "
                + "7 days after the date on which the order was notified to the party making the application."),
    OPTION_3("No paragraph required");

    private final String displayedValue;
}
