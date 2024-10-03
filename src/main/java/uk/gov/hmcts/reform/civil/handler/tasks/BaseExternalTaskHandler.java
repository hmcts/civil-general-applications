package uk.gov.hmcts.reform.civil.handler.tasks;

import feign.FeignException;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.variable.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.civil.model.ExternalTaskData;

import java.util.Arrays;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.helpers.ExponentialRetryTimeoutHelper.calculateExponentialRetryTimeout;

public abstract class BaseExternalTaskHandler implements ExternalTaskHandler {

    protected static String FLOW_STATE = "flowState";
    protected static String FLOW_FLAGS = "flowFlags";

    protected static Logger log = LoggerFactory.getLogger(BaseExternalTaskHandler.class);

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String topicName = externalTask.getTopicName();

        try {
            log.info("External task '{}' started", topicName);
            var externalTaskData = handleTask(externalTask);
            completeTask(externalTask, externalTaskService, externalTaskData);
        } catch (BpmnError e) {
            externalTaskService.handleBpmnError(externalTask, e.getErrorCode());
            log.error("Bpmn error for external task '{}'", topicName, e);
        } catch (Exception e) {
            handleFailure(externalTask, externalTaskService, e);
            log.error("External task '{}' errored", topicName, e);
        }
    }

    private void completeTask(ExternalTask externalTask,
                              ExternalTaskService externalTaskService,
                              ExternalTaskData data) {
        String topicName = externalTask.getTopicName();

        try {
            ofNullable(getVariableMap(data)).ifPresentOrElse(
                variableMap -> externalTaskService.complete(externalTask, variableMap),
                () -> externalTaskService.complete(externalTask)
            );
            log.info("External task '{}' finished", topicName);
        } catch (Exception e) {
            log.error("Completing external task '{}' errored", topicName, e);
        }
    }

    void handleFailure(ExternalTask externalTask, ExternalTaskService externalTaskService, Exception e) {
        int maxRetries = getMaxAttempts();
        int remainingRetries = externalTask.getRetries() == null ? maxRetries : externalTask.getRetries();

        externalTaskService.handleFailure(
                externalTask,
                e.getMessage(),
                getStackTrace(e),
                remainingRetries - 1,
                calculateExponentialRetryTimeout(1000, maxRetries, remainingRetries)
        );
    }

    String getStackTrace(Throwable throwable) {
        if (throwable instanceof FeignException) {
            return ((FeignException) throwable).contentUTF8();
        }

        return Arrays.toString(throwable.getStackTrace());
    }

    int getMaxAttempts() {
        return 3;
    }

    /**
     * Defines a Map of variables to be added to an external task on completion.
     * By default this is null, override to add values.
     *
     * @return the variables to add to the external task.
     */
    VariableMap getVariableMap(ExternalTaskData externalTaskData) {
        return null;
    }

    /**
     * Executed for each fetched and locked task.
     *
     * @param externalTask the external task to be handled.
     */
    abstract ExternalTaskData handleTask(ExternalTask externalTask) throws Exception;
}
