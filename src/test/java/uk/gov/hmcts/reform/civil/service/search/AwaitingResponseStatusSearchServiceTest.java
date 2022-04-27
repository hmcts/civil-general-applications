package uk.gov.hmcts.reform.civil.service.search;

import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.reform.civil.model.search.Query;

import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static uk.gov.hmcts.reform.civil.enums.CaseState.AWAITING_RESPONDENT_RESPONSE;

class AwaitingResponseStatusSearchServiceTest extends ElasticSearchServiceTest {

    @BeforeEach
    void setup() {
        searchService = new AwaitingResponseStatusSearchService(coreCaseDataService);
    }

    @Override
    protected Query buildQuery(int fromValue) {
        return new Query(
            matchQuery("state", AWAITING_RESPONDENT_RESPONSE.toString()),
            emptyList(),
            fromValue
        );
    }
}
