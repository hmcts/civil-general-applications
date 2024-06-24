package uk.gov.hmcts.reform.civil;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.civil.client.FeesApiClient;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@PactTestFor(providerName = "feeRegister_lookUp")
@MockServerConfig(hostInterface = "localhost", port = "6662")
@TestPropertySource(properties = "fees.api.url=http://localhost:6662")
public class FeesLookupApiConsumerTest extends BaseContractTest {

    public static final String ENDPOINT = "/fees-register/fees/lookup";
    public static final String CHANNEL = "default";
    private static final String GENERAL_APP_EVENT = "general application";
    private static final String WITH_NOTICE_KEYWORD = "GAOnNotice";
    private static final String CONSENT_WITHWITHOUT_NOTICE_KEYWORD = "GeneralAppWithoutNotice";
    private static final String APPN_TO_VARY_KEYWORD = "AppnToVaryOrSuspend";
    public static final String SERVICE_GENERAL = "general";
    public static final String JURISDICTION_CIVIL = "civil";
    public static final String HACFO_ON_NOTICE_KEYWORD = "HACFOOnNotice";

    @Autowired
    private FeesApiClient feesApiClient;


    @Pact(consumer = "civil-general-applications")
    public RequestResponsePact getFeeForAdditionalValue(PactDslWithProvider builder) throws JSONException {
        return buildGenAppFeeRequestResponsePact(builder, "a request for GA HACFOOnNotice",
                                                 HACFO_ON_NOTICE_KEYWORD, GENERAL_APP_EVENT, SERVICE_GENERAL,
                                                 new BigDecimal(10.00), "FEE0011"
        );
    }

    @Pact(consumer = "civil-general-applications")
    public RequestResponsePact getFeeForAppToVaryOrSuspend(PactDslWithProvider builder) throws JSONException {
        return buildGenAppFeeRequestResponsePact(builder, "a request for GA App to vary or to suspend",
                                                 APPN_TO_VARY_KEYWORD, "miscellaneous", "other",
                                                 new BigDecimal(30.00), "FEE0013"
        );
    }

    @Pact(consumer = "civil-general-applications")
    public RequestResponsePact getFeeForConsentWithOrWithout(PactDslWithProvider builder) throws JSONException {
        return buildGenAppFeeRequestResponsePact(builder, "a request for GA Consent with or without notice",
                                                 CONSENT_WITHWITHOUT_NOTICE_KEYWORD, GENERAL_APP_EVENT, SERVICE_GENERAL,
                                                 new BigDecimal(20.00), "FEE0012"
        );
    }

    @Pact(consumer = "civil-general-applications")
    public RequestResponsePact getFeeForWithNotice(PactDslWithProvider builder) throws JSONException {
        return buildGenAppFeeRequestResponsePact(builder, "a request for GA with notice",
                                                 WITH_NOTICE_KEYWORD, GENERAL_APP_EVENT, SERVICE_GENERAL,
                                                 new BigDecimal(20.00), "FEE0012"
        );
    }


    @Test
    @PactTestFor(pactMethod = "getFeeForAdditionalValue")
    public void verifyFeeForAdditionalValue() {

        FeeLookupResponseDto fee = feesApiClient.lookupFee(
            SERVICE_GENERAL,
            JURISDICTION_CIVIL,
            JURISDICTION_CIVIL,
            CHANNEL,
            GENERAL_APP_EVENT,
            HACFO_ON_NOTICE_KEYWORD
        );
        assertThat(fee.getCode(), is(equalTo("FEE0011")));
        assertThat(fee.getFeeAmount(), is(equalTo(new BigDecimal(10.00))));
    }

    @Test
    @PactTestFor(pactMethod = "getFeeForAppToVaryOrSuspend")
    public void verifyFeeForAppToVaryOrSuspend() {

        FeeLookupResponseDto fee = feesApiClient.lookupFee(
            "other",
            JURISDICTION_CIVIL,
            JURISDICTION_CIVIL,
            CHANNEL,
            "miscellaneous",
            APPN_TO_VARY_KEYWORD
        );
        assertThat(fee.getCode(), is(equalTo("FEE0013")));
        assertThat(fee.getFeeAmount(), is(equalTo(new BigDecimal(30.00))));
    }

    @Test
    @PactTestFor(pactMethod = "getFeeForConsentWithOrWithout")
    public void verifyFeeForConsentWithOrWithout() {
        FeeLookupResponseDto fee = feesApiClient.lookupFee(
            SERVICE_GENERAL,
            JURISDICTION_CIVIL,
            JURISDICTION_CIVIL,
            CHANNEL,
            GENERAL_APP_EVENT,
            CONSENT_WITHWITHOUT_NOTICE_KEYWORD
        );
        assertThat(fee.getCode(), is(equalTo("FEE0012")));
        assertThat(fee.getFeeAmount(), is(equalTo(new BigDecimal(20.00))));
    }

    @Test
    @PactTestFor(pactMethod = "getFeeForWithNotice")
    public void verifyFeeForWithNotice() {
        FeeLookupResponseDto fee = feesApiClient.lookupFee(
            SERVICE_GENERAL,
            JURISDICTION_CIVIL,
            JURISDICTION_CIVIL,
            CHANNEL,
            GENERAL_APP_EVENT,
            WITH_NOTICE_KEYWORD
        );
        assertThat(fee.getCode(), is(equalTo("FEE0012")));
        assertThat(fee.getFeeAmount(), is(equalTo(new BigDecimal(20.00))));
    }



    private RequestResponsePact buildGenAppFeeRequestResponsePact(PactDslWithProvider builder, String uponReceiving,
                                                                  String keyword, String event, String service,
                                                                  BigDecimal feeAmount, String feeCode) {
        return builder
            .given("General Application fees exist")
            .uponReceiving(uponReceiving)
            .path(ENDPOINT)
            .method(HttpMethod.GET.toString())
            .matchQuery("channel", CHANNEL, CHANNEL)
            .matchQuery("event", event, event)
            .matchQuery("jurisdiction1", JURISDICTION_CIVIL, JURISDICTION_CIVIL)
            .matchQuery("jurisdiction2", JURISDICTION_CIVIL, JURISDICTION_CIVIL)
            .matchQuery("service", service, service)
            .matchQuery("keyword", keyword, keyword)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(buildFeesResponseBody(feeCode, feeAmount))
            .status(HttpStatus.SC_OK)
            .toPact();
    }

    private PactDslJsonBody buildFeesResponseBody(String feeCode, BigDecimal feeAmount) {
        return new PactDslJsonBody()
            .stringType("code", feeCode)
            .stringType("description", "Fee Description")
            .numberType("version", 1)
            .decimalType("fee_amount", feeAmount);
    }
}
