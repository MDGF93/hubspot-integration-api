FROM gradle:8.7-jdk21 AS builder

WORKDIR /app

COPY build.gradle settings.gradle ./

COPY src ./src

RUN gradle clean build bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

USER root

RUN apt-get update && apt-get install -y curl jq && \
    rm -rf /var/lib/apt/lists/*

COPY entrypoint.sh .

RUN chmod +x entrypoint.sh

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["./entrypoint.sh"]

