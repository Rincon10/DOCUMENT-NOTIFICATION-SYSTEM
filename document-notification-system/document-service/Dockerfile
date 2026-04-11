### --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-19 AS builder
WORKDIR /app

# Copy parent POM and all module POMs first (for dependency caching)
COPY pom.xml .
COPY common/pom.xml common/
COPY common/common-domain/pom.xml common/common-domain/
COPY common/common-application-api/pom.xml common/common-application-api/
COPY infraestructure/pom.xml infraestructure/
COPY infraestructure/kafka/pom.xml infraestructure/kafka/
COPY infraestructure/kafka/kafka-config-data/pom.xml infraestructure/kafka/kafka-config-data/
COPY infraestructure/kafka/kafka-model/pom.xml infraestructure/kafka/kafka-model/
COPY infraestructure/kafka/kafka-consumer/pom.xml infraestructure/kafka/kafka-consumer/
COPY infraestructure/kafka/kafka-producer/pom.xml infraestructure/kafka/kafka-producer/
COPY infraestructure/outbox/pom.xml infraestructure/outbox/
COPY infraestructure/saga/pom.xml infraestructure/saga/
COPY document-service/pom.xml document-service/
COPY document-service/document-domain/pom.xml document-service/document-domain/
COPY document-service/document-domain/document-domain-core/pom.xml document-service/document-domain/document-domain-core/
COPY document-service/document-domain/document-application-service/pom.xml document-service/document-domain/document-application-service/
COPY document-service/document-application-api/pom.xml document-service/document-application-api/
COPY document-service/document-dataaccess/pom.xml document-service/document-dataaccess/
COPY document-service/document-messaging/pom.xml document-service/document-messaging/
COPY document-service/document-container/pom.xml document-service/document-container/
COPY generator-service/pom.xml generator-service/
COPY generator-service/generator-domain/pom.xml generator-service/generator-domain/
COPY generator-service/generator-domain/generator-domain-core/pom.xml generator-service/generator-domain/generator-domain-core/
COPY generator-service/generator-domain/generator-application-service/pom.xml generator-service/generator-domain/generator-application-service/
COPY generator-service/generator-dataaccess/pom.xml generator-service/generator-dataaccess/
COPY generator-service/generator-messaging/pom.xml generator-service/generator-messaging/
COPY generator-service/generator-container/pom.xml generator-service/generator-container/
COPY notification-service/pom.xml notification-service/
COPY notification-service/notification-domain/pom.xml notification-service/notification-domain/
COPY notification-service/notification-domain/notification-domain-core/pom.xml notification-service/notification-domain/notification-domain-core/
COPY notification-service/notification-domain/notification-application-service/pom.xml notification-service/notification-domain/notification-application-service/
COPY notification-service/notification-dataaccess/pom.xml notification-service/notification-dataaccess/
COPY notification-service/notification-messaging/pom.xml notification-service/notification-messaging/
COPY notification-service/notification-container/pom.xml notification-service/notification-container/
COPY customer-service/pom.xml customer-service/
COPY customer-service/customer-domain/pom.xml customer-service/customer-domain/
COPY customer-service/customer-domain/customer-domain-core/pom.xml customer-service/customer-domain/customer-domain-core/
COPY customer-service/customer-domain/customer-application-service/pom.xml customer-service/customer-domain/customer-application-service/
COPY customer-service/customer-application-api/pom.xml customer-service/customer-application-api/
COPY customer-service/customer-dataaccess/pom.xml customer-service/customer-dataaccess/
COPY customer-service/customer-messaging/pom.xml customer-service/customer-messaging/
COPY customer-service/customer-container/pom.xml customer-service/customer-container/

# Download dependencies (cached layer)
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B -q || true

# Copy source code
COPY . .

# Build the entire project
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -B -q

### --- Stage 2: Runtime ---
FROM eclipse-temurin:19-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/document-service/document-container/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8181

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
