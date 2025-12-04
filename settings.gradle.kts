plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0" }

rootProject.name = "edu-platform"

include(
  ":common-lib",
  ":student-bot",
  ":parent-bot",
  ":teacher-bot",
  ":admin-bot",
  ":multi-bot",
  ":crossbot-tests",
)

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      version("tgbotapi", "28.0.0")
      version("slf4j", "1.6.1")
      version("exposed", "0.56.0")
      version("postgresql", "42.7.2")
      version("h2", "2.2.224")
      version("kotlin-result", "2.0.0")
      version("kotlin-result-coroutines", "2.0.0")
      version("google-api-sheets", "v4-rev20241008-2.0.0")
      version("google-api-drive", "v3-rev20220815-2.0.0")
      version("dotenv-kotlin","6.5.1")
      version("kotlinx", "1.10.2")

      library("kotlin-coro-core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core")
        .versionRef("kotlinx")
      library("kotlin-coro-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test")
        .versionRef("kotlinx")
      library("tgbotapi", "dev.inmo", "tgbotapi").versionRef("tgbotapi")
      library("slf4j-api", "org.slf4j", "slf4j-api").versionRef("slf4j")
      library("slf4j-simple", "org.slf4j", "slf4j-simple").versionRef("slf4j")
      library("exposed-core", "org.jetbrains.exposed", "exposed-core").versionRef("exposed")
      library("exposed-json", "org.jetbrains.exposed", "exposed-json").versionRef("exposed")
      library("exposed-kotlin-datetime", "org.jetbrains.exposed", "exposed-kotlin-datetime")
        .versionRef("exposed")
      library("exposed-spring-boot-starter", "org.jetbrains.exposed", "exposed-spring-boot-starter")
        .versionRef("exposed")
      library("h2database", "com.h2database", "h2").versionRef("h2")
      library("postgresql", "org.postgresql", "postgresql").versionRef("postgresql")
      library("kotlin-result", "com.michael-bull.kotlin-result", "kotlin-result")
        .versionRef("kotlin-result")
      library("google-api-services-sheets", "com.google.apis", "google-api-services-sheets")
        .versionRef("google-api-sheets")
      library("google-api-services-drive", "com.google.apis", "google-api-services-drive")
        .versionRef("google-api-drive")
      library("dotenv-kotlin", "io.github.cdimascio", "dotenv-kotlin").versionRef("dotenv-kotlin")
      library(
          "kotlin-result-coroutines",
          "com.michael-bull.kotlin-result",
          "kotlin-result-coroutines",
        )
        .versionRef("kotlin-result")
    }
  }
}
