plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.github.heheteam.samplebot.MainKt")
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}