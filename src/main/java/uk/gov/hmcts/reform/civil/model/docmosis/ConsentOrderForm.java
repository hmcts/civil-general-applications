package uk.gov.hmcts.reform.civil.model.docmosis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import uk.gov.hmcts.reform.civil.model.common.MappableObject;

@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class ConsentOrderForm implements MappableObject {

    private final String claimNumber;
    private final String claimantName;
    private final String defendantName;
    private final String consentOrder;
    private final String orderDate;
    private final String courtName;

}
