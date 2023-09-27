package uk.gov.hmcts.reform.civil.enums.dq;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
public class AppealTypeChoices {

    private AppealTypeChoiceList appealChoiceOptionA;
    private AppealTypeChoiceList appealChoiceOptionB;

    @JsonCreator
    AppealTypeChoices(@JsonProperty("assistedOrderAppealFirstOption") AppealTypeChoiceList appealChoiceOptionA,
                               @JsonProperty("assistedOrderAppealSecondOption") AppealTypeChoiceList appealChoiceOptionB
    ) {

        this.appealChoiceOptionA = appealChoiceOptionA;
        this.appealChoiceOptionB = appealChoiceOptionB;
    }

}
