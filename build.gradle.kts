plugins {
    kotlin("jvm") version "2.0.20" apply false
    kotlin("plugin.serialization") version "2.0.20" apply false
    id("com.diffplug.spotless") version "7.0.0.BETA3"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
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

detekt {
    buildUponDefaultConfig = true
    baseline = file("$rootDir/config/detekt/baseline.xml")
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    parallel = false
    ignoreFailures = false
}

val configFile = files("$rootDir/config/detekt/detekt.yml")
val baselineFile = file("$rootDir/config/detekt/baseline.xml")
val kotlinFiles = "**/*.kt"
val resourceFiles = "**/resources/**"
val buildFiles = "**/build/**"

tasks.register<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>("detektGenerateBaseline") {
    description = "Custom DETEKT build to build baseline for all modules"
    parallel = true
    ignoreFailures = false
    buildUponDefaultConfig = true
    setSource(projectDir)
    baseline.set(file(baselineFile))
    config.setFrom(files(configFile))
    include(kotlinFiles)
    exclude(resourceFiles, buildFiles)
}

tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektAll") {
    description = "Custom DETEKT build for all modules"
    dependsOn("detektGenerateBaseline")
    parallel = true
    ignoreFailures = false
    autoCorrect = false
    buildUponDefaultConfig = true
    setSource(projectDir)
    baseline.set(file(baselineFile))
    config.setFrom(files(configFile))
    include(kotlinFiles)
    exclude(resourceFiles, buildFiles)
    reports {
        html.required.set(true)
        md.required.set(true)
    }
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.7")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-ruleauthors:1.23.7")
}