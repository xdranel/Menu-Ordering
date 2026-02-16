# Stage 1: Build (This happens on the laptop)
FROM maven:3.8.4-openjdk-17-slim AS build
COPY . /app
WORKDIR /app
# skipTests is vital to save RAM and time during deployment
RUN mvn clean package -DskipTests

# Stage 2: Runtime (This is the light version that actually runs)
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Limit Java's "hunger" for RAM
ENV JAVA_OPTS="-Xmx512M -Xms256M"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]