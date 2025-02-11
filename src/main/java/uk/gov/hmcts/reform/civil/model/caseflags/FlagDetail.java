package uk.gov.hmcts.reform.civil.model.caseflags;

import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.model.common.Element;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class FlagDetail {

    private final String name;
    private final String subTypeValue;
    private final String subTypeKey;
    private final String otherDescription;
    private final String flagComment;
    private final LocalDateTime dateTimeModified;
    private final LocalDateTime dateTimeCreated;
    private final List<Element<String>> path;
    private final YesOrNo hearingRelevant;
    private final String flagCode;
    private final String status;

}
