plugins {
    application
}

application {
    mainClass.set("com.github.heheteam.teacherbot.MainKt")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":common-lib"))

    implementation(libs.tgbotapi)
    implementation(libs.exposed.core)
    implementation(libs.kotlin.result)
    implementation(libs.hoplite.json)
    implementation(libs.postgresql)
}
