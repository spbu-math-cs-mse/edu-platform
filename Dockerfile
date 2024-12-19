FROM ubuntu:latest

FROM eclipse-temurin:21-jdk-jammy AS build

# Copy the project files (including gradlew) to /src in the container
COPY . /edu-platform
WORKDIR /edu-platform

# Make gradlew executable (if not already executable)
RUN chmod +x ./gradlew

COPY gradlew /edu-platform/
COPY gradle /edu-platform/gradle/
COPY settings.gradle.kts /edu-platform/
COPY . /edu-platform/

RUN ./gradlew spotlessApply
RUN ./gradlew --no-daemon clean assemble
RUN ./gradlew :multi-bot:shadowJar




# Runtime stage: use a smaller base image for the final app
FROM eclipse-temurin:21-jre-jammy

# Set the working directory in the runtime container
WORKDIR /edu-platform

# Copy the built JAR file from the build stage
COPY --from=build /edu-platform/multi-bot/build/libs/multi-bot-1.0.jar multi-bot.jar

# Run the application
ENTRYPOINT ["java", "-jar", "multi-bot.jar"]