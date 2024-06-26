package uk.gov.hmcts.reform.civil.service.search;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.civil.model.search.Query;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class EvidenceUploadNotificationSearchServiceTest {

    @Captor
    protected ArgumentCaptor<Query> queryCaptor;

    @Mock
    protected CoreCaseDataService coreCaseDataService;

    protected EvidenceUploadNotificationSearchService searchService;

    @BeforeEach
    void setup() {
        searchService = new EvidenceUploadNotificationSearchService(coreCaseDataService);
    }

    @Test
    void shouldCallGetCasesOnce_WhenNoCasesReturned() {
        SearchResult searchResult = buildSearchResult(0, emptyList());

        when(coreCaseDataService.searchGeneralApplication(any())).thenReturn(searchResult);

        assertThat(searchService.getApplications()).isEmpty();
        verify(coreCaseDataService).searchGeneralApplication(queryCaptor.capture());
        assertThat(queryCaptor.getValue()).usingRecursiveComparison()
                .isEqualTo(query(0));
    }

    private Query query(int startIndex) {
        return new Query(
                boolQuery()
                        .minimumShouldMatch(1)
                        .should(rangeQuery("data.caseDocumentUploadDate").lt("now").gt(
                                "now-1d"))
                        .should(rangeQuery("data.caseDocumentUploadDateRes").lt("now").gt(
                                "now-1d")),
                List.of("reference"),
                startIndex
        );
    }

    private SearchResult buildSearchResult(int i, List<CaseDetails> caseDetails) {
        return SearchResult.builder()
                .total(i)
                .cases(caseDetails)
                .build();
    }
}
