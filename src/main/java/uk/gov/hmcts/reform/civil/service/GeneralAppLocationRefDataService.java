package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.civil.config.GeneralAppLRDConfiguration;
import uk.gov.hmcts.reform.civil.model.LocationRefData;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static org.apache.logging.log4j.util.Strings.concat;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneralAppLocationRefDataService {

    private final RestTemplate restTemplate;
    private final GeneralAppLRDConfiguration lrdConfiguration;
    private final AuthTokenGenerator authTokenGenerator;

    public List<LocationRefData> getCourtLocations(String authToken) {
        try {
            ResponseEntity<List<LocationRefData>> responseEntity = restTemplate.exchange(
                buildURI(),
                HttpMethod.GET,
                getHeaders(authToken),
                new ParameterizedTypeReference<>() {
                }
            );
            return onlyEnglandAndWalesLocations(responseEntity.getBody())
                .stream().sorted(Comparator.comparing(LocationRefData::getSiteName)).toList();
        } catch (Exception e) {
            log.error("Location Reference Data Lookup Failed - " + e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    private URI buildURI() {
        String queryURL = lrdConfiguration.getUrl() + lrdConfiguration.getEndpoint();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(queryURL)
            .queryParam("is_hearing_location", "Y")
            .queryParam("is_case_management_location", "Y")
            .queryParam("court_type_id", "10")
            .queryParam("location_type", "Court");
        return builder.buildAndExpand(new HashMap<>()).toUri();
    }

    private HttpEntity<String> getHeaders(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authToken);
        headers.add("ServiceAuthorization", authTokenGenerator.generate());
        return new HttpEntity<>(headers);
    }

    private List<LocationRefData> onlyEnglandAndWalesLocations(List<LocationRefData> locationRefData) {
        return locationRefData == null
                ? new ArrayList<>()
                : locationRefData.stream().filter(location -> !"Scotland".equals(location.getRegion()))
                .toList();
    }

    private String getDisplayEntry(LocationRefData location) {
        return concat(concat(concat(location.getSiteName(), " - "), concat(location.getCourtAddress(), " - ")),
                      location.getPostcode());
    }

    public List<LocationRefData> getCourtLocationsByEpimmsId(String authToken, String epimmsId) {
        try {
            ResponseEntity<List<LocationRefData>> responseEntity = this.restTemplate.exchange(this.buildURIforCourtLocation(epimmsId),
                                                                                              HttpMethod.GET, this.getHeaders(authToken),
                                                                                              new ParameterizedTypeReference<List<LocationRefData>>() {});
            return responseEntity.getBody();
        } catch (Exception var4) {
            log.error("Location Reference Data Lookup Failed - " + var4.getMessage(), var4);
            return new ArrayList<LocationRefData>();
        }
    }

    private URI buildURIforCourtLocation(String epimmsId) {
        String var10000 = this.lrdConfiguration.getUrl();
        String queryURL = var10000 + this.lrdConfiguration.getEndpoint();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(queryURL).queryParam("epimms_id", epimmsId);
        return builder.buildAndExpand(new HashMap<>()).toUri();
    }

}
