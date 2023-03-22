package uk.gov.hmcts.reform.civil.model.genapplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.hmcts.reform.civil.enums.dq.AppealOriginTypes;
import uk.gov.hmcts.reform.civil.enums.dq.PermissionToAppealTypes;

import java.util.List;

@Setter
@Data
@Builder(toBuilder = true)
public class AssistedOrderAppealDetails {

    private final List<AppealOriginTypes> appealOrigin;
    private final String otherOriginText;
    private final List<PermissionToAppealTypes> permissionToAppeal;
    private final String reasonsText;

    @JsonCreator
    AssistedOrderAppealDetails(@JsonProperty("appealOrigin") List<AppealOriginTypes> appealOrigin,
                               @JsonProperty("otherOriginText") String otherOriginText,
                               @JsonProperty("permissionToAppeal") List<PermissionToAppealTypes> permissionToAppeal,
                               @JsonProperty("reasonsText") String reasonsText
                               ) {

        this.appealOrigin = appealOrigin;
        this.otherOriginText = otherOriginText;
        this.permissionToAppeal = permissionToAppeal;
        this.reasonsText = reasonsText;
    }
}
