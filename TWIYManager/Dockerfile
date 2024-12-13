FROM ubuntu:latest
LABEL authors="VintusS"

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src /app/src

RUN mvn clean package -DskipTests

FROM openjdk:21

WORKDIR /app

COPY --from=build /app/target/TWIYManager-1.0-SNAPSHOT.jar /app/TWIYManager.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "TWIYManager.jar"]
