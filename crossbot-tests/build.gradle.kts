plugins {
    `java-library`
}


dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":common-lib"))
    implementation(project(":student-bot"))
    implementation(project(":teacher-bot"))
    implementation(project(":admin-bot"))
    implementation(libs.tgbotapi)
    implementation(libs.exposed.spring.boot.starter)
    implementation(libs.kotlin.result)
}
