package uk.gov.hmcts.reform.civil.service.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SearchResult;
import uk.gov.hmcts.reform.civil.enums.CaseState;
import uk.gov.hmcts.reform.civil.model.search.Query;
import uk.gov.hmcts.reform.civil.service.CoreCaseDataService;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
abstract class ElasticSearchServiceTest {

    @Captor
    protected ArgumentCaptor<Query> queryCaptor;

    @Mock
    protected CoreCaseDataService coreCaseDataService;

    protected ElasticSearchService searchService;

    @Test
    void shouldCallGetCasesOnce_WhenCasesReturnEqualsTotalCases() {
        SearchResult searchResult = buildSearchResultWithTotalCases(1);

        when(coreCaseDataService.searchGeneralApplication(any())).thenReturn(searchResult);

        assertThat(searchService.getGeneralApplications(CaseState.AWAITING_WRITTEN_REPRESENTATIONS))
            .isEqualTo(searchResult.getCases());
        verify(coreCaseDataService).searchGeneralApplication(queryCaptor.capture());
        assertThat(queryCaptor.getValue()).usingRecursiveComparison()
            .isEqualTo(buildQuery(0, CaseState.AWAITING_WRITTEN_REPRESENTATIONS));
    }

    @Test
    void shouldCallGetCasesTwice_WhenNoCasesReturned() {
        SearchResult searchResult = buildSearchResult(0, emptyList());

        when(coreCaseDataService.searchGeneralApplication(any())).thenReturn(searchResult);

        assertThat(searchService.getOrderMadeGeneralApplications(CaseState.ORDER_MADE)).isEmpty();

        verify(coreCaseDataService, times(2)).searchGeneralApplication(queryCaptor.capture());

        List<Query> capturedQueries = queryCaptor.getAllValues();
        assertThat(capturedQueries.get(0)).usingRecursiveComparison()
            .isEqualTo(queryForOrderMade_StayTheClaimCase(0, CaseState.ORDER_MADE));
        assertThat(capturedQueries.get(1)).usingRecursiveComparison()
            .isEqualTo(queryForOrderMade_UnlessOrderCase(0, CaseState.ORDER_MADE));
    }

    @Test
    void shouldCallGetCasesOnce_WhenCasesRetrievedEqualsEsSearchLimit() {
        SearchResult searchResult = buildSearchResultWithTotalCases(10);

        when(coreCaseDataService.searchGeneralApplication(any())).thenReturn(searchResult);

        assertThat(searchService.getGeneralApplications(CaseState.AWAITING_RESPONDENT_RESPONSE)).hasSize(1);
        verify(coreCaseDataService).searchGeneralApplication(queryCaptor.capture());
        assertThat(queryCaptor.getValue()).usingRecursiveComparison()
            .isEqualTo(buildQuery(0, CaseState.AWAITING_RESPONDENT_RESPONSE));
    }

    @Test
    void shouldCallGetCasesMultipleTimes_WhenCasesReturnedIsMoreThanEsSearchLimit() {
        SearchResult searchResult = buildSearchResultWithTotalCases(11);

        when(coreCaseDataService.searchGeneralApplication(any())).thenReturn(searchResult);

        assertThat(searchService.getOrderMadeGeneralApplications(CaseState.ORDER_MADE)).hasSize(2);
        verify(coreCaseDataService, times(4)).searchGeneralApplication(queryCaptor.capture());

        List<Query> capturedQueries = queryCaptor.getAllValues();
        assertThat(capturedQueries.get(0)).usingRecursiveComparison()
            .isEqualTo(queryForOrderMade_StayTheClaimCase(0, CaseState.ORDER_MADE));
        assertThat(capturedQueries.get(1)).usingRecursiveComparison()
            .isEqualTo(queryForOrderMade_StayTheClaimCase(10, CaseState.ORDER_MADE));

        assertThat(capturedQueries.get(2)).usingRecursiveComparison()
            .isEqualTo(queryForOrderMade_UnlessOrderCase(0, CaseState.ORDER_MADE));
        assertThat(capturedQueries.get(3)).usingRecursiveComparison()
            .isEqualTo(queryForOrderMade_UnlessOrderCase(10, CaseState.ORDER_MADE));
    }

    protected SearchResult buildSearchResultWithTotalCases(int i) {
        return buildSearchResult(i, List.of(CaseDetails.builder().id(1L).build()));
    }

    protected SearchResult buildSearchResult(int i, List<CaseDetails> caseDetails) {
        return SearchResult.builder()
            .total(i)
            .cases(caseDetails)
            .build();
    }

    protected abstract Query buildQuery(int fromValue, CaseState caseState);

    protected abstract Query queryForOrderMade_StayTheClaimCase(int fromValue, CaseState caseState);

    protected abstract Query queryForOrderMade_UnlessOrderCase(int startIndex, CaseState caseState);
}
