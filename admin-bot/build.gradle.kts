plugins {
    application
}

application {
    mainClass.set("com.github.heheteam.adminbot.MainKt")
}

dependencies {
    implementation(project(":common-lib"))

    implementation(libs.tgbotapi)
    implementation(libs.exposed.spring.boot.starter)
    implementation(libs.kotlin.result)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.yaml)
    implementation(libs.postgresql)
}
