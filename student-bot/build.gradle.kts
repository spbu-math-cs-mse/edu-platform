plugins {
    application
}

application {
    mainClass.set("com.github.heheteam.studentbot.MainKt")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":common-lib"))

    implementation(libs.tgbotapi)
    implementation(libs.exposed.core)
    implementation(libs.kotlin.result)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.yaml)
    implementation(libs.postgresql)
}
