package uk.gov.hmcts.reform.civil.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "exit-survey")
public class ExitSurveyConfiguration {

    @NotBlank
    private String applicantLink;
    @NotBlank
    private String respondentLink;
}
