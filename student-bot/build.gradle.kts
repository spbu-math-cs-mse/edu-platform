plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.github.heheteam.samplebot.MainKt")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":common-lib"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}