FROM ubuntu:latest

FROM eclipse-temurin:21-jdk-jammy AS build

COPY . /edu-platform
WORKDIR /edu-platform

COPY gradlew ./
COPY gradle/ gradle/
COPY settings.gradle.kts ./
COPY . ./

RUN chmod +x ./gradlew

RUN ./gradlew spotlessApply
RUN ./gradlew :multi-bot:shadowJar

FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /edu-platform

COPY --from=build edu-platform/multi-bot/build/libs/multi-bot-1.0.jar multi-bot.jar

CMD java -jar multi-bot.jar --student-bot-token $STUDENT_BOT_TOKEN --teacher-bot-token $TEACHER_BOT_TOKEN --admin-bot-token $ADMIN_BOT_TOKEN --parent-bot-token $PARENT_BOT_TOKEN > ./logs/log 2>&1
