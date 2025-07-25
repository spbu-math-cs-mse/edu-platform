plugins {
    `java-library`
}

val exposedVersion: String by project
val postgresDriverVersion: String by project

dependencies {
    testImplementation("io.mockk:mockk:1.14.2")
    testImplementation(libs.kotlin.coro.core)
    testImplementation(libs.kotlin.coro.test)
    testImplementation(kotlin("test"))
    implementation(libs.tgbotapi)
    implementation(libs.exposed.core)
    implementation(libs.exposed.json)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.spring.boot.starter)
    implementation(libs.h2database)
    implementation(libs.kotlin.result)
    implementation(libs.kotlin.result.coroutines)
    implementation(libs.google.api.services.sheets)
    implementation(libs.google.api.services.drive)
    implementation(libs.dotenv.kotlin)
    implementation(libs.postgresql)
}
