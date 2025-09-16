plugins {
    application
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

application {
    mainClass.set("com.github.heheteam.multibot.MainKt")
}

tasks {
    shadowJar {
        archiveBaseName.set("multi-bot")
        archiveClassifier.set("")
        archiveVersion.set("1.0")
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
}


dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":common-lib"))
    implementation(project(":student-bot"))
    implementation(project(":teacher-bot"))
    implementation(project(":admin-bot"))

    implementation(libs.tgbotapi)
    implementation(libs.kotlin.result)
    implementation(libs.exposed.spring.boot.starter)
    implementation(libs.postgresql)
    implementation("com.github.ajalt.clikt:clikt:5.0.2")
}
