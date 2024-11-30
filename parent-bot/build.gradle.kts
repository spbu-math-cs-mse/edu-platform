plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.github.heheteam.parentbot.MainKt")
}

val exposedVersion: String by project
val postgresDriverVersion: String by project

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":common-lib"))

    implementation("dev.inmo:tgbotapi:18.2.1")
    implementation("org.slf4j:slf4j-api:1.6.1")
    implementation("org.slf4j:slf4j-simple:1.6.1")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-money:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
    implementation("org.postgresql:postgresql:$postgresDriverVersion")
    implementation("com.h2database:h2:2.2.224")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
