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
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.civil.client.OrganisationApi;
import uk.gov.hmcts.reform.civil.model.OrganisationResponse;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@PactTestFor(providerName = "referenceData_organisationalInternal")
@MockServerConfig(hostInterface = "localhost", port = "6667")
@TestPropertySource(properties = "rd_professional.api.url=http://localhost:6667")
public class InternalOrganisationApiConsumerTest extends BaseContractTest {

    public static final String ENDPOINT = "/refdata/internal/v1/organisations";
    public static final String ORG_ID = "OrgId";

    @Autowired
    private OrganisationApi organisationApi;

    @Pact(consumer = "civil_general_applications")
    public RequestResponsePact getOrganisationById(PactDslWithProvider builder) {
        return buildUserOrganisationResponsePact(builder);
    }

    @Test
    @PactTestFor(pactMethod = "getOrganisationById")
    public void verifyInternalOrganisation() {
        OrganisationResponse response =
            organisationApi.findOrganisationById(AUTHORIZATION_TOKEN, SERVICE_AUTH_TOKEN, ORG_ID);

        assertThat(response.getOrganisationIdentifier(), is("BJMSDFDS80808"));
    }

    private RequestResponsePact buildUserOrganisationResponsePact(PactDslWithProvider builder) {
        return builder
            .given("Organisation exists for given Id")
            .uponReceiving("a request to get an organisation by id")
            .method("GET")
            .path(ENDPOINT)
            .matchQuery("id", ORG_ID, ORG_ID)
            .headers(AUTHORIZATION_HEADER, AUTHORIZATION_TOKEN, SERVICE_AUTHORIZATION_HEADER,
                     SERVICE_AUTH_TOKEN
            )
            .willRespondWith()
            .body(buildOrganisationResponseDsl())
            .status(HttpStatus.SC_OK)
            .toPact();
    }

    private DslPart buildOrganisationResponseDsl() {
        return newJsonBody(o -> {
            o
                .stringType("companyNumber", "companyNumber")
                .stringType("companyUrl", "companyUrl")
                .minArrayLike("contactInformation", 1, 1,
                              sh -> {
                                  sh.stringType("addressLine1", "addressLine1")
                                      .stringType("addressLine2", "addressLine2")
                                      .stringType("country", "UK")
                                      .stringType("postCode", "SM12SX");

                              }
                )
                .stringType("name", "theKCompany")
                .stringType("organisationIdentifier", "BJMSDFDS80808")
                .stringType("sraId", "sraId")
                .booleanType("sraRegulated", Boolean.TRUE)
                .stringType("status", "ACTIVE")
                .object("superUser", superUser ->
                    superUser
                        .stringType("email", "email")
                        .stringType("firstName", "firstName")
                        .stringType("lastName", "lastName")
                );
        }).build();
    }
}
