package uk.gov.hmcts.reform.civil.service.search;

import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.model.search.Query;

import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

public class CaseStateSearchServiceTest extends ElasticSearchServiceTest {

    @BeforeEach
    void setup() {
        searchService = new CaseStateSearchService(coreCaseDataService);
    }

    @Override
    protected Query buildQuery(int fromValue, CaseState caseState) {
        return new Query(
            matchQuery("state", caseState.toString()),
            emptyList(),
            fromValue
        );
    }
}
