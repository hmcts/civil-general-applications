package uk.gov.hmcts.reform.civil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Arrays;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthorisationService {

    private final ServiceAuthorisationApi serviceAuthorisationApi;

    @Value("${genApp.authorised-services}")
    private String s2sAuthorisedServices;

    private final IdamClient idamClient;

    public Boolean authoriseService(String serviceAuthHeader) {
        String callingService;
        try {
            String bearerJwt = serviceAuthHeader.startsWith("Bearer ") ? serviceAuthHeader : "Bearer " + serviceAuthHeader;
            callingService = serviceAuthorisationApi.getServiceName(bearerJwt);
            log.info("Calling Service... {}", callingService);
            if (callingService != null && Arrays.asList(s2sAuthorisedServices.split(","))
                .contains(callingService)) {

                return true;
            }
        } catch (Exception ex) {
            //do nothing
            log.error("S2S token is not authorised" + ex);
        }
        return false;
    }

    public Boolean authoriseUser(String authorisation) {
        try {
            UserInfo userInfo = idamClient.getUserInfo(authorisation);
            if (null != userInfo) {
                return true;
            }
        } catch (Exception ex) {
            //do nothing
            log.error("User token is invalid");
        }
        return false;
    }

    public boolean isServiceAndUserAuthorized(String authorisation, String s2sToken) {
        return Boolean.TRUE.equals(authoriseUser(authorisation))
            && Boolean.TRUE.equals(authoriseService(s2sToken));
    }

    public boolean isServiceAuthorized(String s2sToken) {
        return Boolean.TRUE.equals(authoriseService(s2sToken));
    }
}
