plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "edu-platform"

include("common-lib", "student-bot", "parent-bot", "teacher-bot", "admin-bot")
