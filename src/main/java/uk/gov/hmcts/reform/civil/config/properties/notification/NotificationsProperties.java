package uk.gov.hmcts.reform.civil.config.properties.notification;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Validated
@Data
public class NotificationsProperties {

    @NotEmpty
    private String govNotifyApiKey;

    @NotEmpty
    private String generalApplicationRespondentEmailTemplate;

    @NotEmpty
    private String writtenRepSequentialRepresentationEmailTemplate;

    @NotEmpty
    private String writtenRepConcurrentRepresentationEmailTemplate;

    @NotEmpty
    private String judgeListsForHearingEmailTemplate;

    @NotEmpty
    private String judgeRequestsMoreInformationEmailTemplate;

    @NotEmpty
    private String judgeGivesDirectionsEmailTemplate;
}
