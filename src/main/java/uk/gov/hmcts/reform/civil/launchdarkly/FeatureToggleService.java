package uk.gov.hmcts.reform.civil.launchdarkly;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
public class FeatureToggleService {

    private final LDClientInterface internalClient;
    private final String environment;

    @Autowired
    public FeatureToggleService(LDClientInterface internalClient, @Value("${launchdarkly.env}") String environment) {
        this.internalClient = internalClient;
        this.environment = environment;
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public boolean isFeatureEnabled(String feature) {
        return internalClient.boolVariation(feature, createLDUser().build(), false);
    }

    public boolean isFeatureEnabled(String feature, LDUser user) {
        return internalClient.boolVariation(feature, user, false);
    }

    public boolean isGaForLipsEnabled() {
        return internalClient.boolVariation("GaForLips", createLDUser().build(), false);
    }

    public boolean isOrganisationOnboarded(String orgId) {
        LDUser ldUser = createLDUser().custom("orgId", orgId).build();
        return internalClient.boolVariation("isOrganisationOnboarded", ldUser, false);
    }

    public boolean isMultipartyEnabled() {
        return internalClient.boolVariation("multiparty", createLDUser().build(), false);
    }

    public boolean isRpaContinuousFeedEnabled() {
        return internalClient.boolVariation("rpaContinuousFeed", createLDUser().build(), false);
    }

    public boolean isCaseFileViewEnabled() {
        return internalClient.boolVariation("case-file-view", createLDUser().build(), false);
    }

    public boolean isCoSCEnabled() {
        return internalClient.boolVariation("isCoSCEnabled", createLDUser().build(), false);
    }

    public LDUser.Builder createLDUser() {
        return new LDUser.Builder("civil-service")
            .custom("timestamp", String.valueOf(System.currentTimeMillis()))
            .custom("environment", environment);
    }

    private void close() {
        try {
            internalClient.close();
        } catch (IOException e) {
            log.error("Error in closing the Launchdarkly client::", e);
        }
    }

    public boolean isMintiEnabled() {
        return internalClient.boolVariation("minti", createLDUser().build(), false);
    }

    public boolean isFeatureEnabledForDate(String feature, Long date, boolean defaultValue) {
        return internalClient.boolVariation(feature, createLDUser().custom("timestamp", date).build(), defaultValue);
    }

    public boolean isMultiOrIntermediateTrackEnabled() {
        ZoneId zoneId = ZoneId.systemDefault();
        long epoch;
        epoch = LocalDateTime.now().atZone(zoneId).toEpochSecond();
        return isMintiEnabled()
            && isFeatureEnabledForDate("multi-or-intermediate-track", epoch, false);
    }
}
