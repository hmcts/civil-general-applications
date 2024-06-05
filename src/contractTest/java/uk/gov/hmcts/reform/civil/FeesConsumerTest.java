package uk.gov.hmcts.reform.civil;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.model.RequestResponsePact;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;

import javax.ws.rs.HttpMethod;
import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "feeRegister_lookUp", port = "6666")
public class FeesConsumerTest {

    public static final String ENDPOINT = "/fees-register/fees/lookup";
    public static final String CHANNEL = "default";
    public static final String JURISDICTION = "civil";
    private static final String GENERAL_APP_EVENT = "general application";
    private static final String WITH_NOTICE_KEYWORD = "GAOnNotice";
    private static final String CONSENT_WITHOUT_NOTICE_KEYWORD = "GeneralAppWithoutNotice";
    private static final String APPN_TO_VARY_KEYWORD = "AppnToVaryOrSuspend";
    private static final String UNCLOACK_FEE = "HACFOOnNotice";
    public static final String SERVICE_GENERAL = "general";

    @Pact(consumer = "civil-service")
    public RequestResponsePact getFeeForGAWithNotice(PactDslWithProvider builder) throws JSONException {
        return buildGenAppFeeRequestResponsePact(builder, "a request for GA with notice",
                                                 WITH_NOTICE_KEYWORD, GENERAL_APP_EVENT, SERVICE_GENERAL,
                                                 new BigDecimal(10.00), "FEE0011"
        );
    }

    @Pact(consumer = "civil-service")
    public RequestResponsePact getFeeForConsentWithOrWithout(PactDslWithProvider builder) throws JSONException {
        return buildGenAppFeeRequestResponsePact(builder, "a request for GA Consent with or without notice",
                                                 CONSENT_WITHOUT_NOTICE_KEYWORD, GENERAL_APP_EVENT, SERVICE_GENERAL,
                                                 new BigDecimal(20.00), "FEE0012"
        );
    }

    @Pact(consumer = "civil-service")
    public RequestResponsePact getFeeForAppToVaryOrSuspend(PactDslWithProvider builder) throws JSONException {
        return buildGenAppFeeRequestResponsePact(builder, "a request for GA App to vary or to suspend",
                                                 APPN_TO_VARY_KEYWORD, "miscellaneous", "other",
                                                 new BigDecimal(30.00), "FEE0013"
        );
    }

    @Pact(consumer = "civil-service")
    public RequestResponsePact getFeeForUncloakFee(PactDslWithProvider builder) throws JSONException {
        return buildGenAppFeeRequestResponsePact(builder, "a request for GA Uncloak fee",
                                                 UNCLOACK_FEE, GENERAL_APP_EVENT, SERVICE_GENERAL,
                                                 new BigDecimal(40.00), "FEE0014"
        );
    }

    @Test
    @PactTestFor(pactMethod = "getFeeForGAWithNotice")
    public void verifyFeeForGAWithNotice() {

        FeeLookupResponseDto fee = getFeeForGA(WITH_NOTICE_KEYWORD, GENERAL_APP_EVENT, SERVICE_GENERAL);
        assertThat(fee.getCode(), is(equalTo("FEE0011")));
        assertThat(fee.getFeeAmount(), is(equalTo(new BigDecimal(10.00))));
    }

    @Test
    @PactTestFor(pactMethod = "getFeeForConsentWithOrWithout")
    public void verifyFeeForConsentWithOrWithout() {

        FeeLookupResponseDto fee = getFeeForGA(CONSENT_WITHOUT_NOTICE_KEYWORD, GENERAL_APP_EVENT, SERVICE_GENERAL);
        assertThat(fee.getCode(), is(equalTo("FEE0012")));
        assertThat(fee.getFeeAmount(), is(equalTo(new BigDecimal(20.00))));
    }

    @Test
    @PactTestFor(pactMethod = "getFeeForAppToVaryOrSuspend")
    public void verifyFeeForAppToVaryOrSuspend() {

        FeeLookupResponseDto fee = getFeeForGA(APPN_TO_VARY_KEYWORD, "miscellaneous", "other");
        assertThat(fee.getCode(), is(equalTo("FEE0013")));
        assertThat(fee.getFeeAmount(), is(equalTo(new BigDecimal(30.00))));
    }

    @Test
    @PactTestFor(pactMethod = "getFeeForUncloakFee")
    public void verifyFeeForUncloakFee() {

        FeeLookupResponseDto fee = getFeeForGA(UNCLOACK_FEE, GENERAL_APP_EVENT, SERVICE_GENERAL);
        assertThat(fee.getCode(), is(equalTo("FEE0014")));
        assertThat(fee.getFeeAmount(), is(equalTo(new BigDecimal(40.00))));
    }

    private RequestResponsePact buildGenAppFeeRequestResponsePact(PactDslWithProvider builder, String uponReceiving,
                                                                  String keyword, String event, String service,
                                                                  BigDecimal feeAmount, String feeCode) {
        return builder
            .given("General Application fees exist")
            .uponReceiving(uponReceiving)
            .path(ENDPOINT)
            .method(HttpMethod.GET)
            .matchQuery("channel", CHANNEL, CHANNEL)
            .matchQuery("event", event, event)
            .matchQuery("jurisdiction1", JURISDICTION, JURISDICTION)
            .matchQuery("jurisdiction2", JURISDICTION, JURISDICTION)
            .matchQuery("service", service, service)
            .matchQuery("keyword", keyword, keyword)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(buildFeesResponseBody(feeCode, feeAmount))
            .status(HttpStatus.SC_OK)
            .toPact();
    }

    private FeeLookupResponseDto getFeeForGA(String keyword, String event, String service) {

        String queryURL = "http://localhost:6666" + ENDPOINT;
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(queryURL)
            .queryParam("channel", CHANNEL)
            .queryParam("event", event)
            .queryParam("jurisdiction1", JURISDICTION)
            .queryParam("jurisdiction2", JURISDICTION)
            .queryParam("service", service)
            .queryParam("keyword", keyword);

        URI uri;
        FeeLookupResponseDto feeLookupResponseDto;
        try {
            uri = builder.buildAndExpand(new HashMap<>()).toUri();

            RestTemplate restTemplate = new RestTemplate();
            feeLookupResponseDto = restTemplate.getForObject(uri, FeeLookupResponseDto.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (feeLookupResponseDto == null || feeLookupResponseDto.getFeeAmount() == null) {
            throw new RuntimeException("No Fees returned by fee-service while creating General Application");
        }
        return feeLookupResponseDto;
    }

    private PactDslJsonBody buildFeesResponseBody(String feeCode, BigDecimal feeAmount) {
        return new PactDslJsonBody()
            .stringType("code", feeCode)
            .stringType("description", "Fee Description")
            .numberType("version", 1)
            .decimalType("fee_amount", feeAmount);
    }
}