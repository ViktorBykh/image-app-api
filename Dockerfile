FROM maven:3.8.5-openjdk-17 AS builder
WORKDIR /app

ARG AWS_ACCESS_KEY
ARG AWS_SECRET_KEY
ARG AWS_REGION
ARG AWS_S3_BUCKET
ARG UI_BASE_URL

ENV AWS_ACCESS_KEY=$AWS_ACCESS_KEY
ENV AWS_SECRET_KEY=$AWS_SECRET_KEY
ENV AWS_REGION=$AWS_REGION
ENV AWS_S3_BUCKET=$AWS_S3_BUCKET
ENV UI_BASE_URL=$UI_BASE_URL

COPY . .
RUN mvn clean package

FROM openjdk:17-oracle
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
EXPOSE 8080
