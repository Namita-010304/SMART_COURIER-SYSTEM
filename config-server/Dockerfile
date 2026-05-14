# STEP 1: Build JAR
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests


# STEP 2: Run JAR
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/target/config-server-1.0.0.jar app.jar

EXPOSE 8889

ENTRYPOINT ["java", "-jar", "app.jar"]