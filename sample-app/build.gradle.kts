plugins {
    kotlin("jvm")
    id("io.github.adinahhh.compile-sast") version "0.1.0"
}

group = "com.compile.sast.sample"
version = "0.1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
