plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.github.heheteam.samplebot.MainKt")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("dev.inmo:tgbotapi:18.2.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}