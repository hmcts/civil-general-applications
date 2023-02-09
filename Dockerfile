ARG APP_INSIGHTS_AGENT_VERSION=3.0.1

# Application image

FROM hmctspublic.azurecr.io/base/java:17-distroless

# Change to non-root privilege
USER hmcts

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/civil-general-applications.jar /opt/app/

EXPOSE 4550
CMD [ "civil-general-applications.jar" ]
