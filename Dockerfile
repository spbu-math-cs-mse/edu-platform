FROM ubuntu:latest

FROM eclipse-temurin:21-jdk-jammy AS build

COPY . /edu-platform
WORKDIR /edu-platform

RUN chmod +x ./gradlew

COPY gradlew ./
COPY gradle/ gradle/
COPY settings.gradle.kts ./
COPY . ./

ARG GOOGLE_API_KEY
RUN cat $GOOGLE_API_KEY > common-lib/src/main/resources/google_api_key.json
RUN cat common-lib/src/main/resources/google_api_key.json
ARG CONFIG_FILE
RUN cat $CONFIG_FILE > common-lib/src/main/resources/config.json

ARG BOT_TOKENS
RUN cat $BOT_TOKENS > ./bot_tokens

RUN ./gradlew spotlessApply
RUN ./gradlew :multi-bot:shadowJar

FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /edu-platform

COPY --from=build edu-platform/multi-bot/build/libs/multi-bot-1.0.jar multi-bot.jar

CMD java -jar multi-bot.jar $(cat ./bot_tokens)
