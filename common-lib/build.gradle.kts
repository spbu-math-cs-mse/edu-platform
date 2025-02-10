plugins {
    `java-library`
}

val exposedVersion: String by project
val postgresDriverVersion: String by project

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.tgbotapi)
    implementation(libs.exposed.core)
    implementation(libs.exposed.json)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.spring.boot.starter)
    implementation(libs.h2database)
    implementation(libs.kotlin.result)
    implementation(libs.google.api.services.sheets)
    implementation(libs.hoplite.json)
    implementation(libs.postgresql)
}
