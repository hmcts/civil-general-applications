package uk.gov.hmcts.reform.civil.advice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.reform.civil.request.RequestData;

import java.util.Arrays;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class UncaughtExceptionHandler {

    private final RequestData requestData;

    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<Object> runtimeException(Exception exception) {
        log.debug(exception.getMessage(), exception);
        log.info(Arrays.toString(exception.getStackTrace()));
        String errorMessage = "Runtime exception of type %s occurred with message: %s for case %s run by user %s";
        log.error(errorMessage.formatted(exception.getClass().getName(), exception.getMessage(),
                                         requestData.caseId(), requestData.userId()));
        return new ResponseEntity<>(exception.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
