plugins {
    application
}

application {
    mainClass.set("com.github.heheteam.studentbot.MainKt")
}

val exposedVersion: String by project
val postgresDriverVersion: String by project

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":common-lib"))

    implementation(libs.tgbotapi)
    implementation(libs.exposed.core)
}
