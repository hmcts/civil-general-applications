package uk.gov.hmcts.reform.civil.model.genapplication.finalorder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

@Setter
@Data
@Builder(toBuilder = true)
public class DetailText {

    private final String detailText;

    @JsonCreator
    DetailText(@JsonProperty("detailsRepresentationText") String detailText
    ) {

        this.detailText = detailText;
    }
}
