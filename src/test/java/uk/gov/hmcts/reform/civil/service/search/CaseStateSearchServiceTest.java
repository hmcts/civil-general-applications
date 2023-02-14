package uk.gov.hmcts.reform.civil.service.search;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
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

    @Override
    protected Query queryForOrderMade_StayTheClaimCase(int startIndex, CaseState caseState) {
        MatchQueryBuilder queryCaseState = QueryBuilders.matchQuery("state", caseState.toString());
        MatchQueryBuilder queryGaType = QueryBuilders
            .matchQuery("data.generalAppType.types", "STAY_THE_CLAIM");
        MatchQueryBuilder queryOrderProcessStatus = QueryBuilders
            .matchQuery("data.judicialDecisionMakeOrder.isOrderProcessedByStayScheduler", "No");

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        query.must(queryCaseState).must(queryGaType).must(queryOrderProcessStatus);

        return new Query(
            query,
            emptyList(),
            startIndex
        );
    }

    @Override
    protected Query queryForOrderMade_UnlessOrderCase(int startIndex, CaseState caseState) {
        MatchQueryBuilder queryCaseState = QueryBuilders.matchQuery("state", caseState.toString());
        MatchQueryBuilder queryGaType = QueryBuilders
            .matchQuery("data.generalAppType.types", "UNLESS_ORDER");
        MatchQueryBuilder queryOrderProcessStatus = QueryBuilders
            .matchQuery("data.judicialDecisionMakeOrder.isOrderProcessedByUnlessScheduler", "No");

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        query.must(queryCaseState).must(queryGaType).must(queryOrderProcessStatus);

        return new Query(
            query,
            emptyList(),
            startIndex
        );
    }
}
