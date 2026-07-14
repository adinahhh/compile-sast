plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "com.compile.sast"
version = "0.1.0"

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

val kotlinCompilerVersion = "2.1.10"

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinCompilerVersion")

    testImplementation(kotlin("test-junit5"))
    testImplementation("dev.zacsweers.kctfork:core:0.7.1")
    testImplementation("org.yaml:snakeyaml:2.0")
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
}
