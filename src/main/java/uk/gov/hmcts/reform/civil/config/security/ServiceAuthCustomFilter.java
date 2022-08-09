package uk.gov.hmcts.reform.civil.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;

import java.io.IOException;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ServiceAuthCustomFilter extends OncePerRequestFilter {

    public static final String AUTHORISATION = "ServiceAuthorization";

    private static final Logger LOG = LoggerFactory.getLogger(uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter.class);
    @Value("${idam.s2s-authorised-services}")
    private final List<String> authorisedServices;

    private final ServiceAuthorisationApi serviceAuthorisationApi;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {

            String bearerToken = extractBearerToken(request);
            String serviceName = serviceAuthorisationApi.getServiceName(bearerToken);

            if (!authorisedServices.contains(serviceName)) {
                LOG.debug("service forbidden {}", serviceName);
                response.setStatus(HttpStatus.FORBIDDEN.value());
            } else {
                LOG.debug("service authorized {}", serviceName);
                request.authenticate().setStatus(HttpServletResponse.SC_);
                filterChain.doFilter(request, response);
            }
        } catch (InvalidTokenException | ServiceException exception) {
            LOG.warn("Unsuccessful service authentication", exception);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String token = request.getHeader(AUTHORISATION);
        if (token == null) {
            log.info("Not service Token, looking for authentication token");
            token = request.getHeader("Authorization");
            if(token==null)
                throw new InvalidTokenException("ServiceAuthorization Token is missing");
        }
        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }

}

