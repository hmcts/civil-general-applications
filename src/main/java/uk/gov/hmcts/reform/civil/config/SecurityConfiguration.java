package uk.gov.hmcts.reform.civil.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.spring.useronly.AuthCheckerUserOnlyFilter;
import uk.gov.hmcts.reform.civil.security.CustomAuthCheckerUserOnlyFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final String[] AUTHORITIES = {
        "caseworker-civil",
        "caseworker-civil-solicitor",
        "caseworker",
        "citizen"
    };

    private static final String[] AUTH_WHITELIST = {
        // -- swagger ui
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-resources/**",
        "/swagger-ui/**",
        "/webjars/**",
        // other public endpoints of API
        "/health",
        "/env",
        "/health/liveness",
        "/service-request-update",
        "/health/readiness",
        "/status/health",
        "/service-request-update",
        "/",
        "/loggers/**",
        "/testing-support/flowstate"
    };

    private final RequestAuthorizer<User> userRequestAuthorizer;
    private final AuthenticationManager authenticationManager;

    public SecurityConfiguration(
        RequestAuthorizer<User> userRequestAuthorizer,
        AuthenticationManager authenticationManager
    ) {
        this.userRequestAuthorizer = userRequestAuthorizer;
        this.authenticationManager = authenticationManager;
    }

    @Bean
    public AuthCheckerUserOnlyFilter<User> authCheckerUserOnlyFilter() {
        CustomAuthCheckerUserOnlyFilter<User> filter = new CustomAuthCheckerUserOnlyFilter<>(userRequestAuthorizer);
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthCheckerUserOnlyFilter<User> authCheckerUserOnlyFilter) throws Exception {
        http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .addFilter(authCheckerUserOnlyFilter)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(AUTH_WHITELIST).permitAll()
                .requestMatchers("/cases/callbacks/**", "/case/document/generateAnyDoc", "/dashboard/**")
                .hasAnyAuthority(AUTHORITIES)
                .anyRequest().authenticated()
            )
            .oauth2Client();

        return http.build();
    }
}
