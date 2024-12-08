plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "edu-platform"

include(
  ":common-lib",
  ":student-bot",
  ":parent-bot",
  ":teacher-bot",
  ":admin-bot",
  ":multi-bot",
  ":crossbot-tests"
)

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      version("tgbotapi", "18.2.1")
      version("slf4j", "1.6.1")
      version("exposed", "0.56.0")
      version("postgres", "42.7.2")
      version("h2", "2.2.224")
      version("kotlin-result", "2.0.0")
      version("google-api-sheets", "v4-rev20241008-2.0.0")
      version("google-http-client", "1.45.0")
      version("hoplite", "2.7.5")

      library("tgbotapi", "dev.inmo", "tgbotapi").versionRef("tgbotapi")
      library("slf4j-api", "org.slf4j", "slf4j-api").versionRef("slf4j")
      library("slf4j-simple", "org.slf4j", "slf4j-simple").versionRef("slf4j")
      library("exposed-core", "org.jetbrains.exposed", "exposed-core").versionRef("exposed")
      library("exposed-kotlin-datetime", "org.jetbrains.exposed", "exposed-kotlin-datetime").versionRef("exposed")
      library("exposed-spring-boot-starter", "org.jetbrains.exposed", "exposed-spring-boot-starter").versionRef("exposed")
      library("h2database", "com.h2database", "h2").versionRef("h2")
      library("kotlin-result" , "com.michael-bull.kotlin-result", "kotlin-result").versionRef("kotlin-result")
      library("google-api-services-sheets", "com.google.apis", "google-api-services-sheets").versionRef("google-api-sheets")
      library("google-http-client-jackson2", "com.google.http-client", "google-http-client-jackson2").versionRef("google-http-client")
      library("hoplite-core", "com.sksamuel.hoplite", "hoplite-core").versionRef("hoplite")
      library("hoplite-yaml", "com.sksamuel.hoplite","hoplite-yaml").versionRef("hoplite")
    }
  }
}