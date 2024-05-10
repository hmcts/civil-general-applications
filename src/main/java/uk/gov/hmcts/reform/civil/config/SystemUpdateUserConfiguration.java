package uk.gov.hmcts.reform.civil.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
public class SystemUpdateUserConfiguration {

    private final String userName;
    private final String password;

    public SystemUpdateUserConfiguration(@Value("${civil.system-update.username}") String userName,
                                         @Value("${civil.system-update.password}") String password) {
        log.info("SystemUpdateUserConfiguration::userName: {}", userName);
        log.info("SystemUpdateUserConfiguration::password: {}", password);
        this.userName = userName;
        this.password = password;
    }
}
