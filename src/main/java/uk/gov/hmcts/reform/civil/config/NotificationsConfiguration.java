package uk.gov.hmcts.reform.civil.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.civil.config.properties.notification.NotificationsProperties;
import uk.gov.service.notify.NotificationClient;

@Configuration
public class NotificationsConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "notifications")
    public NotificationsProperties notificationsProperties() {
        return new NotificationsProperties();
    }

    @Bean
    public NotificationClient notificationClient(NotificationsProperties notificationsProperties) {
        return new NotificationClient("unspec-a8b1617c-8e15-49aa-a8d3-a27a243f3c45-62103421-281e-4f28-ae70-74dff6a1a76f");
    }

}
