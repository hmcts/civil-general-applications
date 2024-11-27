package uk.gov.hmcts.reform.civil.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGeneratorFactory;

@Configuration
@Lazy
@Slf4j
public class ServiceTokenGeneratorConfiguration {

    @Bean
    public AuthTokenGenerator serviceAuthTokenGenerator(
        @Value("${idam.s2s-auth.totp_secret}") String secret,
        @Value("${idam.s2s-auth.microservice}") String microService,
        ServiceAuthorisationApi serviceAuthorisationApi
    ) {
        log.info("Initializing ServiceAuthTokenGenerator bean for microservice: {}", microService);
        return AuthTokenGeneratorFactory.createDefaultGenerator(secret, microService, serviceAuthorisationApi);
    }

}
