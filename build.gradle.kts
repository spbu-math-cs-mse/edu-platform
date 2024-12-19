plugins {
    kotlin("jvm") version "2.0.20" apply false
    kotlin("plugin.serialization") version "2.0.20" apply false
    id("com.diffplug.spotless") version "7.0.0.BETA3"
    java
}

allprojects {
    group = "com.github.heheteam"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "kotlin")
    apply(plugin = "kotlinx-serialization")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.test {
        useJUnitPlatform()
    }
    dependencies {
        testImplementation(kotlin("test"))
        implementation("redis.clients:jedis:5.1.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    }
}

spotless {
    kotlin {
        ktlint("1.0.0")
            .setEditorConfigPath("$projectDir/.editorconfig")  // sample unusual placement
            .editorConfigOverride(
                mapOf(
                    "indent_size" to 2,
                    // intellij_idea is the default style we preset in Spotless, you can override it referring to https://pinterest.github.io/ktlint/latest/rules/code-styles.
                    "ktlint_code_style" to "intellij_idea",
                )
            )
            .customRuleSets(
                listOf(
                    "io.nlopez.compose.rules:ktlint:0.3.3"
                )
            )
        target("**/*.kt")
    }
}
