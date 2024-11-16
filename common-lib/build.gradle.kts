plugins {
    kotlin("jvm")
    `java-library`
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
