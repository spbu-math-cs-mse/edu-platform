plugins {
    application
}

application {
    mainClass.set("com.github.heheteam.studentbot.MainKt")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.17")
    implementation(project(":common-lib"))

    implementation(libs.tgbotapi)
    implementation(libs.exposed.core)
    implementation(libs.kotlin.result)
    implementation(libs.hoplite.json)
    implementation(libs.postgresql)
    implementation(libs.kotlin.result.coroutines)
}
