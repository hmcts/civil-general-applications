package uk.gov.hmcts.reform.civil.config.security;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.AuthCheckerException;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.BearerTokenMissingException;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.core.user.UserRequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserPair;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Slf4j
public class AuthCheckServiceFilter extends AbstractPreAuthenticatedProcessingFilter {

//    public static final String AUTHORISATION = "ServiceAuthorization";
//
//    private static final Logger LOG = LoggerFactory.getLogger(uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter.class);
//    @Value("${idam.s2s-authorised-services}")
//    private final List<String> authorisedServices;
//
//    private final ServiceAuthorisationApi serviceAuthorisationApi;
//
////    private final RequestAuthorizer<User> userRequestAuthorizer;
////    private static final Set anonymousRole = new HashSet<String>(Arrays.asList("ROLE_ANONYMOUS"));
//
//
//
//    @Override
//    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
//        Service service = authorizeService(request);
//        if (service == null) {
//            return null;
//        }
//
//        return service;
//    }
//
//    @Override
//    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
//        String preAuthenticatedCredentials = request.getHeader(AUTHORISATION);
//        return (preAuthenticatedCredentials != null) ? preAuthenticatedCredentials : " " ;
//    }
//
////    private User authorizeUser(HttpServletRequest request) {
////        try {
////            return userRequestAuthorizer.authorise(request);
////        } catch (BearerTokenMissingException btme) {
////                return new User("anonymous", anonymousRole);
////        } catch(AuthCheckerException ace) {
////            log.debug("Unsuccessful user authentication", ace);
////            return null;
////        }
////    }
//
//    private Service authorizeService(HttpServletRequest request) {
//
//        try {
//
//            String bearerToken = extractBearerToken(request);
//            String serviceName = serviceAuthorisationApi.getServiceName(bearerToken);
//
//            if (!authorisedServices.contains(serviceName)) {
//                LOG.debug("service forbidden {}", serviceName);
//                response.setStatus(HttpStatus.FORBIDDEN.value());
//            } else {
//                LOG.debug("service authorized {}", serviceName);
//                response.setStatus(HttpServletResponse.SC_);
//                filterChain.doFilter(request, response);
//            }
//        } catch (InvalidTokenException | ServiceException exception) {
//            LOG.warn("Unsuccessful service authentication", exception);
//            response.setStatus(HttpStatus.UNAUTHORIZED.value());
//        }
//        try {
//            return serviceRequestAuthorizer.authorise(request);
//        } catch (AuthCheckerException e) {
//            log.warn("Unsuccessful service authentication", e);
//            return null;
//        }
//    }
//    private String extractBearerToken(HttpServletRequest request) {
//        String token = request.getHeader(AUTHORISATION);
//        if (token == null) {
//            log.info("Not service Token, looking for authentication token");
//            token = request.getHeader("Authorization");
//            if(token==null)
//                throw new InvalidTokenException("ServiceAuthorization Token is missing");
//        }
//        return token.startsWith("Bearer ") ? token : "Bearer " + token;
//    }

}
