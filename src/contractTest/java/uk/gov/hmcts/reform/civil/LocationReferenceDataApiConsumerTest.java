package uk.gov.hmcts.reform.civil;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.civil.client.LocationReferenceDataApiClient;
import uk.gov.hmcts.reform.civil.model.LocationRefData;

import java.util.List;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@PactTestFor(providerName = "referenceData_location")
@MockServerConfig(hostInterface = "localhost", port = "6669")
@TestPropertySource(properties = "location.api.baseUrl=http://localhost:6669")
public class LocationReferenceDataApiConsumerTest extends BaseContractTest {

    public static final String ENDPOINT = "/refdata/location/court-venues";

    @Autowired
    private LocationReferenceDataApiClient locationReferenceDataApiClient;

    @Pact(consumer = "civil_general_applications")
    public RequestResponsePact getCourtVenueByName(PactDslWithProvider builder) {
        return buildCourtVenueByNameResponsePact(builder);
    }

    @Pact(consumer = "civil_general_applications")
    public RequestResponsePact getCourtVenueByEpimmsId(PactDslWithProvider builder) {
        return buildCourtVenueByEpimmsIdResponsePact(builder);
    }

    @Pact(consumer = "civil_general_applications")
    public RequestResponsePact getCourtVenue(PactDslWithProvider builder) {
        return buildCourtVenueResponsePact(builder);
    }

    @Test
    @PactTestFor(pactMethod = "getCourtVenue")
    public void verifyCourtVenue() {
        List<LocationRefData> response = locationReferenceDataApiClient.getCourtVenue(
            SERVICE_AUTH_TOKEN,
            AUTHORIZATION_TOKEN,
            "isHearingLocation",
            "isCaseManagementLocation",
            "courtTypeId",
            "locationType"
        );
        assertThat(
            response.get(0).getRegion(),
            is(equalTo("regionTest123"))
        );
    }

    @Test
    @PactTestFor(pactMethod = "getCourtVenueByName")
    public void verifyCourtVenueByName() {
        List<LocationRefData> response = locationReferenceDataApiClient.getCourtVenueByName(
            SERVICE_AUTH_TOKEN,
            AUTHORIZATION_TOKEN,
            "courtNameTest"
        );
        assertThat(
            response.get(0).getRegion(),
            is(equalTo("regionTest123"))
        );
    }

    @Test
    @PactTestFor(pactMethod = "getCourtVenueByEpimmsId")
    public void verifyCourtVenueByEpimmsId() {
        List<LocationRefData> response = locationReferenceDataApiClient.getCourtVenueByEpimmsId(
            SERVICE_AUTH_TOKEN,
            AUTHORIZATION_TOKEN,
            "epimmsId",
            "courtTypeId"
        );
        assertThat(
            response.get(0).getRegion(),
            is(equalTo("regionTest123"))
        );
    }

    private RequestResponsePact buildCourtVenueResponsePact(PactDslWithProvider builder) {
        return builder
            .given("There are court locations to be returned")
            .uponReceiving("a location request")
            .path(ENDPOINT)
            .headers(SERVICE_AUTHORIZATION_HEADER, SERVICE_AUTH_TOKEN, AUTHORIZATION_HEADER, AUTHORIZATION_TOKEN)
            .method(HttpMethod.GET.toString())
            .matchQuery("is_hearing_location", "isHearingLocation", "isHearingLocation")
            .matchQuery("is_case_management_location", "isCaseManagementLocation", "isCaseManagementLocation")
            .matchQuery("court_type_id", "courtTypeId", "courtTypeId")
            .matchQuery("location_type", "locationType", "locationType")
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(buildLocationRefDataResponseBody())
            .status(HttpStatus.SC_OK)
            .toPact();
    }

    private RequestResponsePact buildCourtVenueByNameResponsePact(PactDslWithProvider builder) {
        return builder
            .given("There are court locations to be returned")
            .uponReceiving("a location request")
            .path(ENDPOINT)
            .headers(SERVICE_AUTHORIZATION_HEADER, SERVICE_AUTH_TOKEN, AUTHORIZATION_HEADER, AUTHORIZATION_TOKEN)
            .method(HttpMethod.GET.toString())
            .matchQuery("court_venue_name", "courtNameTest", "courtNameTest")
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(buildLocationRefDataResponseBody())
            .status(HttpStatus.SC_OK)
            .toPact();
    }

    private RequestResponsePact buildCourtVenueByEpimmsIdResponsePact(PactDslWithProvider builder) {
        return builder
            .given("There are court locations to be returned")
            .uponReceiving("a location request")
            .path(ENDPOINT)
            .headers(SERVICE_AUTHORIZATION_HEADER, SERVICE_AUTH_TOKEN, AUTHORIZATION_HEADER, AUTHORIZATION_TOKEN)
            .method(HttpMethod.GET.toString())
            .matchQuery("court_type_id", "courtTypeId", "courtTypeId")
            .matchQuery("epimms_id", "epimmsId", "epimmsId")
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(buildLocationRefDataResponseBody())
            .status(HttpStatus.SC_OK)
            .toPact();
    }

    static DslPart buildLocationRefDataResponseBody() {
        return newJsonArray(response ->
                                response
                                    .object(locationRefData -> locationRefData
                                        .stringType("courtVenueId", "courtVenueId123")
                                        .stringType("epimmsId", "epimmsIdTest123")
                                        .stringType("siteName", "siteNameTest123")
                                        .stringType("regionId", "regionIdTest123")
                                        .stringType("region", "regionTest123")
                                        .stringType("courtType", "courtTypeTest123")
                                        .stringType("courtTypeId", "courtTypeIdTest123")
                                        .stringType("courtAddress", "courtAddressTest123")
                                        .stringType("postcode", "postcodeTest123")
                                        .stringType("phoneNumber", "phoneNumberTest123")
                                        .stringType("courtLocationCode", "courtLocationCodeTest123")
                                        .stringType("courtStatus", "courtStatusTest123")
                                        .stringType("courtName", "courtNameTest123")
                                        .stringType("venueName", "venueNameTest123")
                                        .stringType("locationType", "locationTypeTest123")
                                        .stringType("parentLocation", "parentLocationTest123"))
        ).build();
    }
}
