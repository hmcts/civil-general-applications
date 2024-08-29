package uk.gov.hmcts.reform.civil.model.genapplication;

import uk.gov.hmcts.reform.civil.enums.YesOrNo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FeePaymentOutcomeDetails {

    private YesOrNo hwfNumberAvailable;
    private String  hwfNumberForFeePaymentOutcome;
    private YesOrNo hwfFullRemissionGrantedForGa;
    private YesOrNo hwfFullRemissionGrantedForAdditional;
    private List<String> hwfOutstandingFeePaymentDoneForGa;
    private List<String> hwfOutstandingFeePaymentDoneForAdditional;
}
