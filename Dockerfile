FROM eclipse-temurin:21-jdk-jammy AS cache
WORKDIR /cache
COPY gradle gradle
COPY gradlew .
COPY settings.gradle.kts .
COPY build.gradle.kts .
RUN ./gradlew dependencies

FROM eclipse-temurin:21-jdk-jammy AS build
COPY --from=cache /cache /edu-platform
WORKDIR /edu-platform

COPY gradlew ./
COPY gradle/ gradle/
COPY settings.gradle.kts ./
COPY . ./

RUN chmod +x ./gradlew
RUN ./gradlew :multi-bot:shadowJar

FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /edu-platform

RUN groupadd -r appgroup && useradd -r -g appgroup -s /sbin/nologin appuser
RUN chown -R appuser:appgroup /edu-platform

COPY --from=build edu-platform/multi-bot/build/libs/multi-bot-1.0.jar multi-bot.jar

USER appuser
CMD java -jar multi-bot.jar > ./logs/log 2>&1