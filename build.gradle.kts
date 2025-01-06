plugins {
    kotlin("jvm") version "2.0.20" apply false
    kotlin("plugin.serialization") version "2.0.20" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    java
}

allprojects {
    group = "com.github.heheteam"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    apply(plugin = "kotlin")
    apply(plugin = "kotlinx-serialization")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    detekt {
        buildUponDefaultConfig = true
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        parallel = false
        ignoreFailures = false
    }
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.test {
        useJUnitPlatform()
        if (project.hasProperty("excludeTests")) {
            exclude(project.property("excludeTests").toString())
        }
    }
    dependencies {
        testImplementation(kotlin("test"))
        implementation("redis.clients:jedis:5.1.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.7")
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-ruleauthors:1.23.7")
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        md.required.set(true)
    }
}
