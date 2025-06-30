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

RUN mkdir -p /edu-platform/logs

COPY --from=build edu-platform/multi-bot/build/libs/multi-bot-1.0.jar multi-bot.jar

CMD java -jar multi-bot.jar --noinit > /edu-platform/logs/log 2>&1
