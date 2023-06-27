package uk.gov.hmcts.reform.civil.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.civil.callback.CaseEvent;
import uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.civil.enums.BusinessProcessStatus.READY;

@Data
@Builder(toBuilder = true)
public class BusinessProcess {

    private String processInstanceId;
    private BusinessProcessStatus status;
    private String activityId;
    private String camundaEvent;
    private String failedExternalTaskId;

    public static BusinessProcess ready(CaseEvent caseEvent) {
        return BusinessProcess.builder().status(READY).camundaEvent(caseEvent.name()).build();
    }

    @JsonIgnore
    public boolean hasSameProcessInstanceId(String processInstanceId) {
        return this.getProcessInstanceId().equals(processInstanceId);
    }

    @JsonIgnore
    public BusinessProcessStatus getStatusOrDefault() {
        return ofNullable(this.getStatus()).orElse(READY);
    }

    @JsonIgnore
    public BusinessProcess start() {
        this.status = BusinessProcessStatus.STARTED;
        this.activityId = null;
        return this;
    }

    @JsonIgnore
    public BusinessProcess updateProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @JsonIgnore
    public BusinessProcess updateActivityId(String activityId) {
        this.activityId = activityId;
        return this;
    }

    @JsonIgnore
    public BusinessProcess resetFailedBusinessProcessToStarted() {
        if (this.status.equals(BusinessProcessStatus.FAILED)) {
            this.failedExternalTaskId = null;
            this.status = BusinessProcessStatus.STARTED;
        }
        return this;
    }

    @JsonIgnore
    public BusinessProcess reset() {
        this.activityId = null;
        this.failedExternalTaskId = null;
        this.processInstanceId = null;
        this.status = BusinessProcessStatus.FINISHED;

        return this;
    }
}
