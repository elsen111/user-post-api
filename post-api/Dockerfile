# Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew bootJar --no-daemon -x test


# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd \
        --system \
        --uid 1001 \
        --create-home \
        appuser

COPY --from=builder \
     /app/build/libs/*.jar \
     app.jar

RUN chown appuser:appuser app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]