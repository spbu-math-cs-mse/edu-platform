plugins {
    `java-library`
}

val exposedVersion: String by project
val postgresDriverVersion: String by project

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.tgbotapi)
    implementation(libs.exposed.core)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.spring.boot.starter)
    implementation(libs.h2database)
}
