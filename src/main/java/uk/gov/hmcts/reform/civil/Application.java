package uk.gov.hmcts.reform.civil;

import org.camunda.bpm.extension.rest.EnableCamundaRestClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
@ComponentScan("uk.gov.hmcts.reform")
@EnableCamundaRestClient
@EnableFeignClients(basePackages = {
    "uk.gov.hmcts.reform.idam.client",
    "uk.gov.hmcts.reform.civil",
    "uk.gov.hmcts.reform.prd",
    "uk.gov.hmcts.reform.ccd.document.am"
})
@ImportAutoConfiguration({FeignAutoConfiguration.class, HttpClientConfiguration.class})
@SuppressWarnings("HideUtilityClassConstructor")
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
