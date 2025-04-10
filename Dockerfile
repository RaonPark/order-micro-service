FROM gradle:8.13.0-jdk17-corretto as builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test --parallel

FROM amazoncorretto:17
WORKDIR /app
COPY --from=builder /app/build/libs/ordermicroservice-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]