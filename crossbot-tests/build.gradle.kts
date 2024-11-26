plugins {
    kotlin("jvm")
    `java-library`
}


dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":common-lib"))
    implementation(project(":student-bot"))
    implementation(project(":teacher-bot"))
    implementation(project(":admin-bot"))
    implementation("dev.inmo:tgbotapi:18.2.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
