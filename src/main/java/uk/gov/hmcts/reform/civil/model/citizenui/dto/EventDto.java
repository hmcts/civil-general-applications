package uk.gov.hmcts.reform.civil.model.citizenui.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {

    private CaseEvent event;
    private Map<String, Object> generalApplicationUpdate;

}
