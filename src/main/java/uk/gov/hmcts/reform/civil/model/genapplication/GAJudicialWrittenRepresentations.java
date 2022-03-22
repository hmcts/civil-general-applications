package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.dq.GAJudgeWrittenRepresentationsOptions;

import java.time.LocalDate;

@Setter
@Data
@Builder(toBuilder = true)
public class GAJudicialWrittenRepresentations {

    private GAJudgeWrittenRepresentationsOptions writtenOption;
    private LocalDate writtenSequentailRepresentationsBy;
    private String sequentialMustRespondWithin1;
    private LocalDate writtenConcurrentRepresentationsBy;

    @JsonCreator
    GAJudicialWrittenRepresentations(@JsonProperty("makeAnOrderForWrittenRepresentations") GAJudgeWrittenRepresentationsOptions writtenOption,
                          @JsonProperty("writtenSequentailRepresentationsBy") LocalDate writtenSequentailRepresentationsBy,
                          @JsonProperty("sequentialMustRespondWithin1") String sequentialMustRespondWithin1,
                          @JsonProperty("writtenConcurrentRepresentationsBy") LocalDate writtenConcurrentRepresentationsBy) {
        this.writtenOption = writtenOption;
        this.writtenSequentailRepresentationsBy = writtenSequentailRepresentationsBy;
        this.sequentialMustRespondWithin1 = sequentialMustRespondWithin1;
        this.writtenConcurrentRepresentationsBy = writtenConcurrentRepresentationsBy;
    }
}
