package uk.gov.hmcts.reform.civil.sampledata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.model.CaseData;

import java.time.LocalDateTime;
import java.util.Map;

import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;

@SuppressWarnings("unchecked")
public class CaseDetailsBuilder {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private String state;
    private Map<String, Object> data;
    private Long id;
    private LocalDateTime createdDate;

    public static CaseDetailsBuilder builder() {
        return new CaseDetailsBuilder();
    }

    public CaseDetailsBuilder state(CaseState state) {
        this.state = state.name();
        return this;
    }

    public CaseDetailsBuilder data(CaseData caseData) {
        this.data = mapper.convertValue(caseData, Map.class);
        return this;
    }

    public CaseDetailsBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public CaseDetailsBuilder createdDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public CaseDetails build() {
        return CaseDetails.builder()
            .data(data)
            .state(state)
            .id(id)
            .createdDate(createdDate)
            .build();
    }

    public CaseDetailsBuilder atStateAwaitingRespondentAcknowledgement() {
        CaseData caseData = CaseDataBuilder.builder().build();
        this.data = mapper.convertValue(caseData, Map.class);
        this.state = AWAITING_RESPONDENT_RESPONSE.name();
        return this;
    }
}
