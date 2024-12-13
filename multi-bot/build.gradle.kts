plugins {
    application
}

application {
    mainClass.set("com.github.heheteam.multibot.MainKt")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":common-lib"))
    implementation(project(":student-bot"))
    implementation(project(":teacher-bot"))
    implementation(project(":admin-bot"))
    implementation(project(":parent-bot"))

    implementation(libs.tgbotapi)
    implementation(libs.exposed.spring.boot.starter)
    implementation(libs.hoplite.json)
    implementation(libs.postgresql)
}
