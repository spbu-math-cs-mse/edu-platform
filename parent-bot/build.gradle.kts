plugins {
    application
}

application {
    mainClass.set("com.github.heheteam.parentbot.MainKt")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":common-lib"))

    implementation(libs.tgbotapi)
    implementation(libs.exposed.spring.boot.starter)
}
