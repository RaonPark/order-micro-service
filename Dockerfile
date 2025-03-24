FROM amazoncorretto:17 as builder
ARG JAR_FILE=build/libs/ordermicroservice-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]