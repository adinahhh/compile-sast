plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.compile.sast"
version = "0.1.0"

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.1.10")
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

gradlePlugin {
    plugins {
        create("sast") {
            id = "com.compile.sast"
            implementationClass = "com.compile.sast.gradle.SastGradlePlugin"
        }
    }
}
