FROM openjdk:17.0.2
EXPOSE 5000

COPY build/libs/*.jar .
CMD java -jar *.jar

