package uk.gov.hmcts.reform.civil.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.civil.config.GeneralAppFeesConfiguration;
import uk.gov.hmcts.reform.civil.config.GeneralAppLRDConfiguration;
import uk.gov.hmcts.reform.civil.model.LocationRefData;
import uk.gov.hmcts.reform.civil.model.common.DynamicList;
import uk.gov.hmcts.reform.civil.model.common.DynamicListElement;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.civil.model.common.DynamicList.fromList;

@SpringBootTest(classes = {GeneralAppFeesService.class, RestTemplate.class, GeneralAppFeesConfiguration.class})
class GeneralAppLocationRefDataServiceTest {

    @Captor
    private ArgumentCaptor<URI> uriCaptor;

    @Captor
    private ArgumentCaptor<HttpMethod> httpMethodCaptor;

    @Captor
    private ArgumentCaptor<HttpEntity<?>> httpEntityCaptor;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private GeneralAppLRDConfiguration lrdConfiguration;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @InjectMocks
    private GeneralAppLocationRefDataService refDataService;

    @BeforeEach
    void setUp() {
        when(lrdConfiguration.getUrl()).thenReturn("dummy_url");
        when(lrdConfiguration.getEndpoint()).thenReturn("/fees-register/fees/lookup");
    }

    private ResponseEntity<List<LocationRefData>> getAllLocationsRefDataResponse() {
        List<LocationRefData> responseData = new ArrayList<LocationRefData>();
        responseData.add(getLocationRefData("site_name_01", "London", "AA0 0BB",
                                            "court address 1111"
        ));
        responseData.add(getLocationRefData("site_name_02", "London", "AA0 0BB",
                                            "court address 2222"
        ));
        responseData.add(getLocationRefData("site_name_03", "Midlands", "AA0 0BB",
                                            "court address 3333"
        ));
        responseData.add(getLocationRefData("site_name_04", "Midlands", "AA0 0BB",
                                            "court address 4444"
        ));
        responseData.add(getLocationRefData("site_name_05", "North East", "AA0 0BB",
                                            "court address 5555"
        ));
        responseData.add(getLocationRefData("site_name_06", "South East", "AA0 0BB",
                                            "court address 6666"
        ));
        responseData.add(getLocationRefData("site_name_07", "North West", "AA0 0BB",
                                            "court address 7777"
        ));
        responseData.add(getLocationRefData("site_name_08", "South West", "AA0 0BB",
                                            "court address 8888"
        ));
        responseData.add(getLocationRefData("site_name_09", "Wales", "AA0 0BB",
                                            "court address 9999"
        ));
        responseData.add(getLocationRefData("site_name_10", "London", "AA0 0BB",
                                            "court address 1001"
        ));
        responseData.add(getLocationRefData("site_name_11", "Scotland", "AA0 0BB",
                                            "court address 1011"
        ));
        responseData.add(getLocationRefData("site_name_12", "Scotland", "AA0 0BB",
                                            "court address 1012"
        ));

        return new ResponseEntity<List<LocationRefData>>(responseData, OK);
    }

    private ResponseEntity<List<LocationRefData>> getAllLocationsRefDataResponseByEpimms() {
        List<LocationRefData> responseData = new ArrayList<LocationRefData>();
        responseData.add(getLocationRefData("site_name_01", "London", "AA0 0BB",
                                            "court address 1111"
        ));

        return new ResponseEntity<List<LocationRefData>>(responseData, OK);
    }

    private ResponseEntity<List<LocationRefData>> getNonScotlandLocationsRefDataResponse() {
        List<LocationRefData> responseData = new ArrayList<LocationRefData>();
        responseData.add(getLocationRefData("site_name_01", "London", "AA0 0BB",
                                            "court address 1111"
        ));
        responseData.add(getLocationRefData("site_name_02", "London", "AA0 0BB",
                                            "court address 2222"
        ));
        responseData.add(getLocationRefData("site_name_03", "Midlands", "AA0 0BB",
                                            "court address 3333"
        ));
        responseData.add(getLocationRefData("site_name_04", "Midlands", "AA0 0BB",
                                            "court address 4444"
        ));
        responseData.add(getLocationRefData("site_name_05", "North East", "AA0 0BB",
                                            "court address 5555"
        ));
        responseData.add(getLocationRefData("site_name_06", "South East", "AA0 0BB",
                                            "court address 6666"
        ));
        responseData.add(getLocationRefData("site_name_07", "North West", "AA0 0BB",
                                            "court address 7777"
        ));
        responseData.add(getLocationRefData("site_name_08", "South West", "AA0 0BB",
                                            "court address 8888"
        ));
        responseData.add(getLocationRefData("site_name_09", "Wales", "AA0 0BB",
                                            "court address 9999"
        ));
        responseData.add(getLocationRefData("site_name_10", "London", "AA0 0BB",
                                            "court address 1001"
        ));
        responseData.add(getLocationRefData("site_name_11", "Midlands", "AA0 0BB",
                                            "court address 1011"
        ));
        responseData.add(getLocationRefData("site_name_12", "Wales", "AA0 0BB",
                                            "court address 1012"
        ));

        return new ResponseEntity<List<LocationRefData>>(responseData, OK);
    }

    private ResponseEntity<List<LocationRefData>> getOnlyScotlandLocationsRefDataResponse() {
        List<LocationRefData> responseData = new ArrayList<LocationRefData>();
        responseData.add(getLocationRefData("site_name_01", "Scotland", "AA0 0BB",
                                            "court address 1111"
        ));
        responseData.add(getLocationRefData("site_name_02", "Scotland", "AA0 0BB",
                                            "court address 2222"
        ));
        responseData.add(getLocationRefData("site_name_03", "Scotland", "AA0 0BB",
                                            "court address 3333"
        ));
        responseData.add(getLocationRefData("site_name_04", "Scotland", "AA0 0BB",
                                            "court address 4444"
        ));
        responseData.add(getLocationRefData("site_name_05", "Scotland", "AA0 0BB",
                                            "court address 5555"
        ));

        return new ResponseEntity<List<LocationRefData>>(responseData, OK);
    }

    private LocationRefData getLocationRefData(String siteName, String region, String postcode, String courtAddress) {
        return LocationRefData.builder().siteName(siteName).region(region)
            .postcode(postcode).courtAddress(courtAddress).build();
    }

    private DynamicList getLocationsFromList(final List<LocationRefData> locations) {
        return fromList(locations.stream().map(location -> new StringBuilder().append(location.getSiteName())
                .append(" - ").append(location.getCourtAddress())
                .append(" - ").append(location.getPostcode()).toString())
                            .collect(Collectors.toList()));
    }

    private List<String> locationsFromDynamicList(DynamicList dynamicList) {
        return dynamicList.getListItems().stream()
            .map(DynamicListElement::getLabel)
            .collect(Collectors.toList());
    }

    @Test
    void shouldReturnLocations_whenLRDReturnsAllLocations() {
        when(authTokenGenerator.generate()).thenReturn("service_token");
        when(restTemplate.exchange(
            uriCaptor.capture(),
            httpMethodCaptor.capture(),
            httpEntityCaptor.capture(),
            ArgumentMatchers.<ParameterizedTypeReference<List<LocationRefData>>>any()
        ))
            .thenReturn(getAllLocationsRefDataResponse());

        List<LocationRefData> courtLocations = refDataService
            .getCourtLocations("user_token");

        DynamicList courtLocationString = getLocationsFromList(courtLocations);

        assertThat(locationsFromDynamicList(courtLocationString))
            .containsOnly(
                "site_name_01 - court address 1111 - AA0 0BB",
                "site_name_02 - court address 2222 - AA0 0BB",
                "site_name_03 - court address 3333 - AA0 0BB",
                "site_name_04 - court address 4444 - AA0 0BB",
                "site_name_05 - court address 5555 - AA0 0BB",
                "site_name_06 - court address 6666 - AA0 0BB",
                "site_name_07 - court address 7777 - AA0 0BB",
                "site_name_08 - court address 8888 - AA0 0BB",
                "site_name_09 - court address 9999 - AA0 0BB",
                "site_name_10 - court address 1001 - AA0 0BB"
            );

        assertThat(courtLocations.size()).isEqualTo(10);
        verify(lrdConfiguration, times(1)).getUrl();
        verify(lrdConfiguration, times(1)).getEndpoint();
        assertThat(uriCaptor.getValue().toString())
            .isEqualTo("dummy_url/fees-register/fees/lookup?is_hearing_location=Y&is_case_management"
                           + "_location=Y&court_type_id=10&location_type=Court");
        assertThat(httpMethodCaptor.getValue()).isEqualTo(HttpMethod.GET);
        assertThat(httpEntityCaptor.getValue().getHeaders().getFirst("Authorization")).isEqualTo("user_token");
        assertThat(httpEntityCaptor.getValue().getHeaders().getFirst("ServiceAuthorization"))
            .isEqualTo("service_token");
    }

    @Test
    void shouldReturnLocations_whenLRDReturnsNullBody() {
        when(authTokenGenerator.generate()).thenReturn("service_token");
        when(restTemplate.exchange(
            uriCaptor.capture(),
            httpMethodCaptor.capture(),
            httpEntityCaptor.capture(),
            ArgumentMatchers.<ParameterizedTypeReference<List<LocationRefData>>>any()
        ))
            .thenReturn(new ResponseEntity<List<LocationRefData>>(OK));

        List<LocationRefData> courtLocations = refDataService
            .getCourtLocations("user_token");

        assertThat(courtLocations).isEmpty();
        verify(lrdConfiguration, times(1)).getUrl();
        verify(lrdConfiguration, times(1)).getEndpoint();
        assertThat(uriCaptor.getValue().toString())
            .isEqualTo("dummy_url/fees-register/fees/lookup?is_hearing_location=Y&is_case_management"
                           + "_location=Y&court_type_id=10&location_type=Court");
        assertThat(httpMethodCaptor.getValue()).isEqualTo(HttpMethod.GET);
        assertThat(httpEntityCaptor.getValue().getHeaders().getFirst("Authorization")).isEqualTo("user_token");
        assertThat(httpEntityCaptor.getValue().getHeaders().getFirst("ServiceAuthorization"))
            .isEqualTo("service_token");
    }

    @Test
    void shouldReturnLocations_whenLRDReturnsOnlyScotlandLocations() {
        when(authTokenGenerator.generate()).thenReturn("service_token");
        when(restTemplate.exchange(
            uriCaptor.capture(),
            httpMethodCaptor.capture(),
            httpEntityCaptor.capture(),
            ArgumentMatchers.<ParameterizedTypeReference<List<LocationRefData>>>any()
        ))
            .thenReturn(getOnlyScotlandLocationsRefDataResponse());

        List<LocationRefData> courtLocations = refDataService.getCourtLocations("user_token");

        assertThat(courtLocations.size()).isEqualTo(0);
        verify(lrdConfiguration, times(1)).getUrl();
        verify(lrdConfiguration, times(1)).getEndpoint();
        assertThat(uriCaptor.getValue().toString())
            .isEqualTo("dummy_url/fees-register/fees/lookup?is_hearing_location=Y&is_case_management"
                           + "_location=Y&court_type_id=10&location_type=Court");
        assertThat(httpMethodCaptor.getValue()).isEqualTo(HttpMethod.GET);
        assertThat(httpEntityCaptor.getValue().getHeaders().getFirst("Authorization")).isEqualTo("user_token");
        assertThat(httpEntityCaptor.getValue().getHeaders().getFirst("ServiceAuthorization"))
            .isEqualTo("service_token");
    }

    @Test
    void shouldReturnLocations_whenLRDReturnsNonScotlandLocations() {
        when(authTokenGenerator.generate()).thenReturn("service_token");
        when(restTemplate.exchange(
            uriCaptor.capture(),
            httpMethodCaptor.capture(),
            httpEntityCaptor.capture(),
            ArgumentMatchers.<ParameterizedTypeReference<List<LocationRefData>>>any()
        ))
            .thenReturn(getNonScotlandLocationsRefDataResponse());

        List<LocationRefData> courtLocations = refDataService.getCourtLocations("user_token");

        DynamicList courtLocationString = getLocationsFromList(courtLocations);

        assertThat(courtLocations.size()).isEqualTo(12);
        assertThat(locationsFromDynamicList(courtLocationString)).containsOnly(
            "site_name_01 - court address 1111 - AA0 0BB",
            "site_name_02 - court address 2222 - AA0 0BB",
            "site_name_03 - court address 3333 - AA0 0BB",
            "site_name_04 - court address 4444 - AA0 0BB",
            "site_name_05 - court address 5555 - AA0 0BB",
            "site_name_06 - court address 6666 - AA0 0BB",
            "site_name_07 - court address 7777 - AA0 0BB",
            "site_name_08 - court address 8888 - AA0 0BB",
            "site_name_09 - court address 9999 - AA0 0BB",
            "site_name_10 - court address 1001 - AA0 0BB",
            "site_name_11 - court address 1011 - AA0 0BB",
            "site_name_12 - court address 1012 - AA0 0BB"
        );
        verify(lrdConfiguration, times(1)).getUrl();
        verify(lrdConfiguration, times(1)).getEndpoint();
        assertThat(uriCaptor.getValue().toString())
            .isEqualTo("dummy_url/fees-register/fees/lookup?is_hearing_location=Y&is_case_management"
                           + "_location=Y&court_type_id=10&location_type=Court");
        assertThat(httpMethodCaptor.getValue()).isEqualTo(HttpMethod.GET);
        assertThat(httpEntityCaptor.getValue().getHeaders().getFirst("Authorization")).isEqualTo("user_token");
        assertThat(httpEntityCaptor.getValue().getHeaders().getFirst("ServiceAuthorization"))
            .isEqualTo("service_token");
    }

    @Test
    void shouldReturnEmptyList_whenLRDThrowsException() {
        when(authTokenGenerator.generate()).thenReturn("service_token");
        when(restTemplate.exchange(
            uriCaptor.capture(),
            httpMethodCaptor.capture(),
            httpEntityCaptor.capture(),
            ArgumentMatchers.<ParameterizedTypeReference<List<LocationRefData>>>any()
        ))
            .thenThrow(new RestClientException("403"));

        List<LocationRefData> courtLocations = refDataService
            .getCourtLocations("user_token");

        assertThat(courtLocations.size()).isEqualTo(0);
    }

    @Test
    void shouldReturnLocations_whenLRDReturnsAllLocationsByEpimmsId() {
        when(authTokenGenerator.generate()).thenReturn("service_token");
        when(restTemplate.exchange(
            uriCaptor.capture(),
            httpMethodCaptor.capture(),
            httpEntityCaptor.capture(),
            ArgumentMatchers.<ParameterizedTypeReference<List<LocationRefData>>>any()
        ))
            .thenReturn(getAllLocationsRefDataResponseByEpimms());

        List<LocationRefData> courtLocations = refDataService
            .getCourtLocationsByEpimmsId("user_token", "00000");

        DynamicList courtLocationString = getLocationsFromList(courtLocations);

        assertThat(locationsFromDynamicList(courtLocationString))
            .containsOnly(
                "site_name_01 - court address 1111 - AA0 0BB"
            );

        assertThat(courtLocations.size()).isEqualTo(1);
        verify(lrdConfiguration, times(1)).getUrl();
        verify(lrdConfiguration, times(1)).getEndpoint();
        assertThat(uriCaptor.getValue().toString())
            .isEqualTo("dummy_url/fees-register/fees/lookup?epimms_id=00000");
        assertThat(httpMethodCaptor.getValue()).isEqualTo(HttpMethod.GET);
        assertThat(httpEntityCaptor.getValue().getHeaders().getFirst("Authorization")).isEqualTo("user_token");
        assertThat(httpEntityCaptor.getValue().getHeaders().getFirst("ServiceAuthorization"))
            .isEqualTo("service_token");
    }
}
