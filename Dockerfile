# Build stage with Java 17 and Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Runtime stage with just Java
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/New-Lotus-0.0.1-SNAPSHOT.jar app.jar

CMD ["java", "-jar", "app.jar"]
