package uk.gov.hmcts.reform.civil.handler.callback.camunda.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.civil.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.civil.callback.CaseEvent.GA_EVIDENCE_UPLOAD_CHECK;

import uk.gov.hmcts.reform.civil.callback.CallbackParams;
import uk.gov.hmcts.reform.civil.enums.YesOrNo;
import uk.gov.hmcts.reform.civil.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.civil.model.CaseData;
import uk.gov.hmcts.reform.civil.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.civil.service.DocUploadNotificationService;
import uk.gov.hmcts.reform.civil.service.NotificationException;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {
    DocUploadNotifyHandler.class,
    JacksonAutoConfiguration.class,
})
public class DocUploadNotifyHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private DocUploadNotifyHandler handler;

    @MockBean
    DocUploadNotificationService notificationService;

    Logger logger = (Logger) LoggerFactory.getLogger(DocUploadNotifyHandler.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    private CallbackParams params;

    @Test
    public void shouldReturnCorrectEvent() {
        CaseData caseData = CaseDataBuilder.builder().withNoticeCaseData();
        params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        assertThat(handler.handledEvents()).contains(GA_EVIDENCE_UPLOAD_CHECK);
    }

    @Test
    public void shouldNotifyAll1v2WithNotice() {
        CaseData caseData = CaseDataBuilder.builder().withNoticeCaseData();
        params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        handler.handle(params);
        verify(notificationService, times(1)).notifyApplicantEvidenceUpload(
                caseData
        );
        verify(notificationService, times(1)).notifyRespondentEvidenceUpload(
                caseData
        );
    }

    @Test
    public void shouldNotifyOnlyApp1v2WithoutNotice() {
        CaseData caseData = CaseDataBuilder.builder()
                .judicialOrderMadeWithUncloakApplication(YesOrNo.YES).build();
        params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        handler.handle(params);
        verify(notificationService, times(1)).notifyApplicantEvidenceUpload(
                caseData
        );
        verify(notificationService, times(0)).notifyRespondentEvidenceUpload(
                caseData
        );
    }

    @Test
    public void shouldLogError() {
        listAppender.start();
        logger.addAppender(listAppender);
        doThrow(buildNotificationException()).when(notificationService)
                .notifyApplicantEvidenceUpload(any());
        doThrow(buildNotificationException()).when(notificationService)
                .notifyRespondentEvidenceUpload(any());

        CaseData caseData = CaseDataBuilder.builder()
                .withNoticeCaseData();
        params = callbackParamsOf(caseData, ABOUT_TO_SUBMIT);
        handler.handle(params);
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(0).getMessage()).contains("Failed to send email notification");
        assertEquals(Level.WARN, logsList.get(0).getLevel());
        assertEquals(2, logsList.size());
        listAppender.stop();
    }

    private NotificationException buildNotificationException() {
        return new NotificationException(new Exception("Notification Exception"));
    }
}
