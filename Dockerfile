FROM eclipse-temurin:11
EXPOSE 5000

COPY build/libs/*.jar app.jar
COPY config/elastic/elastic-apm-agent-1.54.0.jar /config/elastic/elastic-apm-agent.jar
CMD ["java", "-jar", "app.jar"]

