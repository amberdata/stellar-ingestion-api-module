FROM openjdk:10-jre-slim

ARG JAR_FILE

COPY ${JAR_FILE} /app.jar
COPY launch.sh /launch.sh

RUN chmod +x /launch.sh

ENTRYPOINT ["/launch.sh"]
